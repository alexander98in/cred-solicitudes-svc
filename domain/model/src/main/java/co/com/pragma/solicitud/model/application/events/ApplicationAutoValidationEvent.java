package co.com.pragma.solicitud.model.application.events;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record ApplicationAutoValidationEvent(
        UUID idApplication,
        BigDecimal amount,
        Integer term,
        String email,

        UUID idLoanType,
        String loanTypeName,
        BigDecimal interestRate,
        BigDecimal maxAmount,
        BigDecimal minAmount,

        UUID idUser,
        String emailUser,
        String fullNameUser,
        BigDecimal salaryUser,

        List<ApprovedApplicationSummary> approvedApplicationSummaries,
        OffsetDateTime occurredAt

) {
}
