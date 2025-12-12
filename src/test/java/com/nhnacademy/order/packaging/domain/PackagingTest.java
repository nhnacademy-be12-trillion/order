package com.nhnacademy.order.packaging.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PackagingTest {

    @Test
    @DisplayName("기본 생성자 테스트")
    void testNoArgsConstructor() {
        Packaging packaging = new Packaging();
        assertThat(packaging).isNotNull();
    }

    @Test
    @DisplayName("전체 인자 생성자 및 Getter 테스트")
    void testAllArgsConstructorAndGetters() {
        Long id = 1L;
        String type = "Box";
        int price = 1000;

        Packaging packaging = new Packaging(id, type, price);

        assertThat(packaging.getPackagingId()).isEqualTo(id);
        assertThat(packaging.getPackagingType()).isEqualTo(type);
        assertThat(packaging.getPackagingPrice()).isEqualTo(price);
    }

    @Test
    @DisplayName("create 팩토리 메서드 테스트")
    void testCreateFactoryMethod() {
        String type = "Vinyl";
        int price = 100;

        Packaging packaging = Packaging.create(type, price);

        assertThat(packaging.getPackagingId()).isNull();
        assertThat(packaging.getPackagingType()).isEqualTo(type);
        assertThat(packaging.getPackagingPrice()).isEqualTo(price);
    }

    @Test
    @DisplayName("updatePrice 메서드 테스트")
    void testUpdatePrice() {
        Packaging packaging = new Packaging(1L, "Box", 1000);
        int newPrice = 1200;

        packaging.updatePrice(newPrice);

        assertThat(packaging.getPackagingPrice()).isEqualTo(newPrice);
    }
}
