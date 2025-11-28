package com.nhnacademy.order.order.service;

import com.nhnacademy.order.order.domain.OrderSaga;
import com.nhnacademy.order.order.domain.SagaStatus;
import com.nhnacademy.order.order.domain.SagaStep;
import com.nhnacademy.order.order.repository.OrderSagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SagaUpdateService {
    private final OrderSagaRepository orderSagaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSagaStep(OrderSaga orderSaga, SagaStep step) {
        orderSaga.setLastCompletedStep(step);
        orderSagaRepository.save(orderSaga);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSagaStatus(OrderSaga orderSaga, SagaStatus status) {
        orderSaga.setOverallStatus(status);
        orderSagaRepository.save(orderSaga);
    }
}