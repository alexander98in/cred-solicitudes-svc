package co.com.pragma.solicitud.model.application.events;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class ApplicationStatusChangedEvent {

    UUID idApplication;
    String newStatus;
    String email;
    BigDecimal amount;
    BigDecimal term;
    OffsetDateTime occurredAt;
}
