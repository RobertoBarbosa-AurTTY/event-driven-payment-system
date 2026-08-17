package br.com.payments.events.contracts;

import br.com.payments.events.DomainEvent;
import java.util.UUID;

public record FraudApproved(UUID fraudCheckId, UUID orderId) implements DomainEvent {

    public static final String TYPE = "fraud.approved";

    @Override
    public String eventType() {
        return TYPE;
    }

    @Override
    public UUID aggregateId() {
        return fraudCheckId;
    }
}