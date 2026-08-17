package br.com.payments.events.contracts;

import br.com.payments.events.DomainEvent;
import java.util.UUID;

public record OrderFailed(UUID orderId, String reason) implements DomainEvent {

    public static final String TYPE = "order.failed";

    @Override
    public String eventType() {
        return TYPE;
    }

    @Override
    public UUID aggregateId() {
        return orderId;
    }
}