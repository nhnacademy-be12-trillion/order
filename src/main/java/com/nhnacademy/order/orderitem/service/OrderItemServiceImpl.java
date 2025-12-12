package com.nhnacademy.order.orderitem.service;

import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderItemServiceImpl implements OrderItemService {
    private final OrderItemRepository orderItemRepository;

    @Override
    public List<Long> getTopNSellingBookIds(int limit) {
        List<OrderItemStatus> excludeStatuses = List.of(OrderItemStatus.CANCELED, OrderItemStatus.RETURNED);

        // Pageable을 사용하여 효율적인 쿼리 수행
        Pageable topN = PageRequest.of(0, limit);

        return orderItemRepository.findTopNSellingBookIds(excludeStatuses, topN);
    }
}
