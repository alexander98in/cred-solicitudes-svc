package co.com.pragma.solicitud.usecase.application;

import co.com.pragma.solicitud.model.application.Application;
import co.com.pragma.solicitud.model.application.ApplicationDetails;
import co.com.pragma.solicitud.model.application.ApplicationFilter;
import co.com.pragma.solicitud.model.application.PaginatedApplications;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ApplicationUseCase {

    Mono<Application> createApplication(Application application, String email);
    Flux<Application> getAllApplications();
    Mono<Application> getApplicationById(UUID id);
    Mono<PaginatedApplications> getApplicationsByPage(ApplicationFilter filter);

    /**
     * Aprueba o rechaza una solicitud cambiando su estado.
     * @param idApplication ID de la solicitud
     * @param targetStatus Nombre del estado destino ("Aprobada" o "Rechazada")
     * @return Mono<ApplicationDetails> con la solicitud actualizada
     */
    Mono<ApplicationDetails> changeApplicationStatus(UUID idApplication, String targetStatus);
}
