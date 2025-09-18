package co.com.pragma.solicitud.model.application.gateways;

import co.com.pragma.solicitud.model.application.events.ApplicationReportEvent;
import reactor.core.publisher.Mono;

public interface ReportQueue {
    Mono<Void> publishReportGenerationEvent(ApplicationReportEvent event);
}
