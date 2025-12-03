package com.nhnacademy.order.client.service;

import com.nhnacademy.order.client.BookClient;
import com.nhnacademy.order.client.dto.BookResponse;
import com.nhnacademy.order.client.dto.BookStocksRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService 단위 테스트")
class BookServiceTest {

    @InjectMocks
    private BookService bookService;

    @Mock
    private BookClient bookClient;

    private UUID testSagaId;
    private List<Long> testBookIds;
    private Map<Long, Integer> testQuantityMap;

    @BeforeEach
    void setUp() {
        testSagaId = UUID.randomUUID();
        testBookIds = Arrays.asList(1L, 2L, 3L);
        testQuantityMap = Map.of(
                1L, 2,
                2L, 3,
                3L, 1
        );
    }

    @Test
    @DisplayName("도서 정보 조회 성공 - 여러 도서")
    void getBookInfos_Success_MultipleBooks() {
        // given
        List<BookResponse> mockResponses = Arrays.asList(
                new BookResponse(1L, 10000),
                new BookResponse(2L, 15000),
                new BookResponse(3L, 20000)
        );
        when(bookClient.getOrderBookInfos(testBookIds)).thenReturn(mockResponses);

        // when
        Map<Long, BookResponse> result = bookService.getBookInfos(testBookIds);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(1L).price()).isEqualTo(10000);
        assertThat(result.get(2L).price()).isEqualTo(15000);
        assertThat(result.get(3L).price()).isEqualTo(20000);
        verify(bookClient, times(1)).getOrderBookInfos(testBookIds);
    }

    @Test
    @DisplayName("도서 정보 조회 성공 - 단일 도서")
    void getBookInfos_Success_SingleBook() {
        // given
        List<Long> singleBookId = Collections.singletonList(1L);
        List<BookResponse> mockResponses = Collections.singletonList(
                new BookResponse(1L, 10000)
        );
        when(bookClient.getOrderBookInfos(singleBookId)).thenReturn(mockResponses);

        // when
        Map<Long, BookResponse> result = bookService.getBookInfos(singleBookId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result).containsKey(1L);
        assertThat(result.get(1L).bookId()).isEqualTo(1L);
        assertThat(result.get(1L).price()).isEqualTo(10000);
    }

    @Test
    @DisplayName("도서 정보 조회 - 빈 리스트")
    void getBookInfos_EmptyList() {
        // given
        List<Long> emptyList = Collections.emptyList();
        when(bookClient.getOrderBookInfos(emptyList)).thenReturn(Collections.emptyList());

        // when
        Map<Long, BookResponse> result = bookService.getBookInfos(emptyList);

        // then
        assertThat(result).isEmpty();
        verify(bookClient, times(1)).getOrderBookInfos(emptyList);
    }

    @Test
    @DisplayName("도서 정보 조회 실패 - FeignClient 예외")
    void getBookInfos_Failure_FeignException() {
        // given
        when(bookClient.getOrderBookInfos(testBookIds))
                .thenThrow(new RuntimeException("External API error"));

        // when & then
        assertThatThrownBy(() -> bookService.getBookInfos(testBookIds))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("External API error");
    }

    @Test
    @DisplayName("재고 감소 성공")
    void decreaseStocks_Success() {
        // given
        doNothing().when(bookClient).decreaseStocks(any(BookStocksRequest.class));

        // when
        bookService.decreaseStocks(testSagaId, testQuantityMap);

        // then
        ArgumentCaptor<BookStocksRequest> captor = ArgumentCaptor.forClass(BookStocksRequest.class);
        verify(bookClient, times(1)).decreaseStocks(captor.capture());
        
        BookStocksRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.sagaId()).isEqualTo(testSagaId);
        assertThat(capturedRequest.quantityMap()).isEqualTo(testQuantityMap);
    }

    @Test
    @DisplayName("재고 감소 성공 - 단일 도서")
    void decreaseStocks_Success_SingleBook() {
        // given
        Map<Long, Integer> singleQuantity = Map.of(1L, 5);

        // when
        bookService.decreaseStocks(testSagaId, singleQuantity);

        // then
        ArgumentCaptor<BookStocksRequest> captor = ArgumentCaptor.forClass(BookStocksRequest.class);
        verify(bookClient).decreaseStocks(captor.capture());
        assertThat(captor.getValue().quantityMap()).containsEntry(1L, 5);
    }

    @Test
    @DisplayName("재고 감소 실패 - 외부 API 오류")
    void decreaseStocks_Failure_ExternalError() {
        // given
        doThrow(new RuntimeException("Stock decrease failed"))
                .when(bookClient).decreaseStocks(any(BookStocksRequest.class));

        // when & then
        assertThatThrownBy(() -> bookService.decreaseStocks(testSagaId, testQuantityMap))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Stock decrease failed");
    }

    @Test
    @DisplayName("재고 증가 성공")
    void increaseStocks_Success() {
        // given
        doNothing().when(bookClient).increaseStocks(any(BookStocksRequest.class));

        // when
        bookService.increaseStocks(testSagaId, testQuantityMap);

        // then
        ArgumentCaptor<BookStocksRequest> captor = ArgumentCaptor.forClass(BookStocksRequest.class);
        verify(bookClient, times(1)).increaseStocks(captor.capture());
        
        BookStocksRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.sagaId()).isEqualTo(testSagaId);
        assertThat(capturedRequest.quantityMap()).isEqualTo(testQuantityMap);
        assertThat(capturedRequest.quantityMap()).hasSize(3);
    }

    @Test
    @DisplayName("재고 증가 성공 - 빈 수량 맵")
    void increaseStocks_Success_EmptyMap() {
        // given
        Map<Long, Integer> emptyMap = Collections.emptyMap();

        // when
        bookService.increaseStocks(testSagaId, emptyMap);

        // then
        ArgumentCaptor<BookStocksRequest> captor = ArgumentCaptor.forClass(BookStocksRequest.class);
        verify(bookClient).increaseStocks(captor.capture());
        assertThat(captor.getValue().quantityMap()).isEmpty();
    }

    @Test
    @DisplayName("재고 증가 실패 - 외부 API 오류")
    void increaseStocks_Failure_ExternalError() {
        // given
        doThrow(new RuntimeException("Stock increase failed"))
                .when(bookClient).increaseStocks(any(BookStocksRequest.class));

        // when & then
        assertThatThrownBy(() -> bookService.increaseStocks(testSagaId, testQuantityMap))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Stock increase failed");
    }

    @Test
    @DisplayName("재고 작업 - 대량 도서 처리")
    void stockOperations_LargeQuantity() {
        // given
        Map<Long, Integer> largeQuantityMap = new HashMap<>();
        for (long i = 1; i <= 100; i++) {
            largeQuantityMap.put(i, (int) (i * 2));
        }

        // when
        bookService.decreaseStocks(testSagaId, largeQuantityMap);
        bookService.increaseStocks(testSagaId, largeQuantityMap);

        // then
        verify(bookClient, times(1)).decreaseStocks(any(BookStocksRequest.class));
        verify(bookClient, times(1)).increaseStocks(any(BookStocksRequest.class));
    }
}