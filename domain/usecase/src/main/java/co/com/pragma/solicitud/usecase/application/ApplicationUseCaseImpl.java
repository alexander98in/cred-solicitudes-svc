package co.com.pragma.solicitud.usecase.application;

import co.com.pragma.solicitud.model.application.Application;
import co.com.pragma.solicitud.model.application.ApplicationDetails;
import co.com.pragma.solicitud.model.application.ApplicationFilter;
import co.com.pragma.solicitud.model.application.PaginatedApplications;
import co.com.pragma.solicitud.model.application.events.ApplicationAutoValidationEvent;
import co.com.pragma.solicitud.model.application.events.ApplicationReportEvent;
import co.com.pragma.solicitud.model.application.events.ApplicationStatusChangedEvent;
import co.com.pragma.solicitud.model.application.events.ApprovedApplicationSummary;
import co.com.pragma.solicitud.model.application.gateways.ApplicationCustomRepository;
import co.com.pragma.solicitud.model.application.gateways.ApplicationRepository;
import co.com.pragma.solicitud.model.application.gateways.NotificationQueue;
import co.com.pragma.solicitud.model.loantype.LoanType;
import co.com.pragma.solicitud.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.solicitud.model.outbox.OutboxEvent;
import co.com.pragma.solicitud.model.outbox.gateways.OutboxRepository;
import co.com.pragma.solicitud.model.status.Status;
import co.com.pragma.solicitud.model.status.gateways.StatusRepository;
import co.com.pragma.solicitud.model.user.RemoteUser;
import co.com.pragma.solicitud.model.user.gateways.ExternalUserService;
import co.com.pragma.solicitud.usecase.exceptions.BusinessRuleViolationException;
import co.com.pragma.solicitud.usecase.utils.ApplicationStatus;
import co.com.pragma.solicitud.usecase.utils.ErrorCodeDomain;
import co.com.pragma.solicitud.usecase.exceptions.ResourceNotFoundException;
import co.com.pragma.solicitud.usecase.utils.LoanMath;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ApplicationUseCaseImpl implements ApplicationUseCase{

    private final ApplicationRepository applicationRepository;
    private final ExternalUserService externalUserService;
    private final StatusRepository statusRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final ApplicationCustomRepository applicationCustomRepository;
    private final OutboxRepository outboxRepository;

    @Override
    public Mono<Application> createApplication(Application application, String email) {
        return Mono.zip(
                        statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus())
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
                    return applicationRepository.saveApplication(application)
                            .flatMap(applicationSaved -> {
                                if(Boolean.TRUE.equals(loanType.getValidationAutomatic())) {
                                    Mono<UUID> approvedStatusIdMono =
                                            statusRepository.getStatusByDescription(ApplicationStatus.APPROVED.getStatus())
                                                    .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                            ErrorCodeDomain.STATUS_NOT_FOUND.getCode(),
                                                            String.format(ErrorCodeDomain.STATUS_NOT_FOUND.getMessage(), ApplicationStatus.APPROVED.getStatus())
                                                    )))
                                                    .map(Status::getIdStatus);

                                    Mono<List<ApprovedApplicationSummary>> approvedApplicationSumariesMono =
                                            approvedStatusIdMono
                                                    .flatMapMany(approvedId ->
                                                            applicationRepository.findByUserAndStatud(remoteUser.id(), approvedId))
                                                    .concatMap(approvedApplication -> loanTypeRepository.getLoanTypeById(approvedApplication.getIdLoanType())
                                                            .map(loanTypeDB -> ApprovedApplicationSummary.builder()
                                                                    .idApplication(approvedApplication.getIdApplication())
                                                                    .amount(approvedApplication.getAmount())
                                                                    .term(approvedApplication.getTerm())
                                                                    .interestRate(loanTypeDB.getInterestRate())
                                                                    .minAmount(loanTypeDB.getMinAmount())
                                                                    .maxAmount(loanTypeDB.getMaxAmount())
                                                                    .build()
                                                            )
                                                    )
                                                    .collectList();

                                    return approvedApplicationSumariesMono
                                            .flatMap(approvedSummaries -> {
                                                var event = ApplicationAutoValidationEvent.builder()
                                                        .idApplication(applicationSaved.getIdApplication())
                                                        .amount(applicationSaved.getAmount())
                                                        .term(applicationSaved.getTerm())
                                                        .email(applicationSaved.getEmail())
                                                        .idLoanType(loanType.getIdLoanType())
                                                        .loanTypeName(loanType.getName())
                                                        .interestRate(loanType.getInterestRate())
                                                        .minAmount(loanType.getMinAmount())
                                                        .maxAmount(loanType.getMaxAmount())
                                                        .idUser(remoteUser.id())
                                                        .emailUser(remoteUser.email())
                                                        .fullNameUser(remoteUser.name() + " " + remoteUser.lastName())
                                                        .salaryUser(remoteUser.salary())
                                                        .approvedApplicationSummaries(approvedSummaries)
                                                        .occurredAt(OffsetDateTime.now())
                                                        .build();

                                                var outbox = OutboxEvent.builder()
                                                        .aggregateId(applicationSaved.getIdApplication())
                                                        .eventType("ApplicationAutoValidationEvent")
                                                        .payload(event)
                                                        .occurredAt(OffsetDateTime.now())
                                                        .processed(false)
                                                        .retries(0)
                                                        .build();

                                                return outboxRepository.save(outbox)
                                                        .thenReturn(applicationSaved);

                                            });
                                }
                                return  Mono.just(applicationSaved);
                            });
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
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        ErrorCodeDomain.APPLICATION_NOT_FOUND.getCode(),
                        String.format(ErrorCodeDomain.APPLICATION_NOT_FOUND.getMessage(), id)
                )));
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
        return Mono.justOrEmpty(targetStatus)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .switchIfEmpty(Mono.error(new BusinessRuleViolationException(ErrorCodeDomain.STATUS_OBLIGATORY.getCode(), ErrorCodeDomain.STATUS_OBLIGATORY.getMessage())))
                .filter(s -> s.equalsIgnoreCase(ApplicationStatus.APPROVED.getStatus()) || s.equalsIgnoreCase(ApplicationStatus.REJECTED.getStatus()))
                .switchIfEmpty(Mono.error(new BusinessRuleViolationException(ErrorCodeDomain.STATUS_NOT_VALID.getCode(), ErrorCodeDomain.STATUS_NOT_VALID.getMessage())))
                .flatMap(normalizedStatus -> {
                    Mono<Application> appMono = applicationRepository.findApplicationById(idApplication)
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                    ErrorCodeDomain.APPLICATION_NOT_FOUND.getCode(),
                                    String.format(ErrorCodeDomain.APPLICATION_NOT_FOUND.getMessage(), idApplication)
                            )));

                    Mono<UUID> pendingIdMono = statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus())
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                    ErrorCodeDomain.PENDING_STATUS_NOT_FOUND.getCode(),
                                    String.format(ErrorCodeDomain.PENDING_STATUS_NOT_FOUND.getMessage())
                            )))
                            .map(Status::getIdStatus);

                    Mono<UUID> targetIdStatusMono = statusRepository
                            .getStatusByDescription(normalizedStatus.equalsIgnoreCase(ApplicationStatus.APPROVED.getStatus()) ? ApplicationStatus.APPROVED.getStatus() : ApplicationStatus.REJECTED.getStatus())
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                    ErrorCodeDomain.STATUS_NOT_FOUND.getCode(),
                                    String.format(ErrorCodeDomain.STATUS_NOT_FOUND.getMessage(), normalizedStatus)
                            )))
                            .map(Status::getIdStatus);

                    return Mono.zip(appMono, pendingIdMono, targetIdStatusMono)
                            .flatMap(tuple -> {
                                Application app = tuple.getT1();
                                UUID pendingId = tuple.getT2();
                                UUID targetId = tuple.getT3();

                                if(!pendingId.equals(app.getIdStatus())) {
                                    return Mono.error(new BusinessRuleViolationException(
                                            ErrorCodeDomain.APPLICATION_CHANGE_STATUS.getCode(),
                                            ErrorCodeDomain.APPLICATION_CHANGE_STATUS.getMessage()
                                    ));
                                }
                                return applicationRepository
                                        .updateStatus(app.getIdApplication(), pendingId, targetId)
                                        .flatMap(rows -> {
                                            if(rows == 0) {
                                                return Mono.error(new BusinessRuleViolationException(
                                                        ErrorCodeDomain.APPLICATION_CONFLICT_CHANGE_STATUS.getCode(),
                                                        ErrorCodeDomain.APPLICATION_CONFLICT_CHANGE_STATUS.getMessage()
                                                ));
                                            }

                                            return applicationRepository.findApplicationById(app.getIdApplication())
                                                    .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                                            ErrorCodeDomain.APPLICATION_NOT_FOUND.getCode(),
                                                            String.format(ErrorCodeDomain.APPLICATION_NOT_FOUND.getMessage(), idApplication)
                                                    )))
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
                                                    })
                                                    .flatMap(applicationDetails -> {
                                                        var evt = ApplicationStatusChangedEvent.builder()
                                                                .idApplication(applicationDetails.getId())
                                                                .newStatus(applicationDetails.getStatus())
                                                                .email(applicationDetails.getEmail())
                                                                .amount(applicationDetails.getAmount())
                                                                .term(BigDecimal.valueOf(applicationDetails.getTerm()))
                                                                .occurredAt(java.time.OffsetDateTime.now())
                                                                .build();

                                                        var outbox = OutboxEvent.builder()
                                                                .aggregateId(evt.idApplication())
                                                                .eventType("ApplicationStatusChangedEvent")
                                                                .payload(evt)
                                                                .occurredAt(java.time.OffsetDateTime.now())
                                                                .processed(false)
                                                                .retries(0)
                                                                .build();

                                                        Mono<Void> reportEventMono = Mono.empty(); // Inicializamos el Mono vacío

                                                        if (ApplicationStatus.APPROVED.getStatus().equalsIgnoreCase(applicationDetails.getStatus())) {
                                                            var reportEvent = ApplicationReportEvent.builder()
                                                                    .idApplication(applicationDetails.getId())
                                                                    .newStatus(applicationDetails.getStatus())
                                                                    .amount(applicationDetails.getAmount())
                                                                    .occurredAt(java.time.OffsetDateTime.now())
                                                                    .build();

                                                            // Crear un OutboxEvent para el evento ApplicationReportEvent
                                                            var reportOutbox = OutboxEvent.builder()
                                                                    .aggregateId(reportEvent.idApplication())
                                                                    .eventType("ApplicationReportEvent")
                                                                    .payload(reportEvent)
                                                                    .occurredAt(java.time.OffsetDateTime.now())
                                                                    .processed(false)
                                                                    .retries(0)
                                                                    .build();

                                                            // Guardar el evento adicional en Outbox
                                                            reportEventMono = outboxRepository.save(reportOutbox);
                                                        }

                                                        return outboxRepository.save(outbox)
                                                                .then(reportEventMono)  // Aseguramos que ambos eventos se guarden en Outbox
                                                                .thenReturn(applicationDetails);
                                                    });
                                        });
                            });
                });
    }

}
