package co.com.pragma.solicitud.r2dbc;

import co.com.pragma.solicitud.r2dbc.entity.ApplicationEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ApplicationReactiveRepository extends ReactiveCrudRepository<ApplicationEntity, UUID>, ReactiveQueryByExampleExecutor<ApplicationEntity> {
    @Modifying
    @Query("""
        UPDATE solicitudes
           SET id_estado = :newStatus
        WHERE id_solicitud = :idApp
           AND id_estado   = :currenStatus
    """)
    Mono<Integer> updateStatus(@Param("idApp") UUID id,
                                               @Param("currenStatus") UUID expectedCurrentStatus,
                                               @Param("newStatus") UUID newStatus);
}
