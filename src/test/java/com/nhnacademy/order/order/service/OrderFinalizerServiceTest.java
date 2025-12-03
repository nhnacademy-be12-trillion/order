package com.nhnacademy.order.order.service;

import com.nhnacademy.order.client.dto.BookResponse;
import com.nhnacademy.order.client.service.BookService;
import com.nhnacademy.order.client.service.CouponService;
import com.nhnacademy.order.delivery.domain.DeliveryPolicy;
import com.nhnacademy.order.delivery.exception.PolicyNotConfiguredException;
import com.nhnacademy.order.delivery.repository.DeliveryPolicyRepository;
import com.nhnacademy.order.order.domain.*;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderFinalizerService 단위 테스트")
class OrderFinalizerServiceTest {

    @InjectMocks
    private OrderFinalizerService orderFinalizerService;

    @Mock
    private DeliveryPolicyRepository deliveryPolicyRepository;

    @Mock
    private BookService bookService;

    @Mock
    private CouponService couponService;

    @Mock
    private OrderRepository orderRepository;

    private Order testOrder;
    private DeliveryPolicy testDeliveryPolicy;

    @BeforeEach
    void setUp() {
        OrdererInfo ordererInfo = new OrdererInfo("홍길동", "010-1234-5678");
        ReceiverInfo receiverInfo = new ReceiverInfo("이순신", "010-9876-5432", "서울시");
        OrderDetails orderDetails = OrderDetails.createInitial(
                "12345",
                LocalDateTime.now().plusDays(3),
                1000,
                1L
        );
        
        testOrder = Order.createInitial(
                1L,
                null,
                ordererInfo,
                receiverInfo,
                orderDetails
        );
        testOrder.setOrderStatus(OrderStatus.AWAITING_POST_PROCESSING);
        
        testDeliveryPolicy = new DeliveryPolicy(1L, 50000, 2500);
    }

    @Test
    @DisplayName("주문 최종 처리 성공 - 쿠폰 있음, 무료 배송")
    void finalizeOrderCreation_Success_WithCouponAndFreeDelivery() {
        // given
        OrderItem item1 = OrderItem.createInitial(testOrder, 1L, 2, LocalDateTime.now().plusDays(3), 500);
        OrderItem item2 = OrderItem.createInitial(testOrder, 2L, 1, LocalDateTime.now().plusDays(3), 1000);
        testOrder.addOrderItem(item1);
        testOrder.addOrderItem(item2);
        
        Map<Long, BookResponse> bookInfoMap = Map.of(
                1L, new BookResponse(1L, 25000),
                2L, new BookResponse(2L, 30000)
        );
        
        when(bookService.getBookInfos(anyList())).thenReturn(bookInfoMap);
        when(couponService.calculateDiscount(anyLong(), anyInt())).thenReturn(5000);
        when(deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc())
                .thenReturn(Optional.of(testDeliveryPolicy));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        orderFinalizerService.finalizeOrderCreation(testOrder);

        // then
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        
        Order savedOrder = captor.getValue();
        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(savedOrder.getOrderDetails().deliveryFee()).isZero();  // 무료 배송
        assertThat(savedOrder.getOrderDetails().originPrice()).isEqualTo(81500);  // (25000*2 + 500) + (30000*1 + 1000)
    }

    @Test
    @DisplayName("주문 최종 처리 성공 - 쿠폰 없음, 배송비 부과")
    void finalizeOrderCreation_Success_NoCouponWithDeliveryFee() {
        // given
        OrderDetails detailsWithoutCoupon = OrderDetails.createInitial(
                "12345",
                LocalDateTime.now().plusDays(3),
                0,
                null  // 쿠폰 없음
        );
        
        OrdererInfo ordererInfo = new OrdererInfo("홍길동", "010-1234-5678");
        ReceiverInfo receiverInfo = new ReceiverInfo("이순신", "010-9876-5432", "서울시");
        
        Order orderWithoutCoupon = Order.createInitial(
                1L,
                null,
                ordererInfo,
                receiverInfo,
                detailsWithoutCoupon
        );
        orderWithoutCoupon.setOrderStatus(OrderStatus.AWAITING_POST_PROCESSING);
        
        OrderItem item = OrderItem.createInitial(orderWithoutCoupon, 1L, 1, LocalDateTime.now().plusDays(3), 0);
        orderWithoutCoupon.addOrderItem(item);
        
        Map<Long, BookResponse> bookInfoMap = Map.of(1L, new BookResponse(1L, 10000));
        
        when(bookService.getBookInfos(anyList())).thenReturn(bookInfoMap);
        when(deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc())
                .thenReturn(Optional.of(testDeliveryPolicy));
        when(orderRepository.save(any(Order.class))).thenReturn(orderWithoutCoupon);

        // when
        orderFinalizerService.finalizeOrderCreation(orderWithoutCoupon);

        // then
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        
        Order savedOrder = captor.getValue();
        assertThat(savedOrder.getOrderDetails().deliveryFee()).isEqualTo(2500);
        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("주문 최종 처리 - 이미 처리된 주문은 재처리하지 않음")
    void finalizeOrderCreation_AlreadyProcessed_NoReprocessing() {
        // given
        testOrder.setOrderStatus(OrderStatus.PENDING);

        // when
        orderFinalizerService.finalizeOrderCreation(testOrder);

        // then
        verify(bookService, never()).getBookInfos(anyList());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 최종 처리 실패 - 배송 정책 없음")
    void finalizeOrderCreation_Failure_NoDeliveryPolicy() {
        // given
        OrderItem item = OrderItem.createInitial(testOrder, 1L, 1, LocalDateTime.now().plusDays(3), 0);
        testOrder.addOrderItem(item);
        
        Map<Long, BookResponse> bookInfoMap = Map.of(1L, new BookResponse(1L, 10000));
        
        when(bookService.getBookInfos(anyList())).thenReturn(bookInfoMap);
        when(deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc())
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderFinalizerService.finalizeOrderCreation(testOrder))
                .isInstanceOf(PolicyNotConfiguredException.class)
                .hasMessageContaining("배송 정책이 설정되지 않음");
    }

    @ParameterizedTest
    @CsvSource({
            "10000, 2500",
            "50000, 0",
            "100000, 0",
            "49999, 2500"
    })
    @DisplayName("주문 최종 처리 - 배송비 결정 테스트")
    void finalizeOrderCreation_DeliveryFeeCalculation(int bookPrice, int expectedDeliveryFee) {
        // given
        OrderDetails detailsWithoutCoupon = OrderDetails.createInitial(
                "12345",
                LocalDateTime.now().plusDays(3),
                0,
                null
        );
        
        OrdererInfo ordererInfo = new OrdererInfo("홍길동", "010-1234-5678");
        ReceiverInfo receiverInfo = new ReceiverInfo("이순신", "010-9876-5432", "서울시");
        
        Order order = Order.createInitial(1L, null, ordererInfo, receiverInfo, detailsWithoutCoupon);
        order.setOrderStatus(OrderStatus.AWAITING_POST_PROCESSING);
        
        OrderItem item = OrderItem.createInitial(order, 1L, 1, LocalDateTime.now().plusDays(3), 0);
        order.addOrderItem(item);
        
        Map<Long, BookResponse> bookInfoMap = Map.of(1L, new BookResponse(1L, bookPrice));
        
        when(bookService.getBookInfos(anyList())).thenReturn(bookInfoMap);
        when(deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc())
                .thenReturn(Optional.of(testDeliveryPolicy));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // when
        orderFinalizerService.finalizeOrderCreation(order);

        // then
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderDetails().deliveryFee()).isEqualTo(expectedDeliveryFee);
    }

    @Test
    @DisplayName("주문 최종 처리 - 포인트 사용으로 최종 금액이 0이 되는 경우")
    void finalizeOrderCreation_FinalPriceZeroWithPoints() {
        // given
        OrderDetails detailsWithLargePoints = OrderDetails.createInitial(
                "12345",
                LocalDateTime.now().plusDays(3),
                20000,  // 큰 포인트 사용
                null
        );
        
        OrdererInfo ordererInfo = new OrdererInfo("홍길동", "010-1234-5678");
        ReceiverInfo receiverInfo = new ReceiverInfo("이순신", "010-9876-5432", "서울시");
        
        Order order = Order.createInitial(1L, null, ordererInfo, receiverInfo, detailsWithLargePoints);
        order.setOrderStatus(OrderStatus.AWAITING_POST_PROCESSING);
        
        OrderItem item = OrderItem.createInitial(order, 1L, 1, LocalDateTime.now().plusDays(3), 0);
        order.addOrderItem(item);
        
        Map<Long, BookResponse> bookInfoMap = Map.of(1L, new BookResponse(1L, 10000));
        
        when(bookService.getBookInfos(anyList())).thenReturn(bookInfoMap);
        when(deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc())
                .thenReturn(Optional.of(testDeliveryPolicy));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // when
        orderFinalizerService.finalizeOrderCreation(order);

        // then
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderDetails().totalPrice()).isZero();
    }

    @Test
    @DisplayName("주문 최종 처리 - 여러 주문 항목")
    void finalizeOrderCreation_MultipleOrderItems() {
        // given
        OrderItem item1 = OrderItem.createInitial(testOrder, 1L, 2, LocalDateTime.now().plusDays(3), 500);
        OrderItem item2 = OrderItem.createInitial(testOrder, 2L, 3, LocalDateTime.now().plusDays(3), 1000);
        OrderItem item3 = OrderItem.createInitial(testOrder, 3L, 1, LocalDateTime.now().plusDays(3), 0);
        testOrder.addOrderItem(item1);
        testOrder.addOrderItem(item2);
        testOrder.addOrderItem(item3);
        
        Map<Long, BookResponse> bookInfoMap = Map.of(
                1L, new BookResponse(1L, 10000),
                2L, new BookResponse(2L, 15000),
                3L, new BookResponse(3L, 20000)
        );
        
        when(bookService.getBookInfos(anyList())).thenReturn(bookInfoMap);
        when(couponService.calculateDiscount(anyLong(), anyInt())).thenReturn(3000);
        when(deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc())
                .thenReturn(Optional.of(testDeliveryPolicy));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        orderFinalizerService.finalizeOrderCreation(testOrder);

        // then
        verify(bookService).getBookInfos(anyList());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 최종 처리 - 쿠폰 할인 금액 계산 검증")
    void finalizeOrderCreation_CouponDiscountCalculation() {
        // given
        OrderItem item = OrderItem.createInitial(testOrder, 1L, 1, LocalDateTime.now().plusDays(3), 0);
        testOrder.addOrderItem(item);
        
        Map<Long, BookResponse> bookInfoMap = Map.of(1L, new BookResponse(1L, 30000));
        
        when(bookService.getBookInfos(anyList())).thenReturn(bookInfoMap);
        when(couponService.calculateDiscount(eq(1L), eq(30000))).thenReturn(3000);
        when(deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc())
                .thenReturn(Optional.of(testDeliveryPolicy));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        orderFinalizerService.finalizeOrderCreation(testOrder);

        // then
        verify(couponService).calculateDiscount(eq(1L), eq(30000));
    }
}