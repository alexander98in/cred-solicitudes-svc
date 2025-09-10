package co.com.pragma.solicitud.r2dbc;

import co.com.pragma.solicitud.r2dbc.entity.StatusEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface StatusReactiveRepository extends ReactiveCrudRepository<StatusEntity, UUID>, ReactiveQueryByExampleExecutor<StatusEntity> {

    /**
     * Find status by description.
     * @param description
     * @return Mono<StatusEntity>
     */
    Mono<StatusEntity> findByDescription(String description);
}
