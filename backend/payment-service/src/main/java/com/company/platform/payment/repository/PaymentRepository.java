package com.company.platform.payment.repository;

import com.company.platform.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Payment> findByUserIdAndPaymentId(UUID userId, UUID paymentId);

    Optional<Payment> findByPaymentId(UUID paymentId);
}
