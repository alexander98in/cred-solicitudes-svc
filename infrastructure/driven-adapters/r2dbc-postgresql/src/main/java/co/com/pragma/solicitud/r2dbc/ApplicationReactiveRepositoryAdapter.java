package co.com.pragma.solicitud.r2dbc;

import co.com.pragma.solicitud.model.application.Application;
import co.com.pragma.solicitud.model.application.gateways.ApplicationRepository;
import co.com.pragma.solicitud.r2dbc.entity.ApplicationEntity;
import co.com.pragma.solicitud.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class ApplicationReactiveRepositoryAdapter extends ReactiveAdapterOperations<Application, ApplicationEntity, UUID, ApplicationReactiveRepository> implements ApplicationRepository {

    public ApplicationReactiveRepositoryAdapter(ApplicationReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, Application.class));
    }

    @Override
    public Mono<Application> saveApplication(Application application) {
        return repository.save(toData(application))
                .map(this::toEntity);
    }

    @Override
    public Flux<Application> findAllApplications() {
        return repository.findAll()
                .map(this::toEntity);
    }

    @Override
    public Mono<Application> findApplicationById(UUID id) {
        return repository.findById(id)
                .map(this::toEntity);
    }

    @Override
    public Mono<Integer> updateStatus(UUID idApp, UUID currentStatus, UUID newStatus) {
        return repository.updateStatus(idApp, currentStatus, newStatus);
    }

    @Override
    public Flux<Application> findByUserAndStatud(UUID idUser, UUID idStatus) {
        return repository.findByIdUserAndIdStatus(idUser, idStatus)
                .map(this::toEntity);
    }
}
