package co.com.pragma.solicitud.api.facade;

import co.com.pragma.solicitud.api.dto.request.ApplicationRequestDTO;
import co.com.pragma.solicitud.api.dto.response.ApplicationResponseDTO;
import co.com.pragma.solicitud.api.dto.response.ApplicationUpdateResponseDTO;
import co.com.pragma.solicitud.model.application.ApplicationFilter;
import co.com.pragma.solicitud.model.application.PaginatedApplications;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApplicationFacade {

    Mono<ApplicationResponseDTO> registerApplication(ApplicationRequestDTO dto);

    Flux<ApplicationResponseDTO> getApplications();

    Mono<ApplicationResponseDTO> getApplicationById(String id);

    Mono<PaginatedApplications> getApplicationsByPage(ApplicationFilter filter);

    /**
     * Aprueba o rechaza una solicitud cambiando su estado.
     * @param idApplication id de la solicitud
     * @param targetStatus nombre del estado destino ("Aprobada" o "Rechazada")
     * @return Mono<ApplicationUpdateResponseDTO> con la solicitud actualizada
     */
    Mono<ApplicationUpdateResponseDTO> changeApplicationStatus(String idApplication, String targetStatus);
}
