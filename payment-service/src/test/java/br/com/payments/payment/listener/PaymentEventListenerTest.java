package br.com.payments.payment.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.payments.events.EventEnvelope;
import br.com.payments.events.contracts.FraudApproved;
import br.com.payments.events.contracts.PaymentSucceeded;
import br.com.payments.events.support.EventMapper;
import br.com.payments.events.support.EventPublisher;
import br.com.payments.events.support.IdempotencyGuard;
import br.com.payments.payment.domain.Payment;
import br.com.payments.payment.domain.PaymentRepository;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private IdempotencyGuard idempotencyGuard;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PaymentEventListener listener;

    private final ObjectMapper realMapper = new ObjectMapper();

    @Test
    void onFraudApprovedCompletesPaymentWithSucceededStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(paymentId, orderId, new BigDecimal("99.90"));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        FraudApproved domainEvent = new FraudApproved(UUID.randomUUID(), orderId);
        EventEnvelope envelope = new EventEnvelope(
            UUID.randomUUID().toString(), FraudApproved.TYPE,
            realMapper.valueToTree(domainEvent), java.time.Instant.now()
        );
        when(objectMapper.readValue(any(String.class), any(Class.class))).thenReturn(envelope);
        when(eventMapper.toDomainEvent(envelope)).thenReturn(domainEvent);
        when(idempotencyGuard.alreadyProcessed(any(), any())).thenReturn(false);

        listener.onFraudEvent("ignored-content");

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.SUCCEEDED);
        verify(eventPublisher).publish(any(PaymentSucceeded.class));
    }
}