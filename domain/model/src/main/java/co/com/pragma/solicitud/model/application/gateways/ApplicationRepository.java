package co.com.pragma.solicitud.model.application.gateways;

import co.com.pragma.solicitud.model.application.Application;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ApplicationRepository {

    Mono<Application> saveApplication(Application application);
    Flux<Application> findAllApplications();
    Mono<Application> findApplicationById(UUID id);
}
