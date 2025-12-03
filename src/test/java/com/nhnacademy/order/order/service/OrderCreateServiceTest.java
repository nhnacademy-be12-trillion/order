package com.nhnacademy.order.order.service;

import com.nhnacademy.order.order.domain.*;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.dto.OrderItemCreateRequest;
import com.nhnacademy.order.packaging.domain.Packaging;
import com.nhnacademy.order.packaging.repository.PackagingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCreateService 단위 테스트")
class OrderCreateServiceTest {

    @InjectMocks
    private OrderCreateService orderCreateService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PackagingRepository packagingRepository;

    private Long testMemberId;
    private String testPassword;
    private OrdererInfo testOrdererInfo;
    private ReceiverInfo testReceiverInfo;
    private OrderDetails testOrderDetails;
    private List<OrderItemCreateRequest> testItemRequests;

    @BeforeEach
    void setUp() {
        testMemberId = 1L;
        testPassword = "encoded_password";
        testOrdererInfo = new OrdererInfo("홍길동", "010-1234-5678");
        testReceiverInfo = new ReceiverInfo("이순신", "010-9876-5432", "서울시 강남구");
        testOrderDetails = OrderDetails.createInitial(
                "12345",
                LocalDateTime.now().plusDays(3),
                1000,
                1L
        );
        testItemRequests = Arrays.asList(
                new OrderItemCreateRequest(1L, 2, 1L, LocalDateTime.now().plusDays(3)),
                new OrderItemCreateRequest(2L, 1, 2L, LocalDateTime.now().plusDays(3))
        );
    }

    @Test
    @DisplayName("초기 주문 생성 성공 - 회원")
    void createInitialOrder_Success_Member() {
        // given
        List<Packaging> packagings = Arrays.asList(
                new Packaging(1L, "기본 포장", 500),
                new Packaging(2L, "선물 포장", 1000)
        );
        when(packagingRepository.findAllById(anyList())).thenReturn(packagings);
        
        Order mockOrder = Order.createInitial(
                testMemberId,
                null,
                testOrdererInfo,
                testReceiverInfo,
                testOrderDetails
        );
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        // when
        Order result = orderCreateService.createInitialOrder(
                testMemberId,
                null,
                testOrdererInfo,
                testReceiverInfo,
                testOrderDetails,
                testItemRequests
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(testMemberId);
        verify(packagingRepository, times(1)).findAllById(anyList());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("초기 주문 생성 성공 - 비회원")
    void createInitialOrder_Success_NonMember() {
        // given
        List<Packaging> packagings = Collections.singletonList(
                new Packaging(1L, "기본 포장", 500)
        );
        when(packagingRepository.findAllById(anyList())).thenReturn(packagings);
        
        Order mockOrder = Order.createInitial(
                null,
                testPassword,
                testOrdererInfo,
                testReceiverInfo,
                testOrderDetails
        );
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        // when
        Order result = orderCreateService.createInitialOrder(
                null,
                testPassword,
                testOrdererInfo,
                testReceiverInfo,
                testOrderDetails,
                testItemRequests
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isNull();
        assertThat(result.getNonMemberPassword()).isEqualTo(testPassword);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("초기 주문 생성 - 포장 없음")
    void createInitialOrder_NoPackaging() {
        // given
        List<OrderItemCreateRequest> noPackagingRequests = Collections.singletonList(
                new OrderItemCreateRequest(1L, 1, null, LocalDateTime.now().plusDays(3))
        );
        when(packagingRepository.findAllById(anyList())).thenReturn(Collections.emptyList());
        
        Order mockOrder = Order.createInitial(
                testMemberId,
                null,
                testOrdererInfo,
                testReceiverInfo,
                testOrderDetails
        );
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        // when
        Order result = orderCreateService.createInitialOrder(
                testMemberId,
                null,
                testOrdererInfo,
                testReceiverInfo,
                testOrderDetails,
                noPackagingRequests
        );

        // then
        assertThat(result).isNotNull();
        verify(packagingRepository, times(1)).findAllById(anyList());
    }

    @Test
    @DisplayName("주문 완료 처리 성공")
    void completeOrder_Success() {
        // given
        Order mockOrder = Order.createInitial(
                testMemberId,
                null,
                testOrdererInfo,
                testReceiverInfo,
                testOrderDetails
        );
        
        List<OrderItem> completedItems = Arrays.asList(
                OrderItem.createInitial(mockOrder, 1L, 2, LocalDateTime.now().plusDays(3), 500),
                OrderItem.createInitial(mockOrder, 2L, 1, LocalDateTime.now().plusDays(3), 1000)
        );
        
        int originPrice = 50000;
        int totalPrice = 48000;
        int deliveryFee = 2500;
        
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        // when
        orderCreateService.completeOrder(mockOrder, originPrice, totalPrice, deliveryFee, completedItems);

        // then
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(captor.capture());
        
        Order savedOrder = captor.getValue();
        assertThat(savedOrder.getOrderDetails().originPrice()).isEqualTo(originPrice);
        assertThat(savedOrder.getOrderDetails().totalPrice()).isEqualTo(totalPrice);
        assertThat(savedOrder.getOrderDetails().deliveryFee()).isEqualTo(deliveryFee);
    }

    @Test
    @DisplayName("주문 완료 처리 - 빈 주문 항목")
    void completeOrder_EmptyItems() {
        // given
        Order mockOrder = Order.createInitial(
                testMemberId,
                null,
                testOrdererInfo,
                testReceiverInfo,
                testOrderDetails
        );
        
        List<OrderItem> emptyItems = Collections.emptyList();
        
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        // when
        orderCreateService.completeOrder(mockOrder, 0, 0, 0, emptyItems);

        // then
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 실패 처리")
    void createFailureOrder_Success() {
        // given
        Order mockOrder = Order.createInitial(
                testMemberId,
                null,
                testOrdererInfo,
                testReceiverInfo,
                testOrderDetails
        );

        // when
        orderCreateService.createFailureOrder(mockOrder);

        // then
        assertThat(mockOrder.getOrderStatus()).isEqualTo(OrderStatus.CREATION_FAILED);
    }

    @Test
    @DisplayName("초기 주문 생성 - 대량 주문 항목")
    void createInitialOrder_LargeNumberOfItems() {
        // given
        List<OrderItemCreateRequest> largeItemRequests = new java.util.ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            largeItemRequests.add(new OrderItemCreateRequest(
                    (long) i, 1, 1L, LocalDateTime.now().plusDays(3)
            ));
        }
        
        List<Packaging> packagings = Collections.singletonList(
                new Packaging(1L, "기본 포장", 500)
        );
        when(packagingRepository.findAllById(anyList())).thenReturn(packagings);
        
        Order mockOrder = Order.createInitial(
                testMemberId,
                null,
                testOrdererInfo,
                testReceiverInfo,
                testOrderDetails
        );
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        // when
        Order result = orderCreateService.createInitialOrder(
                testMemberId,
                null,
                testOrdererInfo,
                testReceiverInfo,
                testOrderDetails,
                largeItemRequests
        );

        // then
        assertThat(result).isNotNull();
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 완료 처리 - 높은 금액")
    void completeOrder_HighAmount() {
        // given
        Order mockOrder = Order.createInitial(
                testMemberId,
                null,
                testOrdererInfo,
                testReceiverInfo,
                testOrderDetails
        );
        
        List<OrderItem> items = Collections.singletonList(
                OrderItem.createInitial(mockOrder, 1L, 10, LocalDateTime.now().plusDays(3), 0)
        );
        
        int originPrice = 1000000;
        int totalPrice = 950000;
        int deliveryFee = 0;

        // when
        orderCreateService.completeOrder(mockOrder, originPrice, totalPrice, deliveryFee, items);

        // then
        verify(orderRepository, times(1)).save(any(Order.class));
    }
}