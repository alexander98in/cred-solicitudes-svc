package co.com.pragma.solicitud.api.sqs;

import co.com.pragma.solicitud.usecase.application.ApplicationUseCase;
import co.com.pragma.solicitud.usecase.utils.ApplicationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsInboundPoller {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;
    private final ApplicationUseCase applicationUseCase;

    @Value("${aws.sqs.incoming-queue.url}")
    private String queueUrl;

    // Tuning
    @Value("${sqs.incoming.poll-interval-ms:2000}")
    private long pollMs;

    @Value("${sqs.incoming.max-messages:10}")
    private int maxMessages;

    @Value("${sqs.incoming.wait-seconds:10}")
    private int waitSeconds;

    @Value("${sqs.incoming.visibility-timeout:30}")
    private int visibilitySeconds;

    @Value("${sqs.incoming.fifo.max-concurrent-groups:4}")
    private int maxConcurrentGroups;

    private Disposable subscription;

    @PostConstruct
    public void start() {
        log.info("Iniciando SqsInboundFifoPoller queue={}, wait={}, vis={}, groups={}",
                queueUrl, waitSeconds, visibilitySeconds, maxConcurrentGroups);

        subscription = Mono.defer(this::receiveAndProcessOnce)
                .repeatWhen(repeat -> repeat.delayElements(Duration.ofMillis(pollMs)))
                .onErrorContinue((e, o) -> log.error("Error en ciclo inbound FIFO (continuando): {}", e.toString(), e))
                .subscribe();

        log.info("SqsInboundFifoPoller suscrito.");
    }

    private Mono<Void> receiveAndProcessOnce() {
        return Mono.fromFuture(sqsAsyncClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(maxMessages)
                        .waitTimeSeconds(waitSeconds)      // usa long polling alto (10–20s)
                        .visibilityTimeout(visibilitySeconds)
                        .messageSystemAttributeNamesWithStrings(
                                "MessageGroupId", "MessageDeduplicationId",
                                "SequenceNumber", "SentTimestamp")
                        .messageAttributeNames("All")
                        .build()))
                .doOnNext(resp -> log.debug("SQS poll: {} mensajes", resp.messages().size()))
                .flatMapMany(resp -> Flux.fromIterable(resp.messages()))
                .groupBy(msg -> msg.attributes().get(MessageSystemAttributeName.MESSAGE_GROUP_ID))
                .flatMap(groupFlux -> groupFlux.concatMap(this::processMessage), maxConcurrentGroups)
                .then();
    }

    @PreDestroy
    public void stop() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("SqsInboundFifoPoller detenido.");
        }
    }

    /** Body esperado: {"idApplication":"<uuid>","estado":"Aprobada|Rechazada|Pendiente de revision"} */
    @Data
    public static class DecisionMessage {
        private UUID idApplication;
        private String estado;
    }

    private Mono<DecisionMessage> parseDecision(Message msg) {
        return Mono.fromCallable(() -> {
            String body = msg.body();
            JsonNode root = objectMapper.readTree(body);
            if (root.has("Type") && "Notification".equals(root.path("Type").asText()) && root.has("Message")) {
                String inner = root.path("Message").asText();
                return objectMapper.readValue(inner, DecisionMessage.class);
            }
            return objectMapper.readValue(body, DecisionMessage.class);
        });
    }

    //System.out.println("SQS INBOUND (FIFO) >>> body=" + msg.body());
    private Mono<Void> processMessage(Message msg) {
        return parseDecision(msg)
                .flatMap(decision -> {
                    String estado = decision.getEstado() == null ? "" : decision.getEstado().trim();
                    UUID id = decision.getIdApplication();
                    log.info("Inbound decision: idApplication={}, estado={}", id, estado);
                    if (estado.equalsIgnoreCase(ApplicationStatus.PENDING.getStatus())) {
                        log.info("Estado 'Pendiente de revision': no se actualiza BD. ACK directo.");
                        return deleteMessage(msg);
                    }
                    return applicationUseCase.changeApplicationStatus(id, estado)
                            .doOnSuccess(det ->
                                    log.info("Estado actualizado (inbound): id={}, nuevo={}, email={}",
                                            det.getId(), det.getStatus(), det.getEmail()))
                            .then(deleteMessage(msg));
                })
                .onErrorResume(ex -> {
                    log.error("Fallo procesando mensaje FIFO id={}, error={}", msg.messageId(), ex.toString(), ex);
                    return Mono.empty();
                });
    }

    private Mono<Void> deleteMessage(Message msg) {
        return Mono.fromFuture(sqsAsyncClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(msg.receiptHandle())
                        .build()))
                .then()
                .doOnSuccess(v -> log.info("Mensaje FIFO eliminado. id={}", msg.messageId()));
    }

}
