package com.hacom.orders.infrastructure.grpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.hacom.orders.domain.port.OrderRepository;
import com.hacom.orders.domain.port.SmsSender;
import com.hacom.orders.grpc.OrderRequest;
import com.hacom.orders.grpc.OrderResponse;
import com.hacom.orders.grpc.OrderServiceGrpc;
import com.hacom.orders.infrastructure.actor.OrderProcessorActor;
import com.hacom.orders.infrastructure.audit.AuditLogRepository;
import io.grpc.stub.StreamObserver;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class GrpcOrderService extends OrderServiceGrpc.OrderServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcOrderService.class);

    private final ActorSystem actorSystem;
    private final Tracer tracer;
    private final OrderRepository orderRepository;
    private final SmsSender smsSender;
    private final AuditLogRepository auditLogRepository;

    public GrpcOrderService(ActorSystem actorSystem, Tracer tracer,
                            OrderRepository orderRepository, SmsSender smsSender,
                            AuditLogRepository auditLogRepository) {
        this.actorSystem = actorSystem;
        this.tracer = tracer;
        this.orderRepository = orderRepository;
        this.smsSender = smsSender;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void createOrder(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        String orderId = request.getOrderId();

        Span grpcSpan = tracer.nextSpan().name("grpc.createOrder")
                .tag("order.id", orderId)
                .tag("customer.id", request.getCustomerId())
                .tag("rpc.method", "CreateOrder")
                .tag("rpc.service", "OrderService")
                .start();

        try (var scope = tracer.withSpan(grpcSpan)) {
            log.info("gRPC request received - OrderId: {}, CustomerId: {}, PhoneNumber: {}",
                    orderId, request.getCustomerId(), request.getCustomerPhoneNumber());

            ActorRef orderProcessor = actorSystem.actorOf(
                    Props.create(OrderProcessorActor.class, orderRepository, smsSender, auditLogRepository, tracer),
                    "order-processor-" + orderId + "-" + System.currentTimeMillis()
            );

            orderProcessor.tell(
                    new OrderProcessorActor.ProcessOrderMessage(request, responseObserver),
                    ActorRef.noSender()
            );

            log.debug("Order {} sent to actor for processing (traceId: {})",
                    orderId, grpcSpan.context().traceId());

        } catch (Exception e) {
            log.error("Error processing gRPC request for order {}: {}", orderId, e.getMessage(), e);
            grpcSpan.error(e);
            OrderResponse response = OrderResponse.newBuilder()
                    .setOrderId(orderId)
                    .setStatus("FAILED")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } finally {
            grpcSpan.end();
        }
    }
}