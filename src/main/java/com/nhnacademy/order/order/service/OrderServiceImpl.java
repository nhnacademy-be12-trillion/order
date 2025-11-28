package com.nhnacademy.order.order.service;

import com.nhnacademy.order.delivery.domain.DeliveryPolicy;
import com.nhnacademy.order.delivery.exception.PolicyNotConfiguredException;
import com.nhnacademy.order.delivery.repository.DeliveryPolicyRepository;
import com.nhnacademy.order.order.domain.*;
import com.nhnacademy.order.order.dto.OrderBaseResponse;
import com.nhnacademy.order.order.dto.OrderCreateRequest;
import com.nhnacademy.order.order.dto.OrderResponse;
import com.nhnacademy.order.order.exception.OrderNotFoundException;
import com.nhnacademy.order.order.exception.OrderPasswordMismatchException;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.dto.NonMemberOrderItemStatusPatchRequest;
import com.nhnacademy.order.orderitem.dto.OrderItemCreateRequest;
import com.nhnacademy.order.orderitem.dto.OrderItemResponse;
import com.nhnacademy.order.orderitem.dto.OrderItemStatusPatchRequest;
import com.nhnacademy.order.orderitem.repository.OrderItemRepository;
import com.nhnacademy.order.packaging.exception.PackagingNotFoundException;
import com.nhnacademy.order.packaging.repository.PackagingRepository;
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
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeliveryPolicyRepository deliveryPolicyRepository;
    private final PackagingRepository packagingRepository;

    private static final String ORDER_NOT_FOUND_MESSAGE = "존재하지 않는 주문 ID: ";

    @Override
    public Page<OrderResponse> findAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);

        return orders.map(order -> {
            List<OrderItemResponse> orderItems = orderItemRepository.findOrderItemByOrder_OrderId(order.getOrderId());

            return OrderResponse.create(
                order,
                orderItems
            );
        });
    }

    @Transactional
    public Long createOrder(Long memberId, OrderCreateRequest request) {

        // 0. 주문 상품 ID 리스트 추출
        List<Long> bookIds = request.orderItems().stream()
                .map(OrderItemCreateRequest::bookId)
                .toList();

        Map<Long, Integer> stockMap = request.orderItems().stream()
                .collect(Collectors.toMap(OrderItemCreateRequest::bookId, OrderItemCreateRequest::quantity));

        // TODO 1: 도서 API -> 재고 확인 및 차감, 가격 검증
        // bookService.decreaseStock(stockMap);
        // Map<bookId, Price> realPrice = bookService.getRealPrice(bookIds);
        // Map<bookId, Stock> stocks = bookService.getStocks(bookIds);

        // 1. OrderItemCreateRequest -> OrderItem (연관 관계 매핑은 아직 안 함)
        List<OrderItem> orderItems = request.orderItems().stream()
                .map(itemReq -> {
                    int bookPrice = 0; // realPrice.get(itemReq.bookId()); // 도서 API에서 조회한 실제 가격
                    int packagingPrice = 0;

                    // 포장 정책을 조회해 포장 비용 계산
                    if (itemReq.packagingId() != null) {
                        packagingPrice = packagingRepository.findById(itemReq.packagingId())
                                .orElseThrow(() -> new PackagingNotFoundException("포장 정보를 찾을 수 없음: " + itemReq.packagingId()))
                                .getPackagingPrice();
                    }
                    return OrderItem.create(null, itemReq.bookId(), itemReq.quantity(), bookPrice, packagingPrice, itemReq.couponId());
                })
                .toList();

        // 2. 도서 + 포장비 금액
        int totalPackagingPrice = orderItems.stream()
                .mapToInt(orderItem -> orderItem.getPrice() + orderItem.getPackagingPrice())
                .sum();

        // 3. 배송비 결정
        DeliveryPolicy deliveryPolicy = deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc()
                .orElseThrow(() -> new PolicyNotConfiguredException("배송 정책이 설정되지 않음"));

        int deliveryFee = (totalPackagingPrice >= deliveryPolicy.getDeliveryPolicyThreshold())
                ? 0
                : deliveryPolicy.getDeliveryPolicyFee();

        // 4. 할인 전 결제 금액 결정
        int originPrice = totalPackagingPrice + deliveryFee;

        // TODO 2: 쿠폰 API -> 쿠폰 사용 처리
        Map<Long, Integer> couponMap = orderItems.stream()
                .filter(orderItem -> orderItem.getCouponId() != null)
                .collect(Collectors.toMap(OrderItem::getCouponId, OrderItem::getPrice));

         int discountAmount = 0; // couponService.applyCoupon(couponMap);

        // TODO 3: 포인트 API -> 포인트 사용 처리
         int pointUsage = request.pointUsage();
         // pointService.usagePoint(pointUsage);

        // 5. 최종 결제 금액 계산
        int finalTotalPrice = originPrice - (discountAmount + pointUsage);

        // 6. Order 객체 생성
        OrdererInfo ordererInfo = new OrdererInfo(
                request.ordererName(),
                request.ordererContact()
        );

        ReceiverInfo receiverInfo = new ReceiverInfo(
                request.receiverName(),
                request.receiverContact(),
                request.receiverAddress()
        );

        OrderDetails orderDetails = OrderDetails.create(
                request.receiverPostCode(),
                request.deliveryDate(),
                deliveryFee,
                request.pointUsage(),
                originPrice,
                finalTotalPrice
        );

        Order order = Order.create(
                memberId,
                Optional.ofNullable(request.nonMemberPassword())
                        .map(passwordEncoder::encode)
                        .orElse(null),
                ordererInfo,
                receiverInfo,
                orderDetails
        );

        // 7. OrderItem 리스트를 Order에 추가
        orderItems.forEach(order::addOrderItem);

        Order savedOrder = orderRepository.save(order);

        return savedOrder.getOrderId();
    }

    @Override
    public OrderResponse findOrderByCustomer(Long memberId, Long orderId) {
        Optional<OrderBaseResponse> orderBaseResponseOptional = orderRepository.findBaseOrderById(orderId);

        return orderBaseResponseOptional.map( orderBaseResponse -> {
            if (!orderBaseResponse.memberId().equals(memberId)) {
                throw new AccessDeniedException("주문 접근 권한 없음: " + orderId);
            }

            List<OrderItemResponse> orderItems = orderItemRepository.findOrderItemByOrder_OrderId(orderId);

            return OrderResponse.create(orderBaseResponse, orderItems);
        }).orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + orderId));
    }

    @Override
    public OrderResponse findOrderByOrderId(Long orderId) {
        Optional<OrderBaseResponse> orderBaseResponseOptional = orderRepository.findBaseOrderById(orderId);

        return orderBaseResponseOptional.map( orderBaseResponse -> {
            List<OrderItemResponse> orderItems = orderItemRepository.findOrderItemByOrder_OrderId(orderId);

            return OrderResponse.create(orderBaseResponse, orderItems);
        }).orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + orderId));
    }

    @Override
    public Page<OrderResponse> findAllOrderByMemberId(Pageable pageable, Long memberId) {
        Page<OrderBaseResponse> orderBaseResponses = orderRepository.findAllBaseOrderByMemberId(pageable, memberId);

        List<Long> orderIds = orderBaseResponses.stream()
                .map(OrderBaseResponse::orderId)
                .toList();

        Map<Long, List<OrderItemResponse>> orderItemResponses = orderItemRepository.findAllByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItemResponse::orderId));

        return orderBaseResponses.map(orderBaseResponse -> {
            List<OrderItemResponse> items = orderItemResponses.getOrDefault(orderBaseResponse.orderId(), Collections.emptyList());

            return OrderResponse.create(orderBaseResponse, items);
        });

    }

    @Override
    @Transactional
    public void patchOrderItemStatus(Long memberId, Long orderId, Long orderItemId, OrderItemStatusPatchRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + orderId));

        // TODO: 관리자 권한도 고려해야 함
        // 반품 요청, 주문 취소 -> 회원 / 비회원 가능
        // 배송중, 배송 완료, 반품 완료 -> 관리자만 가능
        if (!order.getMemberId().equals(memberId)) {
            throw new AccessDeniedException("주문 접근 권한 없음: " + orderId);
        }

        OrderItemStatusUpdateStrategy strategy = OrderItemStatusUpdateStrategy.from(request.status());

        strategy.updateStatus(order, orderItemId);
    }

    @Override
    @Transactional
    public void patchOrderItemStatusForNonMember(Long orderId, Long orderItemId, NonMemberOrderItemStatusPatchRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + orderId));

        if (!passwordEncoder.matches(request.nonMemberPassword(), order.getNonMemberPassword())) {
            throw new OrderPasswordMismatchException("비회원 주문 비밀번호 불일치");
        }

        OrderItemStatusUpdateStrategy strategy = OrderItemStatusUpdateStrategy.from(request.status());

        strategy.updateStatus(order, orderItemId);
    }

    @Override
    public OrderResponse findOrderByOrderNumber(String orderNumber, String nonMemberPassword) {
        Optional<Order> orderOptional = orderRepository.findByOrderNumber(orderNumber);

        return orderOptional.map(order -> {
            if (!passwordEncoder.matches(nonMemberPassword, order.getNonMemberPassword())) {
                throw new OrderPasswordMismatchException("비회원 주문 비밀번호 불일치");
            }

            List<OrderItemResponse> orderItems = orderItemRepository.findOrderItemByOrder_OrderId(order.getOrderId());

            return OrderResponse.create(
                order,
                orderItems
            );
        }).orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND_MESSAGE + orderNumber));
    }
}
