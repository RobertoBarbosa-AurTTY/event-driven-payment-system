package br.com.payments.order.web;

import br.com.payments.order.domain.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Representacao de um pedido")
public record OrderResponse(
    UUID id,
    UUID customerId,
    BigDecimal amount,
    String currency,
    Order.Status status,
    Instant createdAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getAmount(),
            order.getCurrency(),
            order.getStatus(),
            order.getCreatedAt()
        );
    }
}