package br.com.payments.events.support;

import br.com.payments.events.EventEnvelope;
import br.com.payments.events.contracts.FraudApproved;
import br.com.payments.events.contracts.FraudRejected;
import br.com.payments.events.contracts.NotificationRequested;
import br.com.payments.events.contracts.OrderApproved;
import br.com.payments.events.contracts.OrderCreated;
import br.com.payments.events.contracts.OrderFailed;
import br.com.payments.events.contracts.PaymentAuthorizing;
import br.com.payments.events.contracts.PaymentFailed;
import br.com.payments.events.contracts.PaymentSucceeded;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Converte um EventEnvelope recebido do Kafka de volta para o record do evento de dominio.
 */
@Component
@RequiredArgsConstructor
public class EventMapper {

    private final ObjectMapper objectMapper;

    public Object toDomainEvent(EventEnvelope envelope) {
        return switch (envelope.type()) {
            case OrderCreated.TYPE -> treeToValue(envelope, OrderCreated.class);
            case OrderApproved.TYPE -> treeToValue(envelope, OrderApproved.class);
            case OrderFailed.TYPE -> treeToValue(envelope, OrderFailed.class);
            case PaymentAuthorizing.TYPE -> treeToValue(envelope, PaymentAuthorizing.class);
            case PaymentSucceeded.TYPE -> treeToValue(envelope, PaymentSucceeded.class);
            case PaymentFailed.TYPE -> treeToValue(envelope, PaymentFailed.class);
            case FraudApproved.TYPE -> treeToValue(envelope, FraudApproved.class);
            case FraudRejected.TYPE -> treeToValue(envelope, FraudRejected.class);
            case NotificationRequested.TYPE -> treeToValue(envelope, NotificationRequested.class);
            default -> throw new IllegalArgumentException("Tipo de evento desconhecido: " + envelope.type());
        };
    }

    private <T> T treeToValue(EventEnvelope envelope, Class<T> type) {
        try {
            return objectMapper.treeToValue(envelope.payload(), type);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Falha ao deserializar evento " + envelope.type() + " (" + envelope.eventId() + ")", e);
        }
    }
}