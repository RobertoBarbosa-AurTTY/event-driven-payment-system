package br.com.payments.events.support;

import br.com.payments.events.jpa.Outbox;
import br.com.payments.events.jpa.OutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relay do outbox: periodicamente publica os eventos nao enviados para o Kafka.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${outbox.publish-interval-ms:2000}")
    private long publishIntervalMs;

    @Scheduled(fixedDelayString = "${outbox.publish-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        List<Outbox> pending = outboxRepository.findByPublishedFalseOrderByOccurredAtAsc();
        for (Outbox outbox : pending) {
            try {
                kafkaTemplate.send(outbox.getTopic(), outbox.getEventId(), outbox.getPayload()).get();
                outbox.markPublished();
                log.info("Evento publicado no Kafka: type={} eventId={}", outbox.getType(), outbox.getEventId());
            } catch (Exception e) {
                log.error("Falha ao publicar evento no Kafka: type={} eventId={}", outbox.getType(),
                    outbox.getEventId(), e);
                // Nao marca como publicado; sera retentado no proximo ciclo.
                return;
            }
        }
    }
}