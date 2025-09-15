package co.com.pragma.solicitud.model.outbox.gateways;

import co.com.pragma.solicitud.model.outbox.OutboxEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OutboxRepository {
    Mono<Void> save(OutboxEvent event);
    Flux<OutboxEvent> fetchPending(int limit, int maxRetries);
    Mono<Long> markProcessed(UUID id);
    Mono<Long> markFailed(UUID id, String error);
}
