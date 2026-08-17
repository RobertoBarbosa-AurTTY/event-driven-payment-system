package br.com.payments.order.listener;

import br.com.payments.events.EventEnvelope;
import br.com.payments.events.Topics;
import br.com.payments.events.contracts.OrderApproved;
import br.com.payments.events.contracts.OrderFailed;
import br.com.payments.events.contracts.PaymentFailed;
import br.com.payments.events.contracts.PaymentSucceeded;
import br.com.payments.events.support.EventMapper;
import br.com.payments.events.support.EventPublisher;
import br.com.payments.events.support.IdempotencyGuard;
import br.com.payments.order.domain.Order;
import br.com.payments.order.domain.OrderRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final ObjectMapper objectMapper;
    private final EventMapper eventMapper;
    private final IdempotencyGuard idempotencyGuard;
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    @KafkaListener(topics = Topics.PAYMENT_EVENTS)
    @Transactional
    public void onPaymentEvent(String message) {
        EventEnvelope envelope = parse(message);
        if (idempotencyGuard.alreadyProcessed(envelope.eventId(), envelope.type())) {
            log.info("Evento ja processado: type={} eventId={}", envelope.type(), envelope.eventId());
            return;
        }

        Object event = eventMapper.toDomainEvent(envelope);
        switch (event) {
            case PaymentSucceeded succeeded -> handleSucceeded(succeeded);
            case PaymentFailed failed -> handleFailed(failed);
            default -> log.debug("Evento ignorado pelo order-service: {}", envelope.type());
        }
    }

    private void handleSucceeded(PaymentSucceeded event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.approve();
            orderRepository.save(order);
            eventPublisher.publish(new OrderApproved(order.getId()));
            log.info("Pedido aprovado: orderId={}", order.getId());
        });
    }

    private void handleFailed(PaymentFailed event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.fail(event.reason());
            orderRepository.save(order);
            eventPublisher.publish(new OrderFailed(order.getId(), event.reason()));
            log.info("Pedido falhou: orderId={} reason={}", order.getId(), event.reason());
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