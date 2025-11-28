package com.nhnacademy.order.client.service;

import com.nhnacademy.order.client.BookClient;
import com.nhnacademy.order.client.dto.BookResponse;
import com.nhnacademy.order.client.dto.BookStockRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 추후 외부 API 호출 관련 로직(ex) 로깅) 추가를 고려해 서비스 계층으로 분리 (확장성)
@Service
@RequiredArgsConstructor
public class BookService {
    private final BookClient bookClient;

    public Map<Long, BookResponse> getBookInfos(List<Long> bookIds) {
        List<BookResponse> bookResponses = bookClient.getOrderBookInfos(bookIds);

        return bookResponses.stream()
                .collect(Collectors.toMap(BookResponse::bookId, Function.identity()));
    }

    public void decreaseStock(Long sagaId, Map<Long, Integer> quantityMap) {
        bookClient.decreaseStock(new BookStockRequest(sagaId, quantityMap));
    }

    public void increaseStock(Long sagaId, Map<Long, Integer> quantityMap) {
        bookClient.increaseStock(new BookStockRequest(sagaId, quantityMap));
    }
}
