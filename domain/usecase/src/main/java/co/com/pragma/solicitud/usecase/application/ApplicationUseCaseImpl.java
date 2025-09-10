package co.com.pragma.solicitud.usecase.application;

import co.com.pragma.solicitud.model.application.Application;
import co.com.pragma.solicitud.model.application.gateways.ApplicationRepository;
import co.com.pragma.solicitud.model.loantype.LoanType;
import co.com.pragma.solicitud.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.solicitud.model.status.Status;
import co.com.pragma.solicitud.model.status.gateways.StatusRepository;
import co.com.pragma.solicitud.model.user.RemoteUser;
import co.com.pragma.solicitud.model.user.gateways.ExternalUserService;
import co.com.pragma.solicitud.usecase.exceptions.BusinessRuleViolationException;
import co.com.pragma.solicitud.usecase.exceptions.ErrorCodeDomain;
import co.com.pragma.solicitud.usecase.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class ApplicationUseCaseImpl implements ApplicationUseCase{

    private final ApplicationRepository applicationRepository;
    private final ExternalUserService externalUserService;
    private final StatusRepository statusRepository;
    private final LoanTypeRepository loanTypeRepository;

    @Override
    public Mono<Application> createApplication(Application application, String email) {
        return Mono.zip(
                        statusRepository.getStatusByDescription("Pendiente de revisión")
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                        ErrorCodeDomain.PENDING_STATUS_NOT_FOUND.getCode(),
                                        String.format(ErrorCodeDomain.PENDING_STATUS_NOT_FOUND.getMessage())
                                ))),
                        loanTypeRepository.getLoanTypeById(application.getIdLoanType())
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                        ErrorCodeDomain.LOAN_TYPE_NOT_FOUND.getCode(),
                                        String.format(ErrorCodeDomain.LOAN_TYPE_NOT_FOUND.getMessage(), application.getIdLoanType())
                                ))),
                        externalUserService.getUserByEmail(email)
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                        ErrorCodeDomain.USER_NOT_FOUND_WITH_EMAIL.getCode(),
                                        String.format(ErrorCodeDomain.USER_NOT_FOUND_WITH_EMAIL.getMessage(), email)
                                )))
                )
                .flatMap(tuple -> {
                    Status status = tuple.getT1();
                    LoanType loanType = tuple.getT2();
                    RemoteUser remoteUser = tuple.getT3();

                    BigDecimal amount = application.getAmount();
                    if (amount.compareTo(loanType.getMinAmount()) < 0) {
                        return Mono.error(new BusinessRuleViolationException(
                                ErrorCodeDomain.AMOUNT_MINIMUM.getCode(), ErrorCodeDomain.AMOUNT_MINIMUM.getMessage()
                        ));
                    }
                    if (amount.compareTo(loanType.getMaxAmount()) > 0) {
                        return Mono.error(new BusinessRuleViolationException(
                                ErrorCodeDomain.AMOUNT_MAXIMUM.getCode(), ErrorCodeDomain.AMOUNT_MAXIMUM.getMessage()
                        ));
                    }

                    application.setIdStatus(status.getIdStatus());
                    application.setIdLoanType(loanType.getIdLoanType());
                    application.setIdUser(remoteUser.id());
                    application.setEmail(remoteUser.email());
                    return applicationRepository.saveApplication(application);
                })
                .onErrorMap(e -> {
                    if (e instanceof NullPointerException) {
                        return new RuntimeException("Un valor requerido fue nulo: " + e.getMessage(), e);
                    }
                    return e;
                });
    }

    @Override
    public Flux<Application> getAllApplications() {
        return applicationRepository.findAllApplications();
    }

    @Override
    public Mono<Application> getApplicationById(UUID id) {
        return applicationRepository.findApplicationById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("No existe la solicitud con id: " + id)));
    }
}
