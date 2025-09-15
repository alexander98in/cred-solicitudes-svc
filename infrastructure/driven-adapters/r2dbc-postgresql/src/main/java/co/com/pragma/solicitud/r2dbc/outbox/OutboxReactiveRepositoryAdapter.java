package co.com.pragma.solicitud.r2dbc.outbox;

import co.com.pragma.solicitud.model.outbox.OutboxEvent;
import co.com.pragma.solicitud.model.outbox.gateways.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxReactiveRepositoryAdapter implements OutboxRepository {

    private final DatabaseClient client;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> save(OutboxEvent e) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(e.getPayload()))
                .flatMap(json -> client.sql("""
                    INSERT INTO outbox (aggregate_id, event_type, payload, occurred_at, processed, retries)
                    VALUES ($1, $2, $3, $4, false, 0)
                """)
                        .bind(0, e.getAggregateId())
                        .bind(1, e.getEventType())
                        .bind(2, Json.of(json))
                        .bind(3, e.getOccurredAt() != null ? e.getOccurredAt() : OffsetDateTime.now())
                        .fetch()
                        .rowsUpdated()
                        .then()
                );
    }

    @Override
    public Flux<OutboxEvent> fetchPending(int limit, int maxRetries) {
        String sql = """
            SELECT id, aggregate_id, event_type, payload, occurred_at, processed, retries
            FROM outbox
            WHERE processed = false
                AND retries <= :maxRetries
            ORDER BY occurred_at ASC
                LIMIT :limit
        """;
        return client.sql(sql)
                .bind("limit", limit)
                .bind("maxRetries", maxRetries)
                .map((row, meta) -> {
                    Object rawPayload = row.get("payload");
                    String payloadStr;
                    if (rawPayload instanceof Json j) {
                        payloadStr = j.asString();
                    } else if (rawPayload != null) {
                        payloadStr = rawPayload.toString();
                    } else {
                        payloadStr = null;
                    }

                    return OutboxEvent.builder()
                            .id(row.get("id", UUID.class))
                            .aggregateId(row.get("aggregate_id", UUID.class))
                            .eventType(row.get("event_type", String.class))
                            .payload(payloadStr)
                            .occurredAt(row.get("occurred_at", OffsetDateTime.class))
                            .processed(Boolean.TRUE.equals(row.get("processed", Boolean.class)))
                            .retries(row.get("retries", Integer.class))
                            .build();
                })
                .all();
    }

    @Override
    public Mono<Long> markProcessed(UUID id) {
        String sql = "UPDATE outbox SET processed = true WHERE id = $1 AND processed = false";
        return client.sql(sql)
                .bind(0, id)
                .fetch()
                .rowsUpdated();
    }

    @Override
    public Mono<Long> markFailed(UUID id, String error) {
        String sql = """
            UPDATE outbox
               SET retries = retries + 1
             WHERE id = $1
        """;
        return client.sql(sql)
                .bind(0, id)
                .fetch()
                .rowsUpdated();
    }
}
