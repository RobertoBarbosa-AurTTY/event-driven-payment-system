package br.com.payments.events.contracts;

import br.com.payments.events.DomainEvent;
import java.util.UUID;

public record OrderApproved(UUID orderId) implements DomainEvent {

    public static final String TYPE = "order.approved";

    @Override
    public String eventType() {
        return TYPE;
    }

    @Override
    public UUID aggregateId() {
        return orderId;
    }
}