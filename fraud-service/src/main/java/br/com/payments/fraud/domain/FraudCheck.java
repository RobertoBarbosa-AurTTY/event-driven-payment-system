package br.com.payments.fraud.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fraud_checks")
@Getter
@NoArgsConstructor
public class FraudCheck {

    public enum Result {
        APPROVED, REJECTED
    }

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID orderId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Result result;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public FraudCheck(UUID id, UUID orderId, Result result) {
        this.id = id;
        this.orderId = orderId;
        this.result = result;
        this.createdAt = Instant.now();
    }
}