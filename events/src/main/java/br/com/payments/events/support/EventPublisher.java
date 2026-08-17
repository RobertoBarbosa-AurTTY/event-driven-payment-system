package br.com.payments.events.support;

import br.com.payments.events.DomainEvent;
import br.com.payments.events.EventEnvelope;
import br.com.payments.events.jpa.Outbox;
import br.com.payments.events.jpa.OutboxRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publica um evento de dominio persistindo-o na tabela outbox (transacao local do servico).
 * O relay (OutboxPublisher) envia para o Kafka em seguida.
 */
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private static final Map<String, String> TOPIC_BY_TYPE = Map.ofEntries(
        Map.entry("order.created", "order.events"),
        Map.entry("order.approved", "order.events"),
        Map.entry("order.failed", "order.events"),
        Map.entry("payment.authorizing", "payment.events"),
        Map.entry("payment.succeeded", "payment.events"),
        Map.entry("payment.failed", "payment.events"),
        Map.entry("fraud.approved", "fraud.events"),
        Map.entry("fraud.rejected", "fraud.events"),
        Map.entry("notification.requested", "notification.events")
    );

    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;

    @Transactional
    public String publish(DomainEvent event) {
        JsonNode payload = objectMapper.valueToTree(event);
        EventEnvelope envelope = new EventEnvelope(
            UUID.randomUUID().toString(),
            event.eventType(),
            payload,
            event.occurredAt()
        );
        String topic = TOPIC_BY_TYPE.getOrDefault(event.eventType(), "order.events");
        outboxRepository.save(new Outbox(
            envelope.eventId(),
            envelope.type(),
            writePayload(envelope),
            topic,
            envelope.occurredAt()
        ));
        return envelope.eventId();
    }

    private String writePayload(EventEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar envelope de evento", e);
        }
    }
}