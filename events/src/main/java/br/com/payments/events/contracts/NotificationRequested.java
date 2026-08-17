package br.com.payments.events.contracts;

import br.com.payments.events.DomainEvent;
import java.util.UUID;

public record NotificationRequested(UUID notificationId, UUID orderId, String email, String message)
    implements DomainEvent {

    public static final String TYPE = "notification.requested";

    @Override
    public String eventType() {
        return TYPE;
    }

    @Override
    public UUID aggregateId() {
        return notificationId;
    }
}