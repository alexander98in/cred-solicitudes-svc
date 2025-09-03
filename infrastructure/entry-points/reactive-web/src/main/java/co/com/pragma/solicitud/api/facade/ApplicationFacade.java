package co.com.pragma.solicitud.api.facade;

import co.com.pragma.solicitud.api.dto.request.ApplicationRequestDTO;
import co.com.pragma.solicitud.api.dto.response.ApplicationResponseDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApplicationFacade {

    Mono<ApplicationResponseDTO> registerApplication(ApplicationRequestDTO dto);

    Flux<ApplicationResponseDTO> getApplications();

    Mono<ApplicationResponseDTO> getApplicationById(String id);

}
