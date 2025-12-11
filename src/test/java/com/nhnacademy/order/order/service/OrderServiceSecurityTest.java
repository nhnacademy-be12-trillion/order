package com.nhnacademy.order.order.service;

import com.nhnacademy.order.client.service.BookService;
import com.nhnacademy.order.client.service.CouponService;
import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.common.exception.AccessDeniedException;
import com.nhnacademy.order.delivery.repository.DeliveryPolicyRepository;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.dto.OrderItemStatusPatchRequest;
import com.nhnacademy.order.orderitem.repository.OrderItemRepository;
import com.nhnacademy.order.ordersaga.cancellation.service.OrderCancelOrchestrator;
import com.nhnacademy.order.ordersaga.creation.service.OrderCreateOrchestrator;
import com.nhnacademy.order.ordersaga.itemrefund.service.NonMemberOrderItemRefundOrchestrator;
import com.nhnacademy.order.ordersaga.itemrefund.service.OrderItemRefundOrchestrator;
import com.nhnacademy.order.packaging.repository.PackagingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "toss.secret-key=dummy-secret-key-for-test"
})
@Sql("/schema.sql")
class OrderServiceSecurityTest {

    @Autowired
    private OrderService orderService;

    @MockitoBean
    private OrderRepository orderRepository;
    @MockitoBean
    private OrderItemRepository orderItemRepository;
    @MockitoBean
    private PackagingRepository packagingRepository;
    @MockitoBean
    private DeliveryPolicyRepository deliveryPolicyRepository;
    @MockitoBean
    private BookService bookService;
    @MockitoBean
    private CouponService couponService;
    @MockitoBean
    private OrderCreateService orderCreateService;
    @MockitoBean
    private OrderCancelService orderCancelService;
    @MockitoBean
    private OrderCreateOrchestrator orderCreateOrchestrator;
    @MockitoBean
    private OrderCancelOrchestrator orderCancelOrchestrator;
    @MockitoBean
    private OrderItemRefundOrchestrator orderItemRefundOrchestrator;
    @MockitoBean
    private NonMemberOrderItemRefundOrchestrator nonMemberOrderItemRefundOrchestrator;
    @MockitoBean
    private SecurityService securityService;


    @Test
    @DisplayName("권한 실패: 일반 회원이 관리자 기능(전체 주문 조회) 호출")
    void findAllOrders_Failure_AccessDeniedForMember() {
        // given
        UserInfo memberInfo = new UserInfo(1L, "MEMBER");
        Pageable pageable = Pageable.ofSize(10);
        when(orderRepository.findAll(pageable)).thenReturn(Page.empty());

        // when & then
        assertThrows(AccessDeniedException.class, () -> {
            orderService.findAllOrders(memberInfo, pageable);
        });
    }

    private static Stream<Arguments> adminOnlyStatusChanges() {
        return Stream.of(
                Arguments.of(OrderItemStatus.SHIPPED),
                Arguments.of(OrderItemStatus.RETURNED)
        );
    }

    @DisplayName("권한 실패: 일반 회원이 관리자 전용 상태로 변경 시도")
    @ParameterizedTest(name = "관리자 전용 상태({0})로 변경 시도")
    @MethodSource("adminOnlyStatusChanges")
    void patchOrderItemStatus_Failure_AdminRoleRequired(OrderItemStatus adminOnlyStatus) {
        // given
        UserInfo memberInfo = new UserInfo(1L, "MEMBER");
        long orderId = 1L;
        long orderItemId = 101L;
        OrderItemStatusPatchRequest request = new OrderItemStatusPatchRequest(adminOnlyStatus);

        // @PreAuthorize는 통과시키기 위해, 주문 소유주가 맞다고 가정
        when(securityService.isOrderOwner(memberInfo, orderId)).thenReturn(true);
        when(securityService.isAdmin(memberInfo)).thenReturn(false);

        // 서비스 내부 로직이 동작하기 위한 최소한의 Mock 설정
        when(orderRepository.findOrderWithItemsByOrderId(orderId)).thenReturn(Optional.of(new Order()));
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(new OrderItem()));


        // when & then
        // @PreAuthorize는 통과하지만, 서비스 내부의 Strategy 권한 체크에서 실패해야 함
        assertThrows(AccessDeniedException.class, () -> {
            orderService.patchOrderItemStatus(memberInfo, orderId, orderItemId, request);
        });
    }
}
