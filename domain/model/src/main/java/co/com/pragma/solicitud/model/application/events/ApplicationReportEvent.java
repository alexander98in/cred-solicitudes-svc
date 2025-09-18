package co.com.pragma.solicitud.model.application.events;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record ApplicationReportEvent(
        UUID idApplication,
        String newStatus,
        BigDecimal amount,
        OffsetDateTime occurredAt
) {
}
