package co.com.pragma.solicitud.model.application.gateways;

import co.com.pragma.solicitud.model.application.events.ApplicationStatusChangedEvent;
import reactor.core.publisher.Mono;

public interface NotificationQueue {
    Mono<Void> publishStatusChangedEvent(ApplicationStatusChangedEvent event);
}
