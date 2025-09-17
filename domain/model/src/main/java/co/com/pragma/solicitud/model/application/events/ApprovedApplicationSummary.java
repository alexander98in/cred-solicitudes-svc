package co.com.pragma.solicitud.model.application.events;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ApprovedApplicationSummary(
        UUID idApplication,
        BigDecimal amount,
        Integer term,
        BigDecimal interestRate,
        BigDecimal maxAmount,
        BigDecimal minAmount
) {
}
