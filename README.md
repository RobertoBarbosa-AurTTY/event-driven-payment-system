# event-driven-payment-system

Processamento de pagamentos **event-driven** usando mensageria assíncrona (Apache Kafka).
Implementação em Java 21 / Spring Boot 4 com **Outbox pattern** e **Saga coreografia**.

## Arquitetura

```
POST /api/v1/orders
        │
        ▼
┌─────────────────┐   order.created    ┌─────────────────┐
│  order-service  │ ─────────────────► │  payment-service │
│  (:8081)        │  order.events      │  (:8082)         │
└─────────────────┘                    └─────────────────┘
        ▲                                    │
        │ payment.succeeded / payment.failed │ payment.authorizing
        │ payment.events                     ▼ payment.events
        │                              ┌─────────────────┐
        │                              │  fraud-service   │
        │                              │  (:8083)         │
        │                              └─────────────────┘
        │                                    │
        │              fraud.approved / fraud.rejected
        │              fraud.events
        ▼
┌─────────────────┐   order.approved / order.failed   ┌─────────────────────┐
│ notification-    │ ────────────────────────────────► │ notification-service │
│ service (consome)│       order.events                │ (:8084)             │
└─────────────────┘                                    └─────────────────────┘
```

### Fluxo da saga (coreografia)

1. `order-service` cria o pedido (`PENDING`) e publica `order.created`.
2. `payment-service` consome e cria o pagamento (`AUTHORIZING`), publicando `payment.authorizing`.
3. `fraud-service` analisa o valor e publica `fraud.approved` ou `fraud.rejected`.
4. `payment-service` conclui o pagamento (`SUCCEEDED`/`FAILED`) e publica `payment.succeeded` ou `payment.failed`.
5. `order-service` finaliza o pedido (`APPROVED`/`FAILED`) e publica `order.approved` ou `order.failed`.
6. `notification-service` envia o e-mail (simulado) e registra `notification.requested`.

### Confiabilidade

- **Outbox pattern**: eventos de domínio são persistidos na tabela `outbox` dentro da mesma transação do estado de negócio; um relay (`@Scheduled`) publica no Kafka. Garante entrega sem transação distribuída.
- **Idempotência**: consumidores registram o `eventId` em `processed_event` antes de processar; eventos repetidos são ignorados.
- **Dead Letter Topic**: mensagens que falham após a política de retry vão para `<topic>.dlt` (via `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` do Spring Boot).
- **Envelope padrão**: `EventEnvelope { eventId, type, payload, occurredAt }` compartilhado no módulo `events`.

## Pré-requisitos

- Docker + Docker Compose (build e runtime 100% em container — não precisa de JDK/Maven local)

## Como rodar

```bash
docker compose up --build
```

Isso sobe: Postgres 16, Kafka (KRaft), Kafka UI e os 4 serviços. Aguarde os healthchecks (os serviços esperam o Kafka e o Postgres ficarem saudáveis).

## Endpoints

| Serviço             | Porta | Swagger UI                        |
|---------------------|-------|-----------------------------------|
| order-service       | 8081  | http://localhost:8081/swagger-ui.html |
| payment-service     | 8082  | http://localhost:8082/swagger-ui.html |
| fraud-service       | 8083  | http://localhost:8083/swagger-ui.html |
| notification-service| 8084  | http://localhost:8084/swagger-ui.html |
| Kafka UI            | 8090  | http://localhost:8090              |

### Criar um pedido

```bash
curl -X POST http://localhost:8081/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"7a7f9c40-8e6b-4f3e-9b5a-1f2c3d4e5f60","amount":199.90,"currency":"BRL"}'
```

Resposta (estado inicial `PENDING`):

```json
{ "id": "...", "customerId": "...", "amount": 199.90, "currency": "BRL", "status": "PENDING", "createdAt": "..." }
```

### Consultar o status do pedido

```bash
curl http://localhost:8081/api/v1/orders/<order-id>
```

> Regra de fraude (exemplo): valores acima de `fraud.max-amount` (default `10000.00`) são rejeitados.

## Módulos

| Módulo              | Responsabilidade                                          |
|---------------------|-----------------------------------------------------------|
| `events`            | Contratos de eventos, envelope, outbox, idempotência, Kafka |
| `order-service`     | Pedidos (origem do fluxo)                                  |
| `payment-service`   | Processamento de pagamentos (saga)                         |
| `fraud-service`     | Análise anti-fraude                                        |
| `notification-service` | Notificações (e-mail simulado)                          |

## Tópicos Kafka

| Tópico               | Eventos                                                        |
|----------------------|----------------------------------------------------------------|
| `order.events`       | `order.created`, `order.approved`, `order.failed`               |
| `payment.events`     | `payment.authorizing`, `payment.succeeded`, `payment.failed`    |
| `fraud.events`       | `fraud.approved`, `fraud.rejected`                              |
| `notification.events`| `notification.requested`                                        |

## Notas

- Schema das tabelas criado via `ddl-auto: update` (sem Flyway nesta versão).
- Banco: um container Postgres com 4 bancos (`order_db`, `payment_db`, `fraud_db`, `notification_db`) criados por `docker/postgres-init.sql`.
- Build multi-stage: `maven:3.9-eclipse-temurin-21` (build) → `eclipse-temurin:21-jre` (runtime).
