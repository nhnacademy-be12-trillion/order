package com.nhnacademy.order.order.repository;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.dto.NonMemberBaseResponse;
import com.nhnacademy.order.order.dto.OrderBaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
        SELECT o.memberId
        FROM Order o
        WHERE o.orderId = :orderId
    """)
    Optional<Long> findMemberIdByOrderId(Long orderId);

    @Query("""
        SELECT o
        FROM Order o
        JOIN FETCH o.orderItems
        WHERE o.orderId = :orderId
    """)
    Optional<Order> findOrderWithItemsById(Long orderId);

    @Query("""
        SELECT o.memberId
        FROM Order o
        WHERE o.orderNumber = :orderNumber
    """)
    Optional<Order> findOrderWithItemsByOrderNumber(String orderNumber);

    @Query("""
        SELECT new com.nhnacademy.order.order.dto.OrderBaseResponse(
            o.orderId,
            o.memberId,
            o.orderNumber,
            o.orderDetails.orderDate,
            o.orderStatus,
            o.orderDetails.totalPrice,
            o.orderDetails.deliveryFee,
            o.ordererInfo,
            o.receiverInfo
        )
        FROM Order o
        WHERE o.orderId = :orderId
    """)
    Optional<OrderBaseResponse> findBaseOrderById(Long orderId);

    @Query("""
        SELECT new com.nhnacademy.order.order.dto.OrderBaseResponse(
            o.orderId,
            o.memberId,
            o.orderNumber,
            o.orderDetails.orderDate,
            o.orderStatus,
            o.orderDetails.totalPrice,
            o.orderDetails.deliveryFee,
            o.ordererInfo,
            o.receiverInfo
        )
        FROM Order o
        WHERE o.memberId = :memberId
    """)
    Page<OrderBaseResponse> findAllBaseOrderByMemberId(Pageable pageable, Long memberId);

    @Query("""
        SELECT new com.nhnacademy.order.order.dto.NonMemberBaseResponse(
            o.orderId,
            o.nonMemberPassword,
            o.memberId,
            o.orderNumber,
            o.orderDetails.orderDate,
            o.orderStatus,
            o.orderDetails.totalPrice,
            o.orderDetails.deliveryFee,
            o.ordererInfo,
            o.receiverInfo
        )
        FROM Order o
        WHERE o.orderNumber = :orderNumber
    """)
    Optional<NonMemberBaseResponse> findNonMemberOrderByOrderNumber(String orderNumber);

}
