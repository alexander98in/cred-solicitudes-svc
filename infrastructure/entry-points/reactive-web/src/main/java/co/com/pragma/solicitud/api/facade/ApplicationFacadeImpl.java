package co.com.pragma.solicitud.api.facade;

import co.com.pragma.solicitud.api.dto.request.ApplicationRequestDTO;
import co.com.pragma.solicitud.api.dto.response.ApplicationResponseDTO;
import co.com.pragma.solicitud.api.mapper.ApplicationDTOMapper;
import co.com.pragma.solicitud.model.application.ApplicationFilter;
import co.com.pragma.solicitud.model.application.PaginatedApplications;
import co.com.pragma.solicitud.model.auth.gateways.TokenService;
import co.com.pragma.solicitud.usecase.application.ApplicationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ApplicationFacadeImpl implements ApplicationFacade {

    private final ApplicationUseCase applicationUseCase;
    private final TokenService tokenService;
    private final ApplicationDTOMapper mapper;
    private final ReactiveTransactionManager txManager;

    private TransactionalOperator operator() { return TransactionalOperator.create(txManager); }

    private <T> Mono<T> transactional(Mono<T> mono) {
        return mono.as(operator()::transactional);
    }

    @Override
    public Mono<ApplicationResponseDTO> registerApplication(ApplicationRequestDTO dto) {
        return tokenService.getEmailFromContext()
                .flatMap(email ->{
                    var domain = mapper.toDomain(dto);
                    return transactional(
                            applicationUseCase.createApplication(domain, email)
                                    .map(mapper::toResponse)
                    );
                });
    }

    @Override
    public Flux<ApplicationResponseDTO> getApplications() {
        return Flux.defer(() ->
                applicationUseCase.getAllApplications()
                        .map(mapper::toResponse)
        );
    }

    @Override
    public Mono<ApplicationResponseDTO> getApplicationById(String id) {
        return null;
    }

    @Override
    public Mono<PaginatedApplications> getApplicationsByPage(ApplicationFilter filter) {
        return Mono.defer(() ->
                applicationUseCase.getApplicationsByPage(filter)
        );
    }
}
