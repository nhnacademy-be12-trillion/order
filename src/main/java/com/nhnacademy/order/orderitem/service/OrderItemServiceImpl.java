package com.nhnacademy.order.orderitem.service;

import com.nhnacademy.order.common.aop.AuthRole;
import com.nhnacademy.order.common.aop.CheckAuth;
import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.dto.OrderItemResponse;
import com.nhnacademy.order.orderitem.repository.OrderItemRepository;
import com.nhnacademy.order.ordersaga.itemrefund.service.NonMemberOrderItemRefundOrchestrator;
import com.nhnacademy.order.ordersaga.itemrefund.service.OrderItemRefundOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderItemServiceImpl implements OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final OrderItemRefundOrchestrator orderItemRefundOrchestrator;
    private final NonMemberOrderItemRefundOrchestrator nonMemberOrderItemRefundOrchestrator;
    private final OrderItemUpdateService orderItemUpdateService;

    private void handleReturned(UserInfo userInfo, Order order, OrderItem orderItem) {
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
    }

    private void handleConfirmed(UserInfo userInfo, Order order, OrderItem orderItem) {
        try {
            // 회원일 경우
            if (userInfo != null) {
                // 포인트 적립
                orderItemUpdateService.accumulatePoint(order, orderItem);
            }
        } catch (Exception e) {
            log.error("주문 상품 구매 확정 실패: {}", e.getMessage(), e);
            // 구매 확정 실패는 사용자에게 알려야 함
            throw e;
        }
    }

    // 베스트 셀러 도서 ID N개 반환
    @Override
    public List<Long> getTopNSellingBookIds(int limit) {
        List<OrderItemStatus> excludeStatuses = List.of(OrderItemStatus.CANCELED, OrderItemStatus.RETURNED);

        // Pageable을 사용하여 효율적인 쿼리 수행
        Pageable topN = PageRequest.of(0, limit);

        return orderItemRepository.findTopNSellingBookIds(excludeStatuses, topN);
    }

    // 환불 요청, 환불 상태의 주문 상품 목록 반환
    @Override
    @CheckAuth(role = AuthRole.MEMBER)
    @Transactional(readOnly = true)
    public Page<OrderItemResponse> findRefundedOrderItemsByMemberId(UserInfo userInfo, Pageable pageable) {
        List<OrderItemStatus> refundedStatuses = List.of(
                OrderItemStatus.RETURNED,
                OrderItemStatus.RETURN_REQUESTED_CHANGE_OF_MIND,
                OrderItemStatus.RETURN_REQUESTED_DAMAGED
        );
        return orderItemRepository.findAllByOrder_MemberIdAndOrderItemStatusIn(userInfo.userId(), refundedStatuses, pageable);
    }

    // 주문 상품 상태 변경
    @Override
    public void updateOrderItemStatus(UserInfo userInfo, Order order, OrderItem orderItem, OrderItemStatus status) {

        OrderItemStatusUpdateStrategy strategy = OrderItemStatusUpdateStrategy.from(status);

        String role = (userInfo != null) ? userInfo.role() : null;

        if (!strategy.hasPermission(role)) {
            throw new AccessDeniedException("주문 상품 상태 변경 권한이 없음");
        }

        switch (strategy) {
            case RETURNED -> handleReturned(userInfo, order, orderItem);
            case CONFIRMED -> {
                orderItemUpdateService.updateOrderItemStatus(order, orderItem, OrderItemStatusUpdateStrategy.CONFIRMED);

                handleConfirmed(userInfo, order, orderItem);
            }
            default -> orderItemUpdateService.updateOrderItemStatus(order, orderItem, strategy);
        }
    }
}
