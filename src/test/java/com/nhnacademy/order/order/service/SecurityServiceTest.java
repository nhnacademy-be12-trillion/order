package com.nhnacademy.order.order.service;

import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.OrderDetails;
import com.nhnacademy.order.order.domain.OrdererInfo;
import com.nhnacademy.order.order.domain.ReceiverInfo;
import com.nhnacademy.order.order.exception.OrderNotFoundException;
import com.nhnacademy.order.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityService 단위 테스트")
class SecurityServiceTest {

    @InjectMocks
    private SecurityService securityService;

    @Mock
    private OrderRepository orderRepository;

    private UserInfo adminUser;
    private UserInfo memberUser;
    private UserInfo unauthenticatedUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        adminUser = new UserInfo(1L, "ADMIN");
        memberUser = new UserInfo(2L, "MEMBER");
        unauthenticatedUser = new UserInfo(null, null);
        
        OrdererInfo ordererInfo = new OrdererInfo("홍길동", "010-1234-5678");
        ReceiverInfo receiverInfo = new ReceiverInfo("이순신", "010-9876-5432", "서울시");
        OrderDetails orderDetails = OrderDetails.createInitial(
                "12345",
                LocalDateTime.now().plusDays(3),
                0,
                null
        );
        
        testOrder = Order.createInitial(
                2L,  // memberId
                null,
                ordererInfo,
                receiverInfo,
                orderDetails
        );
    }

    @Test
    @DisplayName("관리자 권한 확인 - 관리자")
    void isAdmin_True_AdminUser() {
        // when
        boolean result = securityService.isAdmin(adminUser);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("관리자 권한 확인 - 일반 회원")
    void isAdmin_False_MemberUser() {
        // when
        boolean result = securityService.isAdmin(memberUser);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("관리자 권한 확인 - 미인증 사용자")
    void isAdmin_False_UnauthenticatedUser() {
        // when
        boolean result = securityService.isAdmin(unauthenticatedUser);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("관리자 권한 확인 - null UserInfo")
    void isAdmin_False_NullUserInfo() {
        // when
        boolean result = securityService.isAdmin(null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("인증 여부 확인 - 인증된 사용자")
    void isAuthenticated_True_AuthenticatedUser() {
        // when
        boolean result = securityService.isAuthenticated(memberUser);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("인증 여부 확인 - 미인증 사용자")
    void isAuthenticated_False_UnauthenticatedUser() {
        // when
        boolean result = securityService.isAuthenticated(unauthenticatedUser);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("인증 여부 확인 - null UserInfo")
    void isAuthenticated_False_NullUserInfo() {
        // when
        boolean result = securityService.isAuthenticated(null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("주문 소유자 확인 - 소유자 본인")
    void isOrderOwner_True_Owner() {
        // given
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // when
        boolean result = securityService.isOrderOwner(memberUser, orderId);

        // then
        assertThat(result).isTrue();
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("주문 소유자 확인 - 다른 사용자")
    void isOrderOwner_False_DifferentUser() {
        // given
        Long orderId = 1L;
        UserInfo differentUser = new UserInfo(999L, "MEMBER");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // when
        boolean result = securityService.isOrderOwner(differentUser, orderId);

        // then
        assertThat(result).isFalse();
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("주문 소유자 확인 - 존재하지 않는 주문")
    void isOrderOwner_Exception_OrderNotFound() {
        // given
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> securityService.isOrderOwner(memberUser, orderId))
                .isInstanceOf(OrderNotFoundException.class);
        
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("주문 소유자 확인 - null UserInfo")
    void isOrderOwner_False_NullUserInfo() {
        // given
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // when
        boolean result = securityService.isOrderOwner(null, orderId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("주문 소유자 확인 - 비회원 주문")
    void isOrderOwner_False_NonMemberOrder() {
        // given
        Long orderId = 1L;
        OrdererInfo ordererInfo = new OrdererInfo("홍길동", "010-1234-5678");
        ReceiverInfo receiverInfo = new ReceiverInfo("이순신", "010-9876-5432", "서울시");
        OrderDetails orderDetails = OrderDetails.createInitial(
                "12345",
                LocalDateTime.now().plusDays(3),
                0,
                null
        );
        
        Order nonMemberOrder = Order.createInitial(
                null,  // 비회원 주문
                "password",
                ordererInfo,
                receiverInfo,
                orderDetails
        );
        
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(nonMemberOrder));

        // when
        boolean result = securityService.isOrderOwner(memberUser, orderId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("역할 확인 - ADMIN")
    void getRole_Admin() {
        // when
        Role role = securityService.getRole(adminUser);

        // then
        assertThat(role).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("역할 확인 - MEMBER")
    void getRole_Member() {
        // when
        Role role = securityService.getRole(memberUser);

        // then
        assertThat(role).isEqualTo(Role.MEMBER);
    }

    @Test
    @DisplayName("역할 확인 - GUEST")
    void getRole_Guest() {
        // when
        Role role = securityService.getRole(unauthenticatedUser);

        // then
        assertThat(role).isEqualTo(Role.GUEST);
    }

    @Test
    @DisplayName("역할 확인 - null UserInfo")
    void getRole_Null_ReturnsGuest() {
        // when
        Role role = securityService.getRole(null);

        // then
        assertThat(role).isEqualTo(Role.GUEST);
    }

    @Test
    @DisplayName("관리자 권한 확인 - 대소문자 구분")
    void isAdmin_CaseSensitive() {
        // given
        UserInfo lowerCaseAdmin = new UserInfo(1L, "admin");

        // when
        boolean result = securityService.isAdmin(lowerCaseAdmin);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("주문 소유자 확인 - 동일한 사용자 ID")
    void isOrderOwner_SameUserId() {
        // given
        Long orderId = 1L;
        UserInfo sameIdUser = new UserInfo(2L, "MEMBER");  // testOrder의 memberId와 동일
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

        // when
        boolean result = securityService.isOrderOwner(sameIdUser, orderId);

        // then
        assertThat(result).isTrue();
    }
}