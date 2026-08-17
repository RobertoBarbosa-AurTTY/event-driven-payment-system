package br.com.payments.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.payments.events.contracts.OrderCreated;
import br.com.payments.events.support.EventPublisher;
import br.com.payments.order.domain.Order;
import br.com.payments.order.domain.OrderRepository;
import br.com.payments.order.web.CreateOrderRequest;
import br.com.payments.order.web.OrderResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createPersistsOrderAndPublishesOrderCreated() {
        CreateOrderRequest request = new CreateOrderRequest(
            UUID.randomUUID(),
            new BigDecimal("199.90"),
            "BRL"
        );

        OrderResponse response = orderService.create(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(Order.Status.PENDING);
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publish(any(OrderCreated.class));
    }

    @Test
    void findByIdThrowsWhenOrderDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
            OrderNotFoundException.class, () -> orderService.findById(id));
    }
}