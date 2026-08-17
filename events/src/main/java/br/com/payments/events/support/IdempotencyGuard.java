package br.com.payments.events.support;

import br.com.payments.events.jpa.ProcessedEvent;
import br.com.payments.events.jpa.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Guarda de idempotencia: registra o eventId assim que o processamento comeca e
 * detecta eventos ja processados (sem precisar de transacao, evita corridas).
 */
@Component
@RequiredArgsConstructor
public class IdempotencyGuard {

    private final ProcessedEventRepository processedEventRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * @return true se o evento ja foi processado, false caso contrario (e registra como processado).
     */
    public boolean alreadyProcessed(String eventId, String type) {
        if (processedEventRepository.existsById(eventId)) {
            return true;
        }
        try {
            transactionTemplate.executeWithoutResult(status ->
                processedEventRepository.save(new ProcessedEvent(eventId, type)));
            return false;
        } catch (DataIntegrityViolationException e) {
            return true;
        }
    }
}