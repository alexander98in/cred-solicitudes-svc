package co.com.pragma.solicitud.r2dbc;

import co.com.pragma.solicitud.model.loantype.LoanType;
import co.com.pragma.solicitud.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.solicitud.r2dbc.entity.LoanTypeEntity;
import co.com.pragma.solicitud.r2dbc.helper.ReactiveAdapterOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class LoanTypeReactiveRepositoryAdapter extends ReactiveAdapterOperations<LoanType, LoanTypeEntity, UUID, LoanTypeReactiveRepository> implements LoanTypeRepository {

    public LoanTypeReactiveRepositoryAdapter(LoanTypeReactiveRepository repository, org.reactivecommons.utils.ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, LoanType.class));
    }

    @Override
    public Mono<LoanType> getLoanTypeById(UUID id) {
        return repository.findById(id)
            .map(this::toEntity);
    }

    @Override
    public Flux<LoanType> findLoanTypeAll() {
        return repository.findAll()
            .map(this::toEntity);
    }

    @Override
    public Mono<Boolean> existsLoanTypeById(UUID id) {
        return repository.existsById(id);
    }
}
