package br.com.payments.order.service;

import br.com.payments.events.contracts.OrderCreated;
import br.com.payments.events.support.EventPublisher;
import br.com.payments.order.domain.Order;
import br.com.payments.order.domain.OrderRepository;
import br.com.payments.order.web.CreateOrderRequest;
import br.com.payments.order.web.OrderResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Order order = new Order(
            UUID.randomUUID(),
            request.customerId(),
            request.amount(),
            request.currency().toUpperCase()
        );
        orderRepository.save(order);

        eventPublisher.publish(new OrderCreated(
            order.getId(),
            order.getCustomerId(),
            order.getAmount(),
            order.getCurrency()
        ));

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        return orderRepository.findById(id).map(OrderResponse::from)
            .orElseThrow(() -> new OrderNotFoundException(id));
    }
}