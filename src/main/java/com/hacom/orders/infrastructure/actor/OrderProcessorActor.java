package com.hacom.orders.infrastructure.actor;

import akka.actor.UntypedAbstractActor;
import akka.pattern.Patterns;
import com.hacom.orders.application.config.TracingConfig;
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

import java.time.OffsetDateTime;
import java.util.concurrent.CompletionStage;

/**
 * Akka Classic Actor for processing orders.
 * Uses reactive streams (pipe pattern) instead of blocking .block() calls.
 * Integrates OpenTelemetry tracing and audit logging.
 */
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
        } else if (message instanceof OrderSavedMessage) {
            onOrderSaved((OrderSavedMessage) message);
        } else {
            unhandled(message);
        }
    }

    /**
     * Reactive order processing: uses pipe() to avoid blocking.
     */
    private void processOrderReactive(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        String orderId = request.getOrderId();
        log.info("Received order to process (reactive): orderId={}", orderId);

        // Create trace span for the entire processing
        Span processingSpan = tracer.nextSpan().name("processOrder")
                .tag("order.id", orderId)
                .tag("customer.id", request.getCustomerId())
                .tag("phone.number", request.getCustomerPhoneNumber())
                .start();

        try (var scope = tracer.withSpan(processingSpan)) {

            // 1. Create domain Order using the Aggregate Root factory
            Order order = Order.create(
                    new OrderId(orderId),
                    new CustomerId(request.getCustomerId()),
                    new PhoneNumber(request.getCustomerPhoneNumber()),
                    request.getItemsList()
            );

            log.debug("Order aggregate created: {}", order);

            // 2. Save to MongoDB reactively and pipe result back to self
            CompletionStage<Order> saveFuture = orderRepository.save(order).toFuture();
            CompletionStage<Object> pipedSave = saveFuture.thenApply(savedOrder ->
                    new OrderSavedMessage(savedOrder, request, responseObserver)
            );

            // Pipe the result back to this actor - NO .block()!
            Patterns.pipe(pipedSave, getContext().dispatcher()).to(getSelf());

        } catch (Exception e) {
            log.error("Error starting reactive processing for order {}: {}", orderId, e.getMessage(), e);
            processingSpan.error(e);
            respondWithError(request, responseObserver);
        } finally {
            processingSpan.end();
        }
    }

    /**
     * Called when the order has been successfully saved to MongoDB.
     */
    private void onOrderSaved(OrderSavedMessage msg) {
        Order order = msg.getOrder();
        OrderRequest request = msg.getRequest();
        StreamObserver<OrderResponse> responseObserver = msg.getResponseObserver();
        String orderId = request.getOrderId();

        Span smsSpan = tracer.nextSpan().name("sendSms")
                .tag("order.id", orderId)
                .tag("destination", request.getCustomerPhoneNumber())
                .start();

        try (var scope = tracer.withSpan(smsSpan)) {

            // 3. Process the order (domain logic)
            order.process();

            log.info("Order processed successfully: orderId={}, status={}", orderId, order.getStatus());

            // 4. Save audit log (non-blocking fire-and-forget)
            AuditLog auditLog = new AuditLog(
                    orderId, "ORDER_PROCESSED", "OrderProcessorActor",
                    "Order processed successfully", "SUCCESS",
                    tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : null
            );
            auditLogRepository.save(auditLog).subscribe();

            // 5. Send SMS notification
            String smsMessage = "Your order " + orderId + " has been processed";
            try {
                smsSender.sendSms(request.getCustomerPhoneNumber(), smsMessage);
                log.info("SMS sent for order {}", orderId);

                // Record SMS sent in domain events
                order.recordSmsSent(new PhoneNumber(request.getCustomerPhoneNumber()), "N/A");

            } catch (Exception e) {
                log.error("Failed to send SMS for order {}: {}", orderId, e.getMessage());
            }

            // 6. Respond to gRPC client
            OrderResponse response = OrderResponse.newBuilder()
                    .setOrderId(orderId)
                    .setStatus(order.getStatus())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Order processing completed for: {}", orderId);

        } catch (Exception e) {
            log.error("Error during SMS/save phase for order {}: {}", orderId, e.getMessage(), e);
            smsSpan.error(e);
            AuditLog auditLog = new AuditLog(
                    orderId, "ORDER_FAILED", "OrderProcessorActor",
                    "Error processing order: " + e.getMessage(), "FAILED",
                    tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : null
            );
            auditLogRepository.save(auditLog).subscribe();
            respondWithError(request, responseObserver);
        } finally {
            smsSpan.end();
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

    /**
     * Message class for order processing requests.
     */
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