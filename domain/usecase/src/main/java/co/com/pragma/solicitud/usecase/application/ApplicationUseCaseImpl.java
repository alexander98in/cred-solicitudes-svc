package co.com.pragma.solicitud.usecase.application;

import co.com.pragma.solicitud.model.application.Application;
import co.com.pragma.solicitud.model.application.gateways.ApplicationRepository;
import co.com.pragma.solicitud.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.solicitud.model.status.gateways.StatusRepository;
import co.com.pragma.solicitud.model.user.RemoteUser;
import co.com.pragma.solicitud.model.user.gateways.ExternalUserService;
import co.com.pragma.solicitud.usecase.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ApplicationUseCaseImpl implements ApplicationUseCase{

    private final ApplicationRepository applicationRepository;
    private final ExternalUserService externalUserService;
    private final StatusRepository statusRepository;
    private final LoanTypeRepository loanTypeRepository;

    @Override
    public Mono<Application> createApplication(Application application, String documentId) {
        return Mono.zip(
                        statusRepository.existsStatusById(application.getIdStatus()),
                        loanTypeRepository.existsLoanTypeById(application.getIdLoanType()),
                        externalUserService.getUserByDocumentId(documentId)
                )
                .flatMap(tuple -> {
                    boolean statusExists = tuple.getT1();
                    boolean loanTypeExists = tuple.getT2();
                    RemoteUser remoteUser = tuple.getT3();

                    if (!statusExists) {
                        return Mono.error(new ResourceNotFoundException(
                                "No existe el estado con id: " + application.getIdStatus()));
                    }
                    if (!loanTypeExists) {
                        return Mono.error(new ResourceNotFoundException(
                                "No existe el tipo de crédito con id: " + application.getIdLoanType()));
                    }
                    if (remoteUser == null) {
                        return Mono.error(new ResourceNotFoundException("Usuario no encontrado con el documento: " + documentId));
                    }

                    application.setIdUser(remoteUser.id());
                    return applicationRepository.saveApplication(application);
                })
                .onErrorMap(e -> {
                    if (e instanceof NullPointerException) {
                        return new RuntimeException("Un valor requerido fue nulo: " + e.getMessage(), e);
                    }
                    return e; // Deja que otros errores sigan su flujo
                });
    }

    @Override
    public Flux<Application> getAllApplications() {
        return applicationRepository.findAllApplications();
    }

    @Override
    public Mono<Application> getApplicationById(UUID id) {
        return null;
    }
}
