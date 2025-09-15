package co.com.pragma.solicitud.model.outbox;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

@Value
@Builder
public class OutboxEvent {

    UUID id;
    UUID aggregateId;
    String eventType;
    Object payload;
    OffsetDateTime occurredAt;
    boolean processed;
    int retries;
}
