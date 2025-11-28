package com.nhnacademy.payment.config;

import com.nhnacademy.payment.dto.response.TossPaymentResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.View;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TossPaymentClient {
    private final WebClient tossWebClient;
    private final View error;

    @Value("${toss.secret-key}")
    private String tossSecretKey;

    public TossPaymentResponseDto confirm(String paymentKey, String orderNumber, Integer amount){
        return tossWebClient.post()
                .uri("/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION,getBasicAuthHeader())
                .bodyValue(Map.of(
                        "paymentKey",paymentKey,
                        "orderId",orderNumber,
                        "amount",amount
                ))
                .retrieve()
                //toss 에서 보내준 에러를 String 으로 상태코드가 4xx or 5xx만 잡아냄
                .onStatus(status ->
                        status.isError(), response -> response.bodyToMono(String.class)
                        .flatMap(error -> Mono.error(new RuntimeException("TossAPI 오류 " + error)))
                ).bodyToMono(TossPaymentResponseDto.class)
                .block();//동기
    }


    public TossPaymentResponseDto cancel(String paymentKey, String cancelReason){
        return tossWebClient.post()
                .uri("/payments/" +paymentKey+"/cancel")
                .header(HttpHeaders.AUTHORIZATION,getBasicAuthHeader())
                .bodyValue(Map.of("cancelReason",cancelReason))
                .retrieve()
                .onStatus(status ->
                        status.isError(), response -> response.bodyToMono(String.class)
                        .flatMap(error -> Mono.error(new RuntimeException("Toss 결제 취소 오류 " + error)))
                ).bodyToMono(TossPaymentResponseDto.class)
                .block();
    }

    private String getBasicAuthHeader(){
        return "Basic " + Base64.getEncoder()
                .encodeToString((tossSecretKey+":").getBytes(StandardCharsets.UTF_8));
    }
}
