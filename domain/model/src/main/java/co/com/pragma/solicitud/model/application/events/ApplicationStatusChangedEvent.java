package co.com.pragma.solicitud.model.application.events;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record ApplicationStatusChangedEvent(
        UUID idApplication,
        String newStatus,
        String email,
        BigDecimal amount,
        BigDecimal term,
        OffsetDateTime occurredAt
) {
}
