package br.com.payments.events.contracts;

import br.com.payments.events.DomainEvent;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentAuthorizing(UUID paymentId, UUID orderId, BigDecimal amount) implements DomainEvent {

    public static final String TYPE = "payment.authorizing";

    @Override
    public String eventType() {
        return TYPE;
    }

    @Override
    public UUID aggregateId() {
        return paymentId;
    }
}