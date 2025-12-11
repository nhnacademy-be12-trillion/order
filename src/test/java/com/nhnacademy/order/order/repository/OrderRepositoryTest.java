package com.nhnacademy.order.order.repository;

import com.nhnacademy.order.order.domain.*;
import com.nhnacademy.order.order.dto.NonMemberOrderBaseResponse;
import com.nhnacademy.order.order.dto.OrderBaseResponse;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    // 테스트에서 공통으로 사용할 데이터들을 필드로 선언
    private final Long targetMemberId = 1L;
    private final Long anotherMemberId = 2L;
    private Order order1;
    private OrderItem order1Item1;

    @BeforeEach
    void setUp() {
        // given: 모든 테스트가 공유하는 데이터 준비
        OrdererInfo ordererInfo = new OrdererInfo("홍길동", "010-1234-5678");
        ReceiverInfo receiverInfo = new ReceiverInfo("이순신", "010-9876-5432", "서울");
        OrderDetails orderDetails = OrderDetails.createInitial("12345", LocalDateTime.now(), 0, null);

        // Order 객체들 생성 및 저장
        order1 = Order.createInitial(targetMemberId, null, ordererInfo, receiverInfo, orderDetails);
        Order order2 = Order.createInitial(anotherMemberId, null, ordererInfo, receiverInfo, orderDetails);
        Order order3 = Order.createInitial(targetMemberId, null, ordererInfo, receiverInfo, orderDetails);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);

        // OrderItem 객체들 생성 및 저장 (order1에 연결)
        order1Item1 = OrderItem.createInitial(order1, 101L, 2, (LocalDateTime) null, 0);
        order1Item1.completeOrderItem("테스트 책 101", 15000);
        entityManager.persist(order1Item1);

        OrderItem order1Item2 = OrderItem.createInitial(order1, 102L, 1, (LocalDateTime) null, 0);
        order1Item2.completeOrderItem("테스트 책 102", 25000);
        entityManager.persist(order1Item2);

        entityManager.persist(order1Item1);
        entityManager.persist(order1Item2);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("주문 ID로 회원 ID 조회 - 성공")
    void findMemberIdByOrderId_Success() {
        // given
        Long targetOrderId = order1.getOrderId();

        // when
        Optional<Long> resultOptional = orderRepository.findMemberIdByOrderId(targetOrderId);

        // then
        assertThat(resultOptional).isPresent();
        assertThat(resultOptional).hasValue(targetMemberId);
    }

    @Test
    @DisplayName("주문 ID로 회원 ID 조회 - 실패: 존재하지 않는 주문")
    void findMemberIdByOrderId_Failure_NotFound() {
        // given
        Long nonExistentOrderId = 999L;

        // when
        Optional<Long> resultOptional = orderRepository.findMemberIdByOrderId(nonExistentOrderId);

        // then
        assertThat(resultOptional).isEmpty();
    }

    @Test
    @DisplayName("주문 ID로 주문과 주문상품 함께 조회 - 성공")
    void findOrderWithItemsByOrderId_Success() {
        // given
        Long targetOrderId = order1.getOrderId();

        // when
        Optional<Order> resultOptional = orderRepository.findOrderWithItemsByOrderId(targetOrderId);

        // then
        assertThat(resultOptional).isPresent();
        Order foundOrder = resultOptional.get();
        assertThat(foundOrder.getOrderId()).isEqualTo(targetOrderId);
        assertThat(foundOrder.getOrderItems()).isNotNull().hasSize(2);
        assertThat(foundOrder.getOrderItems().getFirst().getBookId()).isEqualTo(order1Item1.getBookId());
    }

    @Test
    @DisplayName("주문 ID로 주문과 주문상품 함께 조회 - 실패: 존재하지 않는 주문")
    void findOrderWithItemsByOrderId_Failure_NotFound() {
        // given
        Long nonExistentOrderId = 999L;

        // when
        Optional<Order> resultOptional = orderRepository.findOrderWithItemsByOrderId(nonExistentOrderId);

        // then
        assertThat(resultOptional).isEmpty();
    }

    @Test
    @DisplayName("주문 번호로 주문과 주문상품 함께 조회 - 성공")
    void findOrderWithItemsByOrderNumber_Success() {
        // given
        String targetOrderNumber = order1.getOrderNumber();

        // when
        Optional<Order> resultOptional = orderRepository.findOrderWithItemsByOrderNumber(targetOrderNumber);

        // then
        assertThat(resultOptional).isPresent();
        Order foundOrder = resultOptional.get();
        assertThat(foundOrder.getOrderNumber()).isEqualTo(targetOrderNumber);
        assertThat(foundOrder.getOrderItems()).isNotNull().hasSize(2);
        assertThat(foundOrder.getOrderItems().getFirst().getBookId()).isEqualTo(order1Item1.getBookId());
    }

    @Test
    @DisplayName("주문 번호로 주문과 주문상품 함께 조회 - 실패: 존재하지 않는 주문번호")
    void findOrderWithItemsByOrderNumber_Failure_NotFound() {
        // given
        String nonExistentOrderNumber = "NonExistent";

        // when
        Optional<Order> resultOptional = orderRepository.findOrderWithItemsByOrderNumber(nonExistentOrderNumber);

        // then
        assertThat(resultOptional).isEmpty();
    }

    @Test
    @DisplayName("주문 ID로 기본 주문 정보 조회 - 성공")
    void findBaseOrderById_Success() {
        // given
        Long targetOrderId = order1.getOrderId();

        // when
        Optional<OrderBaseResponse> resultOptional = orderRepository.findBaseOrderById(targetOrderId);

        // then
        assertThat(resultOptional).isPresent();
        OrderBaseResponse response = resultOptional.get();
        assertThat(response.orderId()).isEqualTo(targetOrderId);
        assertThat(response.ordererInfo().ordererName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("주문 ID로 기본 주문 정보 조회 - 실패: 존재하지 않는 주문")
    void findBaseOrderById_Failure_NotFound() {
        // given
        Long nonExistentOrderId = 999L;

        // when
        Optional<OrderBaseResponse> resultOptional = orderRepository.findBaseOrderById(nonExistentOrderId);

        // then
        assertThat(resultOptional).isEmpty();
    }

    @Test
    @DisplayName("주문 번호로 비회원 주문 정보 조회 - 성공")
    void findNonMemberOrderByOrderNumber_Success() {
        // given: 이 테스트는 비회원 주문 데이터가 필요하므로 직접 생성합니다.
        Order nonMemberOrder = Order.createInitial(null, "encrypted-password", new OrdererInfo(null, null), new ReceiverInfo(null, null, null), OrderDetails.createInitial(null, null, 0, null));
        entityManager.persist(nonMemberOrder);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<NonMemberOrderBaseResponse> resultOptional = orderRepository.findNonMemberOrderByOrderNumber(nonMemberOrder.getOrderNumber());

        // then
        assertThat(resultOptional).isPresent();
        NonMemberOrderBaseResponse response = resultOptional.get();
        assertThat(response.orderNumber()).isEqualTo(nonMemberOrder.getOrderNumber());
        assertThat(response.nonMemberPassword()).isEqualTo("encrypted-password");
    }

    @Test
    @DisplayName("주문 번호로 비회원 주문 정보 조회 - 실패: 존재하지 않는 주문번호")
    void findNonMemberOrderByOrderNumber_Failure_NotFound() {
        // given
        String nonExistentOrderNumber = "NonExistent-123";

        // when
        Optional<NonMemberOrderBaseResponse> resultOptional = orderRepository.findNonMemberOrderByOrderNumber(nonExistentOrderNumber);

        // then
        assertThat(resultOptional).isEmpty();
    }

    @Test
    @DisplayName("회원 ID로 주문 목록 조회 - 성공 (주문 2건)")
    void findAllBaseOrderByMemberId_Success_WithResults() {
        // when: 테스트하려는 쿼리 메소드 호출
        Page<OrderBaseResponse> resultPage = orderRepository.findAllBaseOrderByMemberId(Pageable.unpaged(), targetMemberId);

        // then: 결과 검증
        assertThat(resultPage.getContent()).hasSize(2);
        assertThat(resultPage.getContent())
                .allMatch(orderBaseResponse -> orderBaseResponse.memberId().equals(targetMemberId));
        assertThat(resultPage.getContent().get(0).ordererInfo().ordererName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("회원 ID로 주문 목록 조회 - 성공 (주문 0건)")
    void findAllBaseOrderByMemberId_Success_WithNoResults() {
        // given
        long memberIdWithNoOrders = 999L;

        // when
        Page<OrderBaseResponse> resultPage = orderRepository.findAllBaseOrderByMemberId(Pageable.unpaged(), memberIdWithNoOrders);

        // then
        assertThat(resultPage.getContent()).isEmpty();
    }
}
