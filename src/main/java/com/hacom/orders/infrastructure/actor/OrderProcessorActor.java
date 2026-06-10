package com.hacom.orders.infrastructure.actor;

import akka.actor.UntypedAbstractActor;
import com.hacom.orders.domain.model.Order;
import com.hacom.orders.domain.model.vo.*;
import com.hacom.orders.domain.port.OrderRepository;
import com.hacom.orders.domain.port.SmsSender;
import com.hacom.orders.grpc.OrderRequest;
import com.hacom.orders.grpc.OrderResponse;
import com.hacom.orders.infrastructure.audit.AuditLog;
import com.hacom.orders.infrastructure.audit.AuditLogRepository;
import io.grpc.stub.StreamObserver;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class OrderProcessorActor extends UntypedAbstractActor {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessorActor.class);

    private final OrderRepository orderRepository;
    private final SmsSender smsSender;
    private final AuditLogRepository auditLogRepository;
    private final Tracer tracer;

    @Autowired
    public OrderProcessorActor(OrderRepository orderRepository, SmsSender smsSender,
                               AuditLogRepository auditLogRepository, Tracer tracer) {
        this.orderRepository = orderRepository;
        this.smsSender = smsSender;
        this.auditLogRepository = auditLogRepository;
        this.tracer = tracer;
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof ProcessOrderMessage) {
            ProcessOrderMessage msg = (ProcessOrderMessage) message;
            processOrderReactive(msg.getRequest(), msg.getResponseObserver());
        } else {
            unhandled(message);
        }
    }

    private void processOrderReactive(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        String orderId = request.getOrderId();
        log.info("Received order to process: orderId={}", orderId);

        Span processingSpan = tracer.nextSpan().name("processOrder")
                .tag("order.id", orderId)
                .tag("customer.id", request.getCustomerId())
                .tag("phone.number", request.getCustomerPhoneNumber())
                .start();

        try (var scope = tracer.withSpan(processingSpan)) {

            Order order = Order.create(
                    new OrderId(orderId),
                    new CustomerId(request.getCustomerId()),
                    new PhoneNumber(request.getCustomerPhoneNumber()),
                    request.getItemsList()
            );

            log.debug("Order aggregate created: {}", order);

            order.process();

            orderRepository.save(order).subscribe(
                    savedOrder -> {
                        log.info("Order saved successfully: orderId={}", orderId);

                        try {
                            String smsMessage = "Your order " + orderId + " has been processed";
                            try {
                                smsSender.sendSms(request.getCustomerPhoneNumber(), smsMessage);
                                log.info("SMS sent for order {}", orderId);
                            } catch (Exception e) {
                                log.error("Failed to send SMS for order {}: {}", orderId, e.getMessage());
                            }

                            AuditLog auditLog = new AuditLog(
                                    orderId, "ORDER_PROCESSED", "OrderProcessorActor",
                                    "Order processed successfully", "SUCCESS",
                                    tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : null
                            );
                            auditLogRepository.save(auditLog).subscribe();

                            OrderResponse response = OrderResponse.newBuilder()
                                    .setOrderId(orderId)
                                    .setStatus(order.getStatus())
                                    .build();

                            responseObserver.onNext(response);
                            responseObserver.onCompleted();

                            log.info("Order processing completed for: {}", orderId);
                        } finally {
                            processingSpan.end();
                        }
                    },
                    error -> {
                        try {
                            log.error("Error saving order {}: {}", orderId, error.getMessage(), error);
                            processingSpan.error(error);

                            AuditLog auditLog = new AuditLog(
                                    orderId, "ORDER_FAILED", "OrderProcessorActor",
                                    "Error saving order: " + error.getMessage(), "FAILED",
                                    tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : null
                            );
                            auditLogRepository.save(auditLog).subscribe();

                            respondWithError(request, responseObserver);
                        } finally {
                            processingSpan.end();
                        }
                    }
            );

        } catch (Exception e) {
            log.error("Error starting processing for order {}: {}", orderId, e.getMessage(), e);
            processingSpan.error(e);
            processingSpan.end();
            respondWithError(request, responseObserver);
        }
    }

    private void respondWithError(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        OrderResponse response = OrderResponse.newBuilder()
                .setOrderId(request.getOrderId())
                .setStatus("FAILED")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public static class ProcessOrderMessage {
        private final OrderRequest request;
        private final StreamObserver<OrderResponse> responseObserver;

        public ProcessOrderMessage(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
            this.request = request;
            this.responseObserver = responseObserver;
        }

        public OrderRequest getRequest() { return request; }
        public StreamObserver<OrderResponse> getResponseObserver() { return responseObserver; }
    }
}