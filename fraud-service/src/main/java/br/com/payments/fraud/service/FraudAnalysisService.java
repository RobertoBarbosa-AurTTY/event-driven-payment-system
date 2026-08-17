package br.com.payments.fraud.service;

import br.com.payments.fraud.domain.FraudCheck;
import br.com.payments.fraud.domain.FraudCheckRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FraudAnalysisService {

    private final FraudCheckRepository fraudCheckRepository;

    @Value("${fraud.max-amount:10000.00}")
    private BigDecimal maxAmount;

    /**
     * Regra simplificada de exemplo: pedidos acima do limite sao rejeitados.
     */
    @Transactional
    public FraudCheck analyze(UUID orderId, BigDecimal amount) {
        FraudCheck.Result result =
            amount.compareTo(maxAmount) > 0 ? FraudCheck.Result.REJECTED : FraudCheck.Result.APPROVED;
        FraudCheck check = new FraudCheck(UUID.randomUUID(), orderId, result);
        return fraudCheckRepository.save(check);
    }
}