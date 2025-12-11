package com.nhnacademy.order.scheduler;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.OrderStatus;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.order.service.OrderCancelService;
import com.nhnacademy.order.order.service.OrderCompensateService;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.repository.OrderItemRepository;
import com.nhnacademy.order.orderitem.service.OrderItemRefundService;
import com.nhnacademy.order.ordersaga.cancellation.domain.OrderCancelSaga;
import com.nhnacademy.order.ordersaga.cancellation.service.OrderCancelOrchestrator;
import com.nhnacademy.order.ordersaga.creation.domain.OrderCreateSaga;
import com.nhnacademy.order.ordersaga.creation.service.OrderCreateOrchestrator;
import com.nhnacademy.order.ordersaga.itemrefund.domain.NonMemberOrderItemRefundSaga;
import com.nhnacademy.order.ordersaga.itemrefund.domain.OrderItemRefundSaga;
import com.nhnacademy.order.ordersaga.itemrefund.service.NonMemberOrderItemRefundOrchestrator;
import com.nhnacademy.order.ordersaga.itemrefund.service.OrderItemRefundOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReconciliationService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderCancelService orderCancelService;
    private final OrderCreateOrchestrator orderCreateOrchestrator;
    private final OrderItemRefundService orderItemRefundService;
    private final OrderCancelOrchestrator orderCancelOrchestrator;
    private final OrderItemRefundOrchestrator orderItemRefundOrchestrator;
    private final NonMemberOrderItemRefundOrchestrator nonMemberOrderItemRefundOrchestrator;
    private final OrderCompensateService orderCompensateService;

    public void processStuckCreateSagaCompensation(OrderCreateSaga saga) {
        try {
            log.info("[사가 스케줄러] 멈춰있는 주문 생성 사가 보상 처리 시작: {}", saga.getOrderId());
            orderRepository.findOrderWithItemsByOrderId(saga.getOrderId()).ifPresent(order -> {
                orderCreateOrchestrator.compensate(saga, order);
            });
        } catch (Exception e) {
            log.error("[사가 스케줄러] 멈춰있는 주문 생성 사가 보상 처리 실패: {}", saga.getOrderId(), e);
        }
    }

    public void processStuckCancelSagaRetry(OrderCancelSaga saga) {
        try {
            log.info("[사가 스케줄러] 멈춰있는 주문 취소 사가 재시도 시작: {}", saga.getOrderId());
            orderRepository.findOrderWithItemsByOrderId(saga.getOrderId()).ifPresent(order -> {
                orderCancelOrchestrator.retry(saga, order);
            });
        } catch (Exception e) {
            log.error("[사가 스케줄러] 멈춰있는 주문 취소 사가 재시도 실패: {}", saga.getOrderId(), e);
        }
    }

    public void processStuckRefundItemSagaRetry(OrderItemRefundSaga saga) {
        try {
            log.info("[사가 스케줄러] 멈춰있는 주문 상품 환불 사가 (회원) 재시도 시작: {}", saga.getOrderItemId());
            orderRepository.findOrderWithItemsByOrderId(saga.getOrderId()).ifPresent(order -> {
                orderItemRepository.findById(saga.getOrderItemId()).ifPresent(orderItem -> {
                    orderItemRefundOrchestrator.retry(saga, order, orderItem);
                });
            });

        } catch (Exception e) {
            log.error("[사가 스케줄러] 멈춰있는 주문 상품 환불 사가 (회원) 재시도 실패: {}", saga.getOrderItemId(), e);
        }
    }

    public void processStuckNonMemberRefundItemSagaRetry(NonMemberOrderItemRefundSaga saga) {
        try {
            log.info("[사가 스케줄러] 멈춰있는 주문 상품 환불 사가 (비회원) 재시도 시작: {}", saga.getOrderItemId());
            orderItemRepository.findById(saga.getOrderItemId()).ifPresent(orderItem -> {
                nonMemberOrderItemRefundOrchestrator.retry(saga, orderItem);
            });
        } catch (Exception e) {
            log.error("[사가 스케줄러] 멈춰있는 주문 상품 환불 사가 (비회원) 재시도 실패: {}", saga.getOrderItemId(), e);
        }
    }

    public void processCompletedCompensateSagaBridge(OrderCreateSaga saga) {
        try {
            orderRepository.findOrderWithItemsByOrderId(saga.getOrderId()).ifPresent(order -> {
                orderCompensateService.compensateOrder(order, saga);
            });
        } catch (Exception e) {
            log.error("[사가 스케줄러] 완료된 보상 성공 주문 생성 사가 브릿징 실패: {}", saga.getOrderId(), e);
        }
    }

    public void compensateForCreateSagaBridgingFailure(OrderCreateSaga saga) {
        try {
            orderRepository.findOrderWithItemsByOrderId(saga.getOrderId()).ifPresent(order -> {
                orderCreateOrchestrator.compensate(saga, order);
            });
        } catch (Exception e) {
            log.error("[도메인 스케줄러] 완료된 주문 생성 사가 보상 실패: {}", saga.getOrderId(), e);
        }
    }

    public void processCompletedCancelSagaBridge(OrderCancelSaga saga) {
        try {
            orderRepository.findOrderWithItemsByOrderId(saga.getOrderId()).ifPresent(order -> {
                if (order.getOrderStatus() != OrderStatus.CANCELED) {
                    orderCancelService.cancelOrder(order, saga);
                }
            });
        } catch (Exception e) {
            log.error("[도메인 스케줄러] 완료된 주문 취소 사가 브릿징 실패: {}", saga.getOrderId(), e);
        }
    }

    public void processCompletedRefundSagaBridge(OrderItemRefundSaga saga) {
        try {
            orderItemRepository.findById(saga.getOrderItemId()).ifPresent(orderItem -> {
                if (orderItem.getOrderItemStatus() != OrderItemStatus.RETURNED) {
                    orderItemRefundService.completeOrderItem(orderItem, saga);
                }
            });
        } catch (Exception e) {
            log.error("[도메인 스케줄러] 완료된 주문 상품 환불 사가 브릿징 실패: {}", saga.getOrderItemId(), e);
        }
    }

    public void processCompletedNonMemberRefundSagaBridge(NonMemberOrderItemRefundSaga saga) {
        try {
            orderItemRepository.findById(saga.getOrderItemId()).ifPresent(orderItem -> {
                if (orderItem.getOrderItemStatus() != OrderItemStatus.RETURNED) {
                    orderItemRefundService.completeNonMemberOrderItem(orderItem, saga);
                }
            });
        } catch (Exception e) {
            log.error("[도메인 스케줄러] 완료된 주문 상품 환불 사가 (비회원) 브릿징 실패: {}", saga.getOrderItemId(), e);
        }
    }
}
