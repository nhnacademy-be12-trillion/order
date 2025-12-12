package com.nhnacademy.cart.common.config;

import com.nhnacademy.cart.common.resolver.CartHolderResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class  CartWebConfig implements WebMvcConfigurer {
    private final CartHolderResolver cartHolderResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(cartHolderResolver);
    }
}