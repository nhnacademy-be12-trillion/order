package com.nhnacademy.order.orderitem.repository;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.OrderDetails;
import com.nhnacademy.order.order.domain.OrdererInfo;
import com.nhnacademy.order.order.domain.ReceiverInfo;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.dto.OrderItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        OrdererInfo ordererInfo = new OrdererInfo("홍길동", "010-1234-5678");
        ReceiverInfo receiverInfo = new ReceiverInfo("이순신", "010-9876-5432", "서울");
        OrderDetails orderDetails = OrderDetails.createInitial("12345", LocalDateTime.now(), 0, null);

        // 주문 2개 생성
        order1 = Order.createInitial(1L, null, ordererInfo, receiverInfo, orderDetails);
        order2 = Order.createInitial(1L, null, ordererInfo, receiverInfo, orderDetails);
        entityManager.persist(order1);
        entityManager.persist(order2);

        // 주문1에 아이템 2개, 주문2에 아이템 1개 추가
        OrderItem item1 = OrderItem.createInitial(order1, 101L, 2, (LocalDateTime) null, 500);
        item1.completeOrderItem("테스트 책 1", null, 15000);
        entityManager.persist(item1);

        OrderItem item2 = OrderItem.createInitial(order1, 102L, 1, (LocalDateTime) null, 0);
        item2.completeOrderItem("테스트 책 2", null, 25000);
        entityManager.persist(item2);

        OrderItem item3 = OrderItem.createInitial(order2, 103L, 5, (LocalDateTime) null, 0);
        item3.completeOrderItem("테스트 책 3", null, 10000);
        entityManager.persist(item3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("특정 주문 ID에 속한 주문 상품 목록 조회 - 성공")
    void findOrderItemByOrder_OrderId_Success() {
        // when
        List<OrderItemResponse> result = orderItemRepository.findOrderItemByOrder_OrderId(order1.getOrderId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(item -> item.orderId().equals(order1.getOrderId()));
        assertThat(result.get(0).bookId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("특정 주문 ID에 속한 주문 상품 목록 조회 - 결과 없음")
    void findOrderItemByOrder_OrderId_NoResult() {
        // given
        Long orderIdWithNoItems = 999L;

        // when
        List<OrderItemResponse> result = orderItemRepository.findOrderItemByOrder_OrderId(orderIdWithNoItems);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("여러 주문 ID에 속한 모든 주문 상품 목록 조회 - 성공")
    void findAllByOrderIds_Success() {
        // given
        List<Long> orderIds = List.of(order1.getOrderId(), order2.getOrderId());

        // when
        List<OrderItemResponse> result = orderItemRepository.findAllByOrderIds(orderIds);

        // then
        // order1의 아이템 2개 + order2의 아이템 1개 = 총 3개
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("여러 주문 ID에 속한 모든 주문 상품 목록 조회 - 결과 없음")
    void findAllByOrderIds_NoResult() {
        // given
        List<Long> orderIdsWithNoItems = List.of(998L, 999L);

        // when
        List<OrderItemResponse> result = orderItemRepository.findAllByOrderIds(orderIdsWithNoItems);

        // then
        assertThat(result).isEmpty();
    }
}
