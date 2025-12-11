package com.nhnacademy.order.client.dto;

public record BookResponse(
    Long bookId,
    String bookName,
    Integer price
) {}
