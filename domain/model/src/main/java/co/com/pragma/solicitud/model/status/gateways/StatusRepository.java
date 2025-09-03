package co.com.pragma.solicitud.model.status.gateways;

import co.com.pragma.solicitud.model.status.Status;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface StatusRepository {

    Mono<Status> findStatusById(UUID id);
    Mono<Boolean> existsStatusById(UUID id);
}
