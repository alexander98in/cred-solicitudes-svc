package co.com.pragma.solicitud.model.application.gateways;

import co.com.pragma.solicitud.model.application.events.ApplicationAutoValidationEvent;
import reactor.core.publisher.Mono;

public interface ValidationQueue {
    Mono<Void> publishAutoValidationApplicationEvent(ApplicationAutoValidationEvent event);
}
