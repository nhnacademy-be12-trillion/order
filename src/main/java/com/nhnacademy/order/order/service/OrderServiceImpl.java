package com.nhnacademy.order.order.service;

import com.nhnacademy.order.common.aop.AuthRole;
import com.nhnacademy.order.common.aop.CheckAuth;
import com.nhnacademy.order.common.aop.SagaIdContext;
import com.nhnacademy.order.common.context.SagaContext;
import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.order.domain.*;
import com.nhnacademy.order.order.dto.NonMemberOrderBaseResponse;
import com.nhnacademy.order.order.dto.OrderBaseResponse;
import com.nhnacademy.order.order.dto.OrderCreateRequest;
import com.nhnacademy.order.order.dto.OrderResponse;
import com.nhnacademy.order.order.exception.OrderCreateFailureException;
import com.nhnacademy.order.order.exception.OrderNotFoundException;
import com.nhnacademy.order.order.exception.OrderPasswordMismatchException;
import com.nhnacademy.order.order.exception.OrderStatusTransitionException;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.dto.NonMemberOrderItemStatusPatchRequest;
import com.nhnacademy.order.orderitem.dto.OrderItemResponse;
import com.nhnacademy.order.orderitem.dto.OrderItemStatusPatchRequest;
import com.nhnacademy.order.orderitem.repository.OrderItemRepository;
import com.nhnacademy.order.ordersaga.creation.domain.OrderCreateSaga;
import com.nhnacademy.order.ordersaga.itemrefund.service.NonMemberOrderItemRefundOrchestrator;
import com.nhnacademy.order.ordersaga.itemrefund.service.OrderItemRefundOrchestrator;
import com.nhnacademy.order.ordersaga.cancellation.service.OrderCancelOrchestrator;
import com.nhnacademy.order.ordersaga.creation.service.OrderCreateOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {
    // Repository
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // Service
    private final OrderCreateService orderCreateService;
    private final OrderFinalizerService orderFinalizerService;

    // 사가 패턴
    private final OrderCreateOrchestrator orderCreateOrchestrator;
    private final OrderCancelOrchestrator orderCancelOrchestrator;
    private final OrderItemRefundOrchestrator orderItemRefundOrchestrator;
    private final NonMemberOrderItemRefundOrchestrator nonMemberOrderItemRefundOrchestrator;

    // 비회원 주문 비밀번호 인코딩
    private final PasswordEncoder passwordEncoder;

    private static final String ORDER_NOT_FOUND_MESSAGE = "존재하지 않는 주문 ID: ";

    // 비회원 주문 비밀번호 확인
    private void nonMemberPasswordCheck(String nonMemberPassword, String orderPassword) {
        if (nonMemberPassword == null || !passwordEncoder.matches(nonMemberPassword, orderPassword)) {
            throw new OrderPasswordMismatchException("비회원 주문번호 불일치");
        }
    }

    // 주문 상품 상태 변경
    private void updateOrderItemStatus(UserInfo userInfo, Order order, Long orderItemId, OrderItemStatus status) {

        OrderItemStatusUpdateStrategy strategy = OrderItemStatusUpdateStrategy.from(status);

        String role = (userInfo != null) ? userInfo.role() : null;

        if (!strategy.hasPermission(role)) {
            throw new AccessDeniedException("주문 상품 상태 변경 권한이 없음");
        }

        // 이제 단건 환불인 경우는 사가에서 상태 변경을 처리
        if (strategy == OrderItemStatusUpdateStrategy.RETURNED) {

            OrderItem orderItem = order.findOrderItemInOrder(orderItemId);

            // 회원
            try {
                if (userInfo != null) {
                    orderItemRefundOrchestrator.processItemRefund(userInfo.userId(), order, orderItem);
                } else {
                    // 비회원
                    nonMemberOrderItemRefundOrchestrator.processNonMemberItemRefund(order, orderItem);
                }
            } catch (Exception e) {
                log.error("주문 상품 환불 처리 실패: {}", e.getMessage(), e);
                // 예외를 삼켜서 사용자는 모르게 함
                // '환불 요청' -> '관리자 승인' -> '환불' 순서로 진행되기 때문에 환불 중 오류가 발생해도 '환불 요청' 상태 유지
                // 스케줄러가 알아서 다시 환불 처리함
            }
        } else {
            // 단건 환불이 아닌 경우 바로 상태 변경
            strategy.updateStatus(order, orderItemId);
        }
    }

    // 전체 주문 조회
    @Override
    @CheckAuth(role = AuthRole.ADMIN)
    @Transactional(readOnly = true)
    public Page<OrderResponse> findAllOrders(UserInfo userInfo, Pageable pageable) {
        Page<OrderBaseResponse> orderBaseResponses = orderRepository.findAllBaseOrder(pageable);

        List<Long> orderIds = orderBaseResponses.stream()
                .map(OrderBaseResponse::orderId)
                .toList();

        Map<Long, List<OrderItemResponse>> orderItemResponses = orderItemRepository.findAllByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItemResponse::orderId));


        return orderBaseResponses.map(orderBaseResponse -> {
            List<OrderItemResponse> orderItems = orderItemResponses.getOrDefault(orderBaseResponse.orderId(), Collections.emptyList());

            return OrderResponse.create(orderBaseResponse, orderItems);
        });
    }

    // 주문 생성
    @Override
    @SagaIdContext
    public OrderResponse createOrder(UserInfo userInfo, OrderCreateRequest request) {
        // 1. 불완전한 초기 Order 생성 (OrderStatus: CREATING)
        String nonMemberPassword = Optional.ofNullable(request.nonMemberPassword())
                .map(passwordEncoder::encode)
                .orElse(null);
        OrdererInfo ordererInfo = new OrdererInfo(request.ordererName(), request.ordererContact());
        ReceiverInfo receiverInfo = new ReceiverInfo(request.receiverName(), request.receiverContact(), request.receiverAddress());
        OrderDetails initialOrderDetails = OrderDetails.createInitial(request.receiverPostCode(), request.deliveryDate(), request.pointUsage(), request.couponId());

        // 비회원인 경우 userId가 null
        Long userId = (userInfo != null) ? userInfo.userId() : null;

        Order order = orderCreateService.createInitialOrder(userId, nonMemberPassword, ordererInfo, receiverInfo, initialOrderDetails, request.orderItems());

        UUID sagaId = SagaContext.get();
        OrderCreateSaga saga = OrderCreateSaga.create(sagaId, order.getOrderId());

        try {
            // 2. 오케스트레이션 사가 시작 (재고 감소 -> 쿠폰 사용 -> 포인트 사용)
            orderCreateOrchestrator.processCreateOrder(saga, order);

            // 3. 최종 처리 실행 (OrderStatus: CREATING -> PENDING)
            orderFinalizerService.finalizeOrderCreation(order, saga);
        } catch (OrderCreateFailureException e) {
            // 주문 생성 중 문제 발생 시 무조건 보상 트랜잭션 시작
            // 기본적으로 외부 API와 2번 통신 재시도 -> 실패 시 OrderCreateFailureException이 던져짐
            log.error("주문 ID: {} - 생성 실패: {}", order.getOrderId(), e.getMessage(), e);
            orderCreateOrchestrator.compensate(saga, order);

            order.setOrderStatus(OrderStatus.CREATION_FAILED);
            orderRepository.save(order);
            throw e; // 주문 생성 실패는 사용자에게 알려야 함
        }

        return OrderResponse.create(order);
    }

    // 주문 ID로 주문 찾기
    @Override
    @CheckAuth(role = AuthRole.MEMBER, checkOrderOwner = true)
    @Transactional(readOnly = true)
    public OrderResponse findOrderByOrderId(UserInfo userInfo, Long orderId) {
        Optional<OrderBaseResponse> orderBaseResponseOptional = orderRepository.findBaseOrderById(orderId);

        return orderBaseResponseOptional.map( orderBaseResponse -> {
            List<OrderItemResponse> orderItems = orderItemRepository.findOrderItemByOrder_OrderId(orderId);

            return OrderResponse.create(orderBaseResponse, orderItems);
        }).orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + orderId));
    }

    // 자신의 회원 ID로 주문 목록 찾기
    @Override
    @CheckAuth(role = AuthRole.MEMBER)
    @Transactional(readOnly = true)
    public Page<OrderResponse> findAllOrderByMemberId(UserInfo userInfo, Pageable pageable) {
        Page<OrderBaseResponse> orderBaseResponses = orderRepository.findAllBaseOrderByMemberId(pageable, userInfo.userId());

        List<Long> orderIds = orderBaseResponses.stream()
                .map(OrderBaseResponse::orderId)
                .toList();

        Map<Long, List<OrderItemResponse>> orderItemResponses;

        // 주문 ID가 하나라도 존재해야 주문 상품 목록 조회
        if (!orderIds.isEmpty()) {
            orderItemResponses = orderItemRepository.findAllByOrderIds(orderIds).stream()
                    .collect(Collectors.groupingBy(OrderItemResponse::orderId));
        } else {
            orderItemResponses = Collections.emptyMap();
        }

        return orderBaseResponses.map(orderBaseResponse -> {

            List<OrderItemResponse> items = orderItemResponses.getOrDefault(orderBaseResponse.orderId(), Collections.emptyList());

            return OrderResponse.create(orderBaseResponse, items);
        });
    }

    // 주문 상품 상태 변경 (회원 & 관리자)
    @Override
    @CheckAuth(role = AuthRole.MEMBER, checkOrderOwner = true)
    @Transactional
    public OrderResponse patchOrderItemStatus(UserInfo userInfo, Long orderId, Long orderItemId, OrderItemStatusPatchRequest request) {

        Order order = orderRepository.findOrderWithItemsByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + orderId));

        updateOrderItemStatus(userInfo, order, orderItemId, request.status());

        return OrderResponse.create(order);
    }

    // 비회원 주문 상품 상태 변경 (주문 취소, 환불 요청)
    @Override
    @Transactional
    public OrderResponse patchOrderItemStatusForNonMember(Long orderId, Long orderItemId, NonMemberOrderItemStatusPatchRequest request) {
        Order order = orderRepository.findOrderWithItemsByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + orderId));

        nonMemberPasswordCheck(request.nonMemberPassword(), order.getNonMemberPassword());

        updateOrderItemStatus(null, order, orderItemId, request.status());

        return OrderResponse.create(order);
    }

    // 주문 번호로 주문 찾기
    @Override
    @Transactional(readOnly = true)
    public OrderResponse findOrderByOrderNumber(String orderNumber, String nonMemberPassword) {
        Optional<NonMemberOrderBaseResponse> nonMemberBaseResponseOptional = orderRepository.findNonMemberOrderByOrderNumber(orderNumber);

        return nonMemberBaseResponseOptional.map(nonMemberOrderBaseResponse -> {
            nonMemberPasswordCheck(nonMemberPassword, nonMemberOrderBaseResponse.nonMemberPassword());

            List<OrderItemResponse> items = orderItemRepository.findOrderItemByOrder_OrderId(nonMemberOrderBaseResponse.orderId());

            return OrderResponse.create(nonMemberOrderBaseResponse.toOrderBaseResponse(), items);
        }).orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + orderNumber));
    }

    // 주문 전체 취소
    @Override
    @CheckAuth(role = AuthRole.MEMBER, checkOrderOwner = true)
    public void cancelOrder(UserInfo userInfo, Long orderId) {
        Order order = orderRepository.findOrderWithItemsByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + orderId));

        // 1. 주문이 취소 가능한 상태인지 확인
        boolean orderCancellable = order.getOrderItems().stream()
                .allMatch(orderItem -> orderItem.getOrderItemStatus() == OrderItemStatus.PREPARING);

        if (!orderCancellable) {
            throw new OrderStatusTransitionException("주문 취소가 불가능한 상태의 상품이 포함되어 있음");
        }

        try {
            // 2. 주문 취소 사가 진행
            orderCancelOrchestrator.processCancelOrder(userInfo.userId(), order);
        } catch (Exception e) {
            log.error("주문 ID: {} - 취소 실패: {}", order.getOrderId(), e.getMessage(), e);
            // 사가 시작과 동시에 주문 상태가 '취소 처리 중'으로 변경
            // 만약 실패하더라도 스케줄러가 실패한 사가를 재시도
            // 사용자에게는 이미 '취소 처리 중'으로 보이므로 별도의 예외를 던지지 않아도 됨
        }
    }

    // 비회원 주문 취소
    @Override
    public void cancelOrderForNonMember(Long orderId, String nonMemberPassword) {
        Order order = orderRepository.findOrderWithItemsByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + orderId));

        nonMemberPasswordCheck(nonMemberPassword, order.getNonMemberPassword());

        // 1. 주문이 취소 가능한 상태인지 확인
        boolean orderCancellable = order.getOrderItems().stream()
                .allMatch(orderItem -> orderItem.getOrderItemStatus() == OrderItemStatus.PREPARING);

        if (!orderCancellable) {
            throw new OrderStatusTransitionException("주문 취소가 불가능한 상태의 상품이 포함되어 있음");
        }

        try {
            // 2. 주문 취소 사가 진행
            // 이제 주문 취소 사가가 완료된 주문에 대해, 주문 상태 변경 및 브릿징까지 동기적으로 처리
            orderCancelOrchestrator.processCancelOrder(null, order);

            // 사가 완료 전에 서버가 종료되면 스케줄러가 사가 재시도
        } catch (Exception e) {
            log.error("주문 ID: {} - 취소 실패: {}", order.getOrderId(), e.getMessage(), e);
        }
    }
}
