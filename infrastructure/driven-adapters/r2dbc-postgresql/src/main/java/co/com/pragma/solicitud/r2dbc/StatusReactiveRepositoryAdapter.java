package co.com.pragma.solicitud.r2dbc;

import co.com.pragma.solicitud.model.status.Status;
import co.com.pragma.solicitud.model.status.gateways.StatusRepository;
import co.com.pragma.solicitud.r2dbc.entity.StatusEntity;
import co.com.pragma.solicitud.r2dbc.helper.ReactiveAdapterOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class StatusReactiveRepositoryAdapter extends ReactiveAdapterOperations<Status, StatusEntity, UUID, StatusReactiveRepository> implements StatusRepository {

    public StatusReactiveRepositoryAdapter(StatusReactiveRepository repository, org.reactivecommons.utils.ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, Status.class));
    }

    @Override
    public Mono<Status> getStatusById(UUID id) {
        return repository.findById(id)
            .map(this::toEntity);
    }

    @Override
    public Mono<Boolean> existsStatusById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public Mono<Status> getStatusByDescription(String description) {
        return repository.findByDescription(description)
                .map(this::toEntity);
    }
}
