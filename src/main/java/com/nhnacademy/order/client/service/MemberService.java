package com.nhnacademy.order.client.service;

import com.nhnacademy.order.client.MemberClient;
import com.nhnacademy.order.client.dto.PointUsageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberClient memberClient;

    public void decreasePoint(Long sagaId, Long memberId, int point) {
        memberClient.decreasePoint(new PointUsageRequest(sagaId, memberId, point));
    }

    public void increasePoint(Long sagaId, Long memberId, int point) {
        memberClient.increasePoint(new PointUsageRequest(sagaId, memberId, point));
    }
}
