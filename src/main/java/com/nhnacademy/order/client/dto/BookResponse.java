package com.nhnacademy.order.client.dto;

public record BookResponse(
    Long bookId,
    String bookName,
    int price,
    boolean canPackage,
    String imageUrl
) {}
