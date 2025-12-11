package com.nhnacademy.order.common.aop;

import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.common.exception.AccessDeniedException;
import com.nhnacademy.order.order.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAspect {

    private final SecurityService securityService;

    @Around("@annotation(checkAuth)")
    public Object checkAuth(ProceedingJoinPoint joinPoint, CheckAuth checkAuth) throws Throwable {
        Object[] args = joinPoint.getArgs();

        UserInfo userInfo = (UserInfo) args[0];

        // 권한 확인
        AuthRole requiredRole = checkAuth.role();
        if (requiredRole == AuthRole.ADMIN) {
            if (!securityService.isAdmin(userInfo)) {
                throw new AccessDeniedException("관리자 권한이 아님");
            }
        } else if (requiredRole == AuthRole.MEMBER) {
            if (!securityService.isAuthenticated(userInfo)) {
                throw new AccessDeniedException("로그인이 필요한 기능");
            }
        }

        // 주문 소유자 확인
        if (checkAuth.checkOrderOwner()) {
            Long orderId = (Long) args[1];

            // 관리자는 소유자 체크 통과
            if (!securityService.isAdmin(userInfo) && !securityService.isOrderOwner(userInfo, orderId)) {
                throw new AccessDeniedException("주문 소유자 또는 관리자만 접근 가능한 기능");
            }
        }

        return joinPoint.proceed();
    }
}