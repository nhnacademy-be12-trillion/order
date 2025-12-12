package com.nhnacademy.cart.common.resolver;

import com.nhnacademy.cart.common.annotation.GuestOnly;
import com.nhnacademy.cart.dto.CartHolder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;

@Component
public class CartHolderResolver implements HandlerMethodArgumentResolver {
    private static final String COOKIE_NAME = "SESSION";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CartHolder.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        // @GuestOnly가 붙어있으면 -> 무조건 쿠키만 뒤져서 GuestHolder 리턴
        if (parameter.hasParameterAnnotation(GuestOnly.class)) {
            String guestId = getGuestIdFromCookie(request);
            // 병합 시 쿠키가 없으면 빈 껍데기라도 줘야 에러 안 남 (서비스에서 처리)
            if (guestId == null) {
                throw new IllegalArgumentException("비회원 세션 정보가 없습니다.");
            }
            return CartHolder.guest(guestId);
        }

        // 일반적인 경우: 헤더(회원) 우선
        String memberIdHeader = request.getHeader("X-User-Id");
        if (memberIdHeader != null) {
            try {
                return CartHolder.member(Long.valueOf(memberIdHeader));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("잘못된 회원 ID 형식입니다.");
            }
        }

        // -> 없으면 쿠키(비회원)
        String guestId = getGuestIdFromCookie(request);
        if (guestId != null) {
            return CartHolder.guest(guestId);
        }

        // 둘 다 없으면...
        throw new IllegalArgumentException("유저 인증이 필요합니다 (Header or Cookie missing)");
    }

    private String getGuestIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }
}