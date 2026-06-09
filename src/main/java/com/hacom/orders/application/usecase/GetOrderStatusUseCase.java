package com.hacom.orders.application.usecase;

import com.hacom.orders.domain.model.Order;
import com.hacom.orders.domain.model.vo.OrderId;
import reactor.core.publisher.Mono;

public interface GetOrderStatusUseCase {

    Mono<OrderResult> execute(OrderId orderId);

    record OrderResult(String orderId, String status, String customerId,
                       String customerPhoneNumber, String ts) {}
}