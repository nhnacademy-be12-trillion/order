package com.nhnacademy.order.orderitem.repository;

import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.dto.OrderItemResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("""
        SELECT new com.nhnacademy.order.orderitem.dto.OrderItemResponse(
            oi.orderItemId,
            oi.order.orderId,
            oi.bookId,
            oi.bookName,
            oi.bookImage,
            oi.quantity,
            oi.price,
            oi.couponDiscountAmount,
            oi.packagingPrice,
            oi.orderItemStatus
        )
        FROM OrderItem oi
        WHERE oi.order.orderId = :orderId
    """)
    List<OrderItemResponse> findOrderItemByOrder_OrderId(Long orderId);

    @Query("""
        SELECT new com.nhnacademy.order.orderitem.dto.OrderItemResponse(
            oi.orderItemId,
            oi.order.orderId,
            oi.bookId,
            oi.bookName,
            oi.bookImage,
            oi.quantity,
            oi.price,
            oi.couponDiscountAmount,
            oi.packagingPrice,
            oi.orderItemStatus
        )
        FROM OrderItem oi
        WHERE oi.order.orderId IN :orderIds
    """)
    List<OrderItemResponse> findAllByOrderIds(List<Long> orderIds);


    @Query("""
        SELECT oi.bookId
        FROM OrderItem oi
        WHERE oi.orderItemStatus NOT IN :excludeStatuses AND oi.quantity > 0
        GROUP BY oi.bookId
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<Long> findTopNSellingBookIds(List<OrderItemStatus> excludeStatuses, Pageable pageable);
}
