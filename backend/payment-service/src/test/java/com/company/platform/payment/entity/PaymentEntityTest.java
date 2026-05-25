package com.company.platform.payment.entity;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

class PaymentEntityTest {

    @Test
    void onCreateCreatesIdAndTimestamps() {
        Payment payment = new Payment();

        payment.onCreate();

        assertNotNull(payment.getId());
        assertNotNull(payment.getCreatedAt());
        assertEquals(payment.getUpdatedAt(), payment.getCreatedAt());
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        Payment payment = new Payment();
        payment.onCreate();

        payment.onUpdate();

        assertNotNull(payment.getUpdatedAt());
    }
}
