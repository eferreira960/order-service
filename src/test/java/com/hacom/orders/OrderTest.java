package com.hacom.orders;

import com.hacom.orders.domain.model.Order;
import com.hacom.orders.domain.model.vo.OrderId;
import com.hacom.orders.domain.model.vo.CustomerId;
import com.hacom.orders.domain.model.vo.PhoneNumber;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {

    @Test
    void testOrderCreation() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Order order = Order.create(
                new OrderId("ORD-001"),
                new CustomerId("CUST-001"),
                new PhoneNumber("+584141234567"),
                List.of("Item1", "Item2")
        );

        assertNotNull(order.get_id());
        assertEquals("ORD-001", order.getOrderId());
        assertEquals("CUST-001", order.getCustomerId());
        assertEquals("+584141234567", order.getCustomerPhoneNumber());
        assertEquals(2, order.getItems().size());
        assertNotNull(order.getTs());
    }

    @Test
    void testOrderProcessing() {
        Order order = Order.create(
                new OrderId("ORD-001"),
                new CustomerId("CUST-001"),
                new PhoneNumber("+584141234567"),
                List.of("Item1", "Item2")
        );

        order.process();
        assertEquals("PROCESSED", order.getStatus());
    }

    @Test
    void testOrderSetters() {
        Order order = Order.create(
                new OrderId("ORD-002"),
                new CustomerId("CUST-002"),
                new PhoneNumber("+584147654321"),
                List.of("ItemA")
        );

        assertEquals("ORD-002", order.getOrderId());
        assertEquals("CUST-002", order.getCustomerId());
        assertEquals("+584147654321", order.getCustomerPhoneNumber());
        assertNotNull(order.getStatus());
        assertEquals(1, order.getItems().size());
    }

    @Test
    void testOrderToString() {
        Order order = Order.create(
                new OrderId("ORD-001"),
                new CustomerId("CUST-001"),
                new PhoneNumber("+584141234567"),
                List.of("Item1")
        );
        String str = order.toString();
        assertTrue(str.contains("ORD-001"));
        assertTrue(str.contains("CUST-001"));
    }
}