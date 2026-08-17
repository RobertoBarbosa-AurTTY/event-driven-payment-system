package br.com.payments.payment.listener;

import br.com.payments.events.EventEnvelope;
import br.com.payments.events.Topics;
import br.com.payments.events.contracts.FraudApproved;
import br.com.payments.events.contracts.FraudRejected;
import br.com.payments.events.contracts.OrderCreated;
import br.com.payments.events.contracts.PaymentAuthorizing;
import br.com.payments.events.contracts.PaymentFailed;
import br.com.payments.events.contracts.PaymentSucceeded;
import br.com.payments.events.support.EventMapper;
import br.com.payments.events.support.EventPublisher;
import br.com.payments.events.support.IdempotencyGuard;
import br.com.payments.payment.domain.Payment;
import br.com.payments.payment.domain.PaymentRepository;
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
public class PaymentEventListener {

    private final ObjectMapper objectMapper;
    private final EventMapper eventMapper;
    private final IdempotencyGuard idempotencyGuard;
    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;

    @KafkaListener(topics = Topics.ORDER_EVENTS)
    @Transactional
    public void onOrderEvent(String message) {
        EventEnvelope envelope = parse(message);
        if (idempotencyGuard.alreadyProcessed(envelope.eventId(), envelope.type())) {
            return;
        }
        if (envelope.type().equals(OrderCreated.TYPE)) {
            OrderCreated event = (OrderCreated) eventMapper.toDomainEvent(envelope);
            startPayment(event);
        }
    }

    @KafkaListener(topics = Topics.FRAUD_EVENTS)
    @Transactional
    public void onFraudEvent(String message) {
        EventEnvelope envelope = parse(message);
        if (idempotencyGuard.alreadyProcessed(envelope.eventId(), envelope.type())) {
            return;
        }
        Object event = eventMapper.toDomainEvent(envelope);
        switch (event) {
            case FraudApproved approved -> completePayment(approved.orderId(), null);
            case FraudRejected rejected -> completePayment(rejected.orderId(), rejected.reason());
            default -> log.debug("Evento ignorado pelo payment-service: {}", envelope.type());
        }
    }

    private void startPayment(OrderCreated event) {
        Payment payment = new Payment(
            UUID.randomUUID(),
            event.orderId(),
            event.amount()
        );
        paymentRepository.save(payment);

        eventPublisher.publish(new PaymentAuthorizing(
            payment.getId(),
            payment.getOrderId(),
            payment.getAmount()
        ));
        log.info("Pagamento iniciado: paymentId={} orderId={}", payment.getId(), payment.getOrderId());
    }

    private void completePayment(UUID orderId, String failureReason) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (failureReason == null) {
                payment.succeed();
                paymentRepository.save(payment);
                eventPublisher.publish(new PaymentSucceeded(
                    payment.getId(), payment.getOrderId(), payment.getAmount()
                ));
                log.info("Pagamento aprovado: paymentId={} orderId={}", payment.getId(), orderId);
            } else {
                payment.fail();
                paymentRepository.save(payment);
                eventPublisher.publish(new PaymentFailed(
                    payment.getId(), payment.getOrderId(), payment.getAmount(), failureReason
                ));
                log.info("Pagamento falhou: paymentId={} orderId={} reason={}", payment.getId(), orderId,
                    failureReason);
            }
        });
    }

    private EventEnvelope parse(String message) {
        try {
            return objectMapper.readValue(message, EventEnvelope.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao deserializar envelope recebido", e);
        }
    }
}