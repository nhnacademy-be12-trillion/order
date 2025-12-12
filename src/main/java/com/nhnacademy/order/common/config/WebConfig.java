package com.nhnacademy.order.common.config;

import com.nhnacademy.order.common.resolver.UserInfoArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final UserInfoArgumentResolver userInfoArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userInfoArgumentResolver);
    }

    // 로컬 테스트용 CORS 설정
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://http://localhost:10407/") // 프론트엔드 도메인
                .allowedMethods("*")
                // 💡 중요: 이 부분이 사용자 정의 헤더를 허용하는 설정입니다.
                .allowedHeaders("Content-Type", "X-USER-ID", "X-USER-ROLE", "Authorization");
    }
}