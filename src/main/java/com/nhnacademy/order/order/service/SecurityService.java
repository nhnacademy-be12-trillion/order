package com.nhnacademy.order.order.service;

import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.order.exception.OrderNotFoundException;
import com.nhnacademy.order.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SecurityService {
    private final OrderRepository orderRepository;

    // 관리자 권한 검사
    public boolean isAdmin(UserInfo userInfo) {
        return (userInfo != null && userInfo.role().equals("ADMIN"));
    }

    // 해당 주문의 소유자인지 검사
    public boolean isOrderOwner(UserInfo userInfo, Long orderId) {
        Long ownerMemberId = orderRepository.findMemberIdByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException("존재하지 않는 주문 ID: " + orderId));

        return userInfo.userId().equals(ownerMemberId);
    }

    // 로그인 사용자인지 검사
    public boolean isAuthenticated(UserInfo userInfo) {
        return (userInfo != null && userInfo.userId() != null);
    }
}
