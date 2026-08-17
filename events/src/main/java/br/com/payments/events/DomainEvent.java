package br.com.payments.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato base de todos os eventos de dominio.
 */
public interface DomainEvent {

    String eventType();

    UUID aggregateId();

    default Instant occurredAt() {
        return Instant.now();
    }
}