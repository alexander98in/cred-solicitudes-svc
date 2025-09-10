package co.com.pragma.solicitud.usecase.application;

import co.com.pragma.solicitud.model.application.Application;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ApplicationUseCase {

    Mono<Application> createApplication(Application application, String email);
    Flux<Application> getAllApplications();
    Mono<Application> getApplicationById(UUID id);

}
