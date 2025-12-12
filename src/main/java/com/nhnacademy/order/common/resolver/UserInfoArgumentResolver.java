package com.nhnacademy.order.common.resolver;

import com.nhnacademy.order.common.dto.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
public class UserInfoArgumentResolver implements HandlerMethodArgumentResolver {
    private static final String HEADER_USER_ID = "X-USER-ID";
    private static final String HEADER_USER_ROLE = "X-USER-ROLE";
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UserInfo.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String userIdStr = webRequest.getHeader(HEADER_USER_ID);
        String userRole = webRequest.getHeader(HEADER_USER_ROLE);

        log.info("요청 헤더 - X-USER-ID: {}, X-USER-ROLE: {}", userIdStr, userRole);

        // 비회원의 경우 null 반환
        // String 클래스의 isBlank()는 null 처리를 하지 못함 -> 추가적인 null 처리 필요
        if (userIdStr == null || userIdStr.isBlank() || userRole == null || userRole.isBlank()) {
            return null;
        }

        Long userId = Long.parseLong(userIdStr);

        return new UserInfo(userId, userRole);
    }
}
