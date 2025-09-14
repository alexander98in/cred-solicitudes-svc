package co.com.pragma.solicitud.usecase.application;

import co.com.pragma.solicitud.model.application.Application;
import co.com.pragma.solicitud.model.application.ApplicationDetails;
import co.com.pragma.solicitud.model.application.ApplicationFilter;
import co.com.pragma.solicitud.model.application.PaginatedApplications;
import co.com.pragma.solicitud.model.application.gateways.ApplicationCustomRepository;
import co.com.pragma.solicitud.model.application.gateways.ApplicationRepository;
import co.com.pragma.solicitud.model.loantype.LoanType;
import co.com.pragma.solicitud.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.solicitud.model.status.Status;
import co.com.pragma.solicitud.model.status.gateways.StatusRepository;
import co.com.pragma.solicitud.model.user.RemoteUser;
import co.com.pragma.solicitud.model.user.gateways.ExternalUserService;
import co.com.pragma.solicitud.usecase.exceptions.BusinessRuleViolationException;
import co.com.pragma.solicitud.usecase.utils.ErrorCodeDomain;
import co.com.pragma.solicitud.usecase.exceptions.ResourceNotFoundException;
import co.com.pragma.solicitud.usecase.utils.LoanMath;
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
    private final ApplicationCustomRepository applicationCustomRepository;

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
                .onErrorMap(e -> e);
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

    @Override
    public Mono<PaginatedApplications> getApplicationsByPage(ApplicationFilter filter) {
        return applicationCustomRepository.listApplicationsByCriteria(filter)
                .map(paginated -> {
                    paginated.getContent().forEach(app -> {
                        BigDecimal installment = LoanMath.monthlyInstallment(
                                app.getAmount(),
                                app.getTerm(),
                                app.getInterestRate() // anual en %
                        );
                        app.setMonthlyInstallment(installment);
                        // app.setMonthlyDebt(...);
                        // app.setMonthlyInstallment(...);
                    });
                    return paginated;
                });
    }

    /**
     * Cambia el estado de una solicitud a "Aprobado" o "Rechazado" solo si está en estado "Pendiente de revisión".
     * @param idApplication ID de la solicitud
     * @param targetStatus Nombre del estado destino ("Aprobado" o "Rechazado")
     * @return Mono<ApplicationDetails> con la solicitud actualizada
     */
    @Override
    public Mono<ApplicationDetails> changeApplicationStatus(UUID idApplication, String targetStatus) {
        final String PENDING_STATUS = "Pendiente de revisión";
        final String APPROVED_STATUS = "Aprobado";
        final String REJECTED_STATUS = "Rechazado";

        return Mono.justOrEmpty(targetStatus)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .switchIfEmpty(Mono.error(new BusinessRuleViolationException("CRED-6005", "El estado destino es obligatorio")))
                .filter(s -> s.equalsIgnoreCase(APPROVED_STATUS) || s.equalsIgnoreCase(REJECTED_STATUS))
                .switchIfEmpty(Mono.error(new BusinessRuleViolationException("CRED-6006", "Solo se permite actualizar a los estados 'Aprobado' o 'Rechazado'")))
                .flatMap(normalizedStatus -> {
                    Mono<Application> appMono = applicationRepository.findApplicationById(idApplication)
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException("No existe la solicitud con id: " + idApplication)));

                    Mono<UUID> pendingIdMono = statusRepository.getStatusByDescription(PENDING_STATUS)
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                    ErrorCodeDomain.PENDING_STATUS_NOT_FOUND.getCode(),
                                    String.format(ErrorCodeDomain.PENDING_STATUS_NOT_FOUND.getMessage())
                            )))
                            .map(Status::getIdStatus);

                    Mono<UUID> targetIdStatusMono = statusRepository
                            .getStatusByDescription(normalizedStatus.equalsIgnoreCase(APPROVED_STATUS) ? APPROVED_STATUS : REJECTED_STATUS)
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                    "CRED-4004",
                                    "Estado detino '" + normalizedStatus + "' no encontrado"
                            )))
                            .map(Status::getIdStatus);

                    return Mono.zip(appMono, pendingIdMono, targetIdStatusMono)
                            .flatMap(tuple -> {
                                Application app = tuple.getT1();
                                UUID pendingId = tuple.getT2();
                                UUID targetId = tuple.getT3();

                                if(!pendingId.equals(app.getIdStatus())) {
                                    return Mono.error(new BusinessRuleViolationException("CRED-6007", "Solo se pueden actualizar solicitudes en estado 'Pendiente de revisión'"));
                                }
                                return applicationRepository
                                        .updateStatus(app.getIdApplication(), pendingId, targetId)
                                        .flatMap(rows -> {
                                            if(rows == 0) {
                                                return Mono.error(new BusinessRuleViolationException(
                                                        "CRED-6004",
                                                        "La solicitud cambió de estado por otro proceso, intente de nuevo"
                                                ));
                                            }

                                            return applicationRepository.findApplicationById(app.getIdApplication())
                                                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("No existe la solicitud con id: " + app.getIdApplication())))
                                                    .flatMap(updateApp -> {
                                                        UUID newStatusId = updateApp.getIdStatus();
                                                        return statusRepository.getStatusById(newStatusId)
                                                                .defaultIfEmpty(new Status(newStatusId, "Desconocido"))
                                                                .map(status -> ApplicationDetails.builder()
                                                                        .id(updateApp.getIdApplication())
                                                                        .amount(updateApp.getAmount())
                                                                        .term(updateApp.getTerm())
                                                                        .email(updateApp.getEmail())
                                                                        .status(status.getDescription())
                                                                        .build());
                                                    });
                                        });
                            });
                });
    }

}
