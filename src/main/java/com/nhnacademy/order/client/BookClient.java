package com.nhnacademy.order.client;

import com.nhnacademy.order.client.dto.BookResponse;
import com.nhnacademy.order.client.dto.BookStockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "book-service")
public interface BookClient {
    @GetMapping("/api/book/1")
    List<BookResponse> getOrderBookInfos(List<Long> bookIds);

    @PatchMapping
    void increaseStock(@RequestBody BookStockRequest request);

    @PatchMapping("/api/book/3")
    void decreaseStock(@RequestBody BookStockRequest request);
}
