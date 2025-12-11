package com.nhnacademy.order.client;

import com.nhnacademy.order.client.dto.BookResponse;
import com.nhnacademy.order.client.dto.BookStocksRequest;
import com.nhnacademy.order.common.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "book-service", configuration = FeignClientConfig.class)
public interface BookClient {
    @GetMapping("/api/book/1")
    List<BookResponse> getOrderBookInfos(@RequestParam("bookIds") List<Long> bookIds);

    @PatchMapping("/api/book/2")
    void increaseStocks(@RequestBody BookStocksRequest request);

    @PatchMapping("/api/book/3")
    void decreaseStocks(@RequestBody BookStocksRequest request);
}
