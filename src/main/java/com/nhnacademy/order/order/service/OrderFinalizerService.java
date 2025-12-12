package com.nhnacademy.order.order.service;

import com.nhnacademy.order.client.dto.BookResponse;
import com.nhnacademy.order.client.service.BookService;
import com.nhnacademy.order.client.service.CouponService;
import com.nhnacademy.order.delivery.domain.DeliveryPolicy;
import com.nhnacademy.order.delivery.exception.PolicyNotConfiguredException;
import com.nhnacademy.order.delivery.repository.DeliveryPolicyRepository;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.OrderStatus;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.ordersaga.creation.domain.OrderCreateSaga;
import com.nhnacademy.order.ordersaga.creation.repository.OrderCreateSagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class OrderFinalizerService {
    private final DeliveryPolicyRepository deliveryPolicyRepository;

    private final BookService bookService;
    private final CouponService couponService;
    private final OrderRepository orderRepository;
    private final OrderCreateSagaRepository orderCreateSagaRepository;

    @Transactional
    public void finalizeOrderCreation(Order order, OrderCreateSaga saga) {

        // 이미 처리된 주문은 다시 처리하지 않음
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            return;
        }

        List<OrderItem> orderItems = order.getOrderItems();

        List<Long> bookIds = orderItems.stream()
                .map(OrderItem::getBookId)
                .toList();

        Map<Long, BookResponse> bookResponseMap = bookService.getBookInfos(bookIds);

        // OrderItem 완성
        orderItems.forEach(orderItem -> {
                    BookResponse bookResponse = bookResponseMap.get(orderItem.getBookId());

                    String bookName = bookResponse.bookName();
                    String bookImage = bookResponse.imageUrl();
                    int price = bookResponse.price();

                    orderItem.completeOrderItem(bookName, bookImage, price);
                });

        // 순수 금액 계산 (도서 * 재고 + 포장비)
        int originPrice = orderItems.stream()
                .mapToInt(orderItem -> orderItem.getPrice() * orderItem.getQuantity() + orderItem.getPackagingPrice())
                .sum();

        // 최종 결제 금액 계산 (순수 금액 - 할인액(쿠폰 + 포인트) + 배송비)
        int totalPrice = originPrice;

        Long couponId = order.getOrderDetails().couponId();

        if (couponId != null) {
            int couponDiscount = couponService.calculateDiscount(couponId, totalPrice);

            totalPrice -= couponDiscount;
        }

        // 배송비 계산
        int deliveryFee = determineDeliveryFee(totalPrice);

        totalPrice += deliveryFee;

        int pointUsage = order.getOrderDetails().pointUsage();

        totalPrice -= pointUsage;

        totalPrice = Math.max(0, totalPrice);

        // 계산된 최종 값들을 Order 엔티티에 반영
        order.completeOrder(originPrice, totalPrice, deliveryFee);

        // 주문 상태를 '결제 대기'로 변경
        order.setOrderStatus(OrderStatus.PENDING);

        orderRepository.save(order);

        // 사가 - 도메인 연결
        saga.setBridged(true);
        orderCreateSagaRepository.save(saga);
    }

    // 배송비 결정
    private int determineDeliveryFee(int finalTotalPrice) {
        DeliveryPolicy deliveryPolicy = deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc()
                .orElseThrow(() -> new PolicyNotConfiguredException("배송 정책이 설정되지 않음"));

        return (finalTotalPrice >= deliveryPolicy.getDeliveryPolicyThreshold())
                ? 0
                : deliveryPolicy.getDeliveryPolicyFee();
    }
}
