package com.hacom.orders.application.usecase;

import com.hacom.orders.domain.model.vo.OrderId;
import com.hacom.orders.domain.model.vo.OrderStatus;

public interface ProcessOrderUseCase {

    ProcessOrderResult execute(ProcessOrderCommand command);

    record ProcessOrderCommand(OrderId orderId, String customerId, String customerPhoneNumber,
                               java.util.List<String> items) {}

    record ProcessOrderResult(OrderId orderId, OrderStatus status) {}
}