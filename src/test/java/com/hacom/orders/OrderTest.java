package com.hacom.orders;

import com.hacom.orders.domain.model.Order;
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
        Order order = new Order(
                "ORD-001",
                "CUST-001",
                "+584141234567",
                "PROCESSED",
                List.of("Item1", "Item2"),
                now
        );

        assertNotNull(order.get_id());
        assertEquals("ORD-001", order.getOrderId());
        assertEquals("CUST-001", order.getCustomerId());
        assertEquals("+584141234567", order.getCustomerPhoneNumber());
        assertEquals("PROCESSED", order.getStatus());
        assertEquals(2, order.getItems().size());
        assertEquals(now, order.getTs());
    }

    @Test
    void testOrderDefaultConstructor() {
        Order order = new Order();
        assertNull(order.get_id());
        assertNull(order.getOrderId());
        assertNull(order.getCustomerId());
    }

    @Test
    void testOrderSetters() {
        Order order = new Order();
        ObjectId id = new ObjectId();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        order.set_id(id);
        order.setOrderId("ORD-002");
        order.setCustomerId("CUST-002");
        order.setCustomerPhoneNumber("+584147654321");
        order.setStatus("PENDING");
        order.setItems(List.of("ItemA"));
        order.setTs(now);

        assertEquals(id, order.get_id());
        assertEquals("ORD-002", order.getOrderId());
        assertEquals("CUST-002", order.getCustomerId());
        assertEquals("+584147654321", order.getCustomerPhoneNumber());
        assertEquals("PENDING", order.getStatus());
        assertEquals(1, order.getItems().size());
        assertEquals(now, order.getTs());
    }

    @Test
    void testOrderToString() {
        Order order = new Order(
                "ORD-001",
                "CUST-001",
                "+584141234567",
                "PROCESSED",
                List.of("Item1"),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        String str = order.toString();
        assertTrue(str.contains("ORD-001"));
        assertTrue(str.contains("CUST-001"));
        assertTrue(str.contains("PROCESSED"));
    }
}