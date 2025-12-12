package com.nhnacademy.order.common.resolver;

import com.nhnacademy.order.common.dto.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserInfoArgumentResolverTest {

    private UserInfoArgumentResolver resolver;
    private MethodParameter userInfoParameter;
    private MethodParameter otherParameter;
    private NativeWebRequest webRequest;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        resolver = new UserInfoArgumentResolver();
        webRequest = mock(NativeWebRequest.class);

        // UserInfo 파라미터를 가진 메서드를 모킹하기 위한 준비
        class TestController {
            public void testMethod(UserInfo userInfo) {}
            public void otherMethod(String other) {}
        }

        userInfoParameter = new MethodParameter(TestController.class.getMethod("testMethod", UserInfo.class), 0);
        otherParameter = new MethodParameter(TestController.class.getMethod("otherMethod", String.class), 0);
    }

    @Test
    @DisplayName("supportsParameter: UserInfo 타입 지원 확인")
    void testSupportsParameter_Success() {
        assertThat(resolver.supportsParameter(userInfoParameter)).isTrue();
    }

    @Test
    @DisplayName("supportsParameter: UserInfo 외 다른 타입은 지원하지 않음")
    void testSupportsParameter_Failure() {
        assertThat(resolver.supportsParameter(otherParameter)).isFalse();
    }

    @Test
    @DisplayName("resolveArgument: 헤더에 ID와 Role이 모두 존재할 때 UserInfo 객체 생성 성공")
    void testResolveArgument_Success() throws Exception {
        when(webRequest.getHeader("X-USER-ID")).thenReturn("123");
        when(webRequest.getHeader("X-USER-ROLE")).thenReturn("MEMBER");

        Object result = resolver.resolveArgument(userInfoParameter, null, webRequest, null);

        assertThat(result).isInstanceOf(UserInfo.class);
        UserInfo userInfo = (UserInfo) result;
        assertThat(userInfo.userId()).isEqualTo(123L);
        assertThat(userInfo.role()).isEqualTo("MEMBER");
    }

    @Test
    @DisplayName("resolveArgument: X-USER-ID 헤더가 없을 때 null 반환")
    void testResolveArgument_MissingUserId() throws Exception {
        when(webRequest.getHeader("X-USER-ID")).thenReturn(null);
        when(webRequest.getHeader("X-USER-ROLE")).thenReturn("MEMBER");

        Object result = resolver.resolveArgument(userInfoParameter, null, webRequest, null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("resolveArgument: X-USER-ROLE 헤더가 없을 때 null 반환")
    void testResolveArgument_MissingUserRole() throws Exception {
        when(webRequest.getHeader("X-USER-ID")).thenReturn("123");
        when(webRequest.getHeader("X-USER-ROLE")).thenReturn(null);

        Object result = resolver.resolveArgument(userInfoParameter, null, webRequest, null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("resolveArgument: 헤더 값이 비어있을 때 null 반환")
    void testResolveArgument_BlankHeaders() throws Exception {
        when(webRequest.getHeader("X-USER-ID")).thenReturn(" ");
        when(webRequest.getHeader("X-USER-ROLE")).thenReturn("  ");

        Object result = resolver.resolveArgument(userInfoParameter, null, webRequest, null);

        assertThat(result).isNull();
    }
    
    @Test
    @DisplayName("resolveArgument: X-USER-ID가 숫자가 아닐 때 NumberFormatException 발생")
    void testResolveArgument_InvalidUserId() {
        when(webRequest.getHeader("X-USER-ID")).thenReturn("not-a-number");
        when(webRequest.getHeader("X-USER-ROLE")).thenReturn("MEMBER");

        assertThatThrownBy(() -> resolver.resolveArgument(userInfoParameter, null, webRequest, null))
            .isInstanceOf(NumberFormatException.class);
    }
}
