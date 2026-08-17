package br.com.payments.events.contracts;

import br.com.payments.events.DomainEvent;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreated(UUID orderId, UUID customerId, BigDecimal amount, String currency)
    implements DomainEvent {

    public static final String TYPE = "order.created";

    @Override
    public String eventType() {
        return TYPE;
    }

    @Override
    public UUID aggregateId() {
        return orderId;
    }
}