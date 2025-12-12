package com.nhnacademy.order.client.service.mock;

import com.nhnacademy.order.client.BookClient;
import com.nhnacademy.order.client.dto.BookResponse;
import com.nhnacademy.order.client.dto.BookStocksRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Profile("local")
@Component
public class MockBookClient implements BookClient {

    @Override
    public List<BookResponse> getOrderBookInfos(List<Long> bookIds) {
        log.info("MockBookClient getOrderBookInfos called with bookIds: {}", bookIds);
        return bookIds.stream()
                .map(id -> new BookResponse(
                        id,
                        "Mock Book Title " + id,
                        25000,
                        true,
                        "http://example.com/mock_book_" + id + ".jpg"))
                .collect(Collectors.toList());
    }

    @Override
    public void increaseStocks(BookStocksRequest request) {
        log.info("MockBookClient increaseStocks called with: {}", request);
    }

    @Override
    public void decreaseStocks(BookStocksRequest request) {
        log.info("MockBookClient decreaseStocks called with: {}", request);
    }

    @Override
    public void rollbackStocks(BookStocksRequest request) {
        log.info("MockBookClient rollbackStocks called with: {}", request);
    }
}
