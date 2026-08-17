package br.com.payments.events.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Registro de eventos ja processados pelo consumidor (idempotencia).
 */
@Entity
@Table(name = "processed_event")
@Getter
@NoArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private String eventId;

    @Column(nullable = false, updatable = false)
    private String type;

    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    public ProcessedEvent(String eventId, String type) {
        this.eventId = eventId;
        this.type = type;
        this.processedAt = Instant.now();
    }
}