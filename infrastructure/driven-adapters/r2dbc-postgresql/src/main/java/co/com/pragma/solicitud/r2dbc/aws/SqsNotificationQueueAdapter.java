package co.com.pragma.solicitud.r2dbc.aws;

import co.com.pragma.solicitud.model.application.events.ApplicationStatusChangedEvent;
import co.com.pragma.solicitud.model.application.gateways.NotificationQueue;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SqsNotificationQueueAdapter implements NotificationQueue {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    @Value("${aws.sqs.is-fifo:false}")
    private boolean isFifo;


    @Override
    public Mono<Void> publishStatusChangedEvent(ApplicationStatusChangedEvent event) {

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .flatMap(json -> {
                    SendMessageRequest.Builder builder = SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(json);

                    if (isFifo) {
                        // Para colas FIFO se requiere MessageGroupId y (opcional) DeduplicationId
                        builder = builder
                                .messageGroupId(event.getEmail() != null ? event.getEmail() : "default-group")
                                .messageDeduplicationId(UUID.randomUUID().toString());
                    }
                    return Mono.fromFuture(sqsAsyncClient.sendMessage(builder.build())).then();
                });
    }
}
