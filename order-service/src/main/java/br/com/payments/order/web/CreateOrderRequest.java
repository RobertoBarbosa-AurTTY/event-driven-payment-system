package br.com.payments.order.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload para criar um pedido")
public record CreateOrderRequest(
    @NotNull UUID customerId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank String currency
) {
}