package br.com.payments.notification.listener;

import br.com.payments.events.EventEnvelope;
import br.com.payments.events.Topics;
import br.com.payments.events.contracts.NotificationRequested;
import br.com.payments.events.contracts.OrderApproved;
import br.com.payments.events.contracts.OrderFailed;
import br.com.payments.events.support.EventMapper;
import br.com.payments.events.support.EventPublisher;
import br.com.payments.events.support.IdempotencyGuard;
import br.com.payments.notification.service.EmailService;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final ObjectMapper objectMapper;
    private final EventMapper eventMapper;
    private final IdempotencyGuard idempotencyGuard;
    private final EmailService emailService;
    private final EventPublisher eventPublisher;

    @KafkaListener(topics = Topics.ORDER_EVENTS)
    @Transactional
    public void onOrderEvent(String message) {
        EventEnvelope envelope = parse(message);
        if (idempotencyGuard.alreadyProcessed(envelope.eventId(), envelope.type())) {
            return;
        }
        Object event = eventMapper.toDomainEvent(envelope);
        switch (event) {
            case OrderApproved approved -> notify(approved.orderId(), "Seu pedido foi aprovado.");
            case OrderFailed failed -> notify(failed.orderId(), "Seu pedido falhou: " + failed.reason());
            default -> log.debug("Evento ignorado pelo notification-service: {}", envelope.type());
        }
    }

    private void notify(UUID orderId, String message) {
        String email = "cliente-" + orderId + "@example.com";
        emailService.send(orderId, email, message);
        eventPublisher.publish(new NotificationRequested(UUID.randomUUID(), orderId, email, message));
        log.info("Notificacao registrada: orderId={}", orderId);
    }

    private EventEnvelope parse(String message) {
        try {
            return objectMapper.readValue(message, EventEnvelope.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao deserializar envelope recebido", e);
        }
    }
}