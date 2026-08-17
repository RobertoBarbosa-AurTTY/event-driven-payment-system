package br.com.payments.events;

import tools.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * Envelope enviado ao Kafka. O payload carrega a representacao JSON do evento de dominio.
 *
 * @param eventId   id do evento (usado para idempotencia do consumidor)
 * @param type      tipo do evento (ex.: "order.created")
 * @param payload   payload do evento
 * @param occurredAt momento em que o evento ocorreu
 */
public record EventEnvelope(String eventId, String type, JsonNode payload, Instant occurredAt) {
}