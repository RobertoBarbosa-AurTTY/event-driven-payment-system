package br.com.payments.fraud.listener;

import br.com.payments.events.EventEnvelope;
import br.com.payments.events.Topics;
import br.com.payments.events.contracts.FraudApproved;
import br.com.payments.events.contracts.FraudRejected;
import br.com.payments.events.contracts.PaymentAuthorizing;
import br.com.payments.events.support.EventMapper;
import br.com.payments.events.support.EventPublisher;
import br.com.payments.events.support.IdempotencyGuard;
import br.com.payments.fraud.domain.FraudCheck;
import br.com.payments.fraud.service.FraudAnalysisService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class FraudEventListener {

    private final ObjectMapper objectMapper;
    private final EventMapper eventMapper;
    private final IdempotencyGuard idempotencyGuard;
    private final FraudAnalysisService fraudAnalysisService;
    private final EventPublisher eventPublisher;

    @KafkaListener(topics = Topics.PAYMENT_EVENTS)
    @Transactional
    public void onPaymentEvent(String message) {
        EventEnvelope envelope = parse(message);
        if (idempotencyGuard.alreadyProcessed(envelope.eventId(), envelope.type())) {
            return;
        }
        if (envelope.type().equals(PaymentAuthorizing.TYPE)) {
            PaymentAuthorizing event = (PaymentAuthorizing) eventMapper.toDomainEvent(envelope);
            analyze(event);
        }
    }

    private void analyze(PaymentAuthorizing event) {
        FraudCheck check = fraudAnalysisService.analyze(event.orderId(), event.amount());
        switch (check.getResult()) {
            case APPROVED -> {
                eventPublisher.publish(new FraudApproved(check.getId(), check.getOrderId()));
                log.info("Fraude aprovada: fraudCheckId={} orderId={}", check.getId(), check.getOrderId());
            }
            case REJECTED -> {
                eventPublisher.publish(new FraudRejected(check.getId(), check.getOrderId(), "valor acima do limite"));
                log.info("Fraude rejeitada: fraudCheckId={} orderId={}", check.getId(), check.getOrderId());
            }
        }
    }

    private EventEnvelope parse(String message) {
        try {
            return objectMapper.readValue(message, EventEnvelope.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao deserializar envelope recebido", e);
        }
    }
}