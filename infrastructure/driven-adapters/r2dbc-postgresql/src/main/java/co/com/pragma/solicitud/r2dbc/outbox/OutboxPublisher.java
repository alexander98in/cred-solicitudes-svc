package co.com.pragma.solicitud.r2dbc.outbox;

import co.com.pragma.solicitud.model.application.events.ApplicationAutoValidationEvent;
import co.com.pragma.solicitud.model.application.events.ApplicationStatusChangedEvent;
import co.com.pragma.solicitud.model.application.gateways.NotificationQueue;
import co.com.pragma.solicitud.model.application.gateways.ValidationQueue;
import co.com.pragma.solicitud.model.outbox.OutboxEvent;
import co.com.pragma.solicitud.model.outbox.gateways.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final ValidationQueue validationQueue;
    private final NotificationQueue notificationQueue;
    private final ObjectMapper objectMapper;

    @Value("${outbox.poll-interval-ms:2000}")
    private long pollMs;
    @Value("${outbox.batch-size:50}")
    private int batch;
    @Value("${outbox.max-retries:10}")
    private int maxRetries;
    @Value("${outbox.parallelism:5}")
    private int parallelism;

    private Disposable subscription;

    @PostConstruct
    public void start() {
        log.info("Iniciando OutboxPublisher: intervalMs={}, batch={}, maxRetries={}, parallelism={}",
                pollMs, batch, maxRetries, parallelism);
        subscription = Flux.interval(Duration.ofMillis(pollMs))
                .flatMap(t -> outboxRepository.fetchPending(batch, maxRetries), 1)
                .flatMap(this::processOne, parallelism)
                .onErrorContinue((e, o) -> log.error("Error en ciclo Outbox (continuando): {}", e.toString(), e))
                .subscribe();

        log.info("OutboxPublisher suscrito y corriendo.");
    }

    @PreDestroy
    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("OutboxPublisher detenido correctamente.");
        }
    }

    /**
     * Procesa un evento de la outbox: deserializa, publica en el destino correspondiente y marca como procesado.
     * Si el tipo no es soportado o hay error de parsing, marca como failed con reintento.
     */
    private Flux<Void> processOne(OutboxEvent e) {
        log.info("Outbox recibido: id={}, type={}, retries={}, processed={}",
                e.getId(), e.getEventType(), e.getRetries(), e.isProcessed());

        return switch (e.getEventType()) {

            case "ApplicationStatusChangedEvent" -> toEvent(e, ApplicationStatusChangedEvent.class)
                    .doOnNext(evt -> log.debug("Payload deserializado (id={}): {}", e.getId(), evt))
                    .flatMap(notificationQueue::publishStatusChangedEvent)
                    .then(Mono.fromRunnable(() ->
                            log.info("Publicado en SQS (ApplicationStatusChangedEvent): id={}", e.getId())
                    ))
                    .onErrorResume(ex -> {
                        log.error("Fallo publicando en SQS (ApplicationStatusChangedEvent). id={}, error={}",
                                e.getId(), ex.getMessage(), ex);
                        return outboxRepository.markFailed(e.getId(), ex.getMessage()).then(Mono.empty());
                    })
                    .thenMany(markOk(e));

            case "ApplicationAutoValidationEvent" -> toEvent(e, ApplicationAutoValidationEvent.class)
                    .doOnNext(evt -> log.debug("Payload deserializado (id={}): {}", e.getId(), evt))
                    .flatMap(validationQueue::publishAutoValidationApplicationEvent)
                    .then(Mono.fromRunnable(() ->
                            log.info("Publicado en SQS (ApplicationAutoValidationEvent): id={}", e.getId())
                    ))
                    .onErrorResume( ex -> {
                        log.error("Fallo publicando en SQS (ApplicationAutoValidationEvent). id={}, error={}",
                                e.getId(), ex.getMessage(), ex);
                        return outboxRepository.markFailed(e.getId(), ex.getMessage()).then(Mono.empty());
                    })
                    .thenMany(markOk(e));

            default -> {
                log.warn("Tipo de evento no soportado. id={}, type={}", e.getId(), e.getEventType());
                yield outboxRepository.markFailed(e.getId(), "Unsupported type: " + e.getEventType())
                        .doOnNext(rows -> log.warn("Outbox marcado como failed por tipo no soportado. id={}, rows={}", e.getId(), rows))
                        .thenMany(Flux.empty());
            }
        };
    }

    /**
     * Deserializa el payload a un tipo concreto. Si falla, marca failed y continúa.
     */
    private <T> Flux<T> toEvent(OutboxEvent e, Class<T> type) {
        try {
            String json = (String) e.getPayload();
            T obj = objectMapper.readValue(json, type);
            return Flux.just(obj);
        } catch (Exception ex) {
            log.error("Error parseando payload. id={}, type={}, error={}",
                    e.getId(), e.getEventType(), ex.getMessage(), ex);
            return outboxRepository.markFailed(e.getId(), ex.getMessage())
                    .doOnNext(rows -> log.warn("Outbox marcado failed por error de parseo. id={}, rows={}", e.getId(), rows))
                    .thenMany(Flux.empty());
        }
    }

    /**
     * Marca el evento como procesado y deja un log claro y consistente.
     */
    private Flux<Void> markOk(OutboxEvent e) {
        return outboxRepository.markProcessed(e.getId())
                .doOnNext(rows -> {
                    if (rows > 0) {
                        log.info("Outbox procesado OK. id={}, type={}, rows={}", e.getId(), e.getEventType(), rows);
                    } else {
                        log.warn("Outbox ya estaba procesado (sin cambios). id={}, type={}", e.getId(), e.getEventType());
                    }
                })
                .thenMany(Flux.empty());
    }
}
