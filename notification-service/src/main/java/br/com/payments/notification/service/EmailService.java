package br.com.payments.notification.service;

import br.com.payments.notification.domain.Notification;
import br.com.payments.notification.domain.NotificationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simula o envio de e-mail (apenas log).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void send(UUID orderId, String email, String message) {
        Notification notification = new Notification(UUID.randomUUID(), orderId, email, message);
        notificationRepository.save(notification);
        log.info("[EMAIL SIMULADO] Para={} Assunto=Atualizacao do pedido {} | {}", email, orderId, message);
    }
}