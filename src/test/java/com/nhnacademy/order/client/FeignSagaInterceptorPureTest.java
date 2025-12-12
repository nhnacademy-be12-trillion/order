
package com.nhnacademy.order.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nhnacademy.order.client.dto.BookStocksRequest;
import com.nhnacademy.order.client.interceptor.FeignSagaIdInterceptor;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.MDC;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * SpringBoot의 도움 없이, 순수 Feign과 Wiremock으로 인터셉터의 동작을 테스트합니다.
 * 이 방식은 Spring의 복잡한 컨텍스트 로딩 문제로부터 완전히 자유롭습니다.
 */
class FeignSagaInterceptorPureTest {

    // JUnit 5 스타일로 WireMock 서버를 실행합니다.
    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private BookClient bookClient;

    @BeforeEach
    void setUp() {
        // 테스트마다 수동으로 Feign Client를 조립합니다.
        bookClient = Feign.builder()
                .client(new OkHttpClient()) // PATCH 메서드를 지원하는 OkHttp 클라이언트를 사용합니다.
                .contract(new SpringMvcContract()) // Spring MVC 어노테이션을 해석하도록 설정합니다.
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .requestInterceptor(new FeignSagaIdInterceptor()) // 테스트할 인터셉터를 직접 등록합니다.
                .target(BookClient.class, wireMockServer.baseUrl()); // WireMock 서버를 바라보도록 설정합니다.

        MDC.clear();
    }
    
    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void testDecreaseStocks_whenSagaIdInMdc_thenHeaderIsAdded() {
        // given: 테스트 준비
        String sagaId = UUID.randomUUID().toString();
        String expectedHeader = "X-SAGA-ID";
        BookStocksRequest requestBody = new BookStocksRequest(Map.of(1L, 1));

        wireMockServer.stubFor(patch(urlEqualTo("/api/order-books/decrease-stocks"))
                .willReturn(aResponse().withStatus(200)));

        MDC.put("sagaId", sagaId);

        // when: 테스트할 코드 실행
        bookClient.decreaseStocks(requestBody);

        // then: 결과 검증
        wireMockServer.verify(1, patchRequestedFor(urlEqualTo("/api/order-books/decrease-stocks"))
                .withHeader(expectedHeader, equalTo(sagaId)));
    }

    @Test
    void testDecreaseStocks_whenNoSagaIdInMdc_thenHeaderIsNotAdded() {
        // given: 테스트 준비
        String expectedHeader = "X-SAGA-ID";
        BookStocksRequest requestBody = new BookStocksRequest(Map.of(1L, 1));

        wireMockServer.stubFor(patch(urlEqualTo("/api/order-books/decrease-stocks"))
                .willReturn(aResponse().withStatus(200)));

        // when: 테스트할 코드 실행
        bookClient.decreaseStocks(requestBody);

        // then: 결과 검증
        wireMockServer.verify(1, patchRequestedFor(urlEqualTo("/api/order-books/decrease-stocks"))
                .withoutHeader(expectedHeader));
    }
}
