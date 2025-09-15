package co.com.pragma.solicitud.usecase.application.usecase.application;

import co.com.pragma.solicitud.model.application.Application;
import co.com.pragma.solicitud.model.application.ApplicationDetails;
import co.com.pragma.solicitud.model.application.ApplicationFilter;
import co.com.pragma.solicitud.model.application.PaginatedApplications;
import co.com.pragma.solicitud.model.application.gateways.ApplicationCustomRepository;
import co.com.pragma.solicitud.model.application.gateways.ApplicationRepository;
import co.com.pragma.solicitud.model.loantype.LoanType;
import co.com.pragma.solicitud.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.solicitud.model.outbox.OutboxEvent;
import co.com.pragma.solicitud.model.outbox.gateways.OutboxRepository;
import co.com.pragma.solicitud.model.status.Status;
import co.com.pragma.solicitud.model.status.gateways.StatusRepository;
import co.com.pragma.solicitud.model.user.RemoteUser;
import co.com.pragma.solicitud.model.user.gateways.ExternalUserService;
import co.com.pragma.solicitud.usecase.application.ApplicationUseCaseImpl;
import co.com.pragma.solicitud.usecase.exceptions.BusinessRuleViolationException;
import co.com.pragma.solicitud.usecase.exceptions.ResourceNotFoundException;
import co.com.pragma.solicitud.usecase.utils.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ApplicationUseCaseImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ExternalUserService externalUserService;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private LoanTypeRepository loanTypeRepository;

    @Mock
    private ApplicationCustomRepository applicationCustomRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private ApplicationUseCaseImpl useCase;

    private UUID userId;
    private UUID loanTypeId;
    private UUID statusId;
    private Application application;
    private RemoteUser remoteUser;
    private LoanType loanType;
    private Status status;

    private UUID appId;
    private UUID pendingId;
    private UUID approvedId;
    private UUID rejectedId;
    private Status pendingStatus;
    private Status approvedStatus;
    private Status rejectedStatus;
    private Application appPending;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        loanTypeId = UUID.randomUUID();
        statusId = UUID.randomUUID();

        appId = UUID.randomUUID();
        pendingId = UUID.randomUUID();
        approvedId = UUID.randomUUID();
        rejectedId = UUID.randomUUID();

        pendingStatus = Status.builder().idStatus(pendingId).description(ApplicationStatus.PENDING.getStatus()).build();
        approvedStatus = Status.builder().idStatus(approvedId).description(ApplicationStatus.APPROVED.getStatus()).build();
        rejectedStatus = Status.builder().idStatus(rejectedId).description(ApplicationStatus.REJECTED.getStatus()).build();

        appPending = Application.builder()
                .idApplication(appId)
                .amount(new BigDecimal("1500000"))
                .term(10)
                .email("john.doe@email.com")
                .idStatus(pendingId)   // importante: está en Pendiente
                .idLoanType(loanTypeId)
                .idUser(userId)
                .build();

        application = Application.builder()
                .amount(new BigDecimal("1000"))
                .term(12)
                .idLoanType(loanTypeId)
                .build();

        remoteUser = new RemoteUser(
                userId, "John", "Doe", "john.doe@email.com",
                "123456", "3001234567", new BigDecimal("2000"), null, null, null
        );

        loanType = LoanType.builder()
                .idLoanType(loanTypeId)
                .minAmount(new BigDecimal("500"))
                .maxAmount(new BigDecimal("5000"))
                .interestRate(new BigDecimal("5"))
                .build();

        status = Status.builder()
                .idStatus(statusId)
                .description("Pendiente de revisión")
                .build();
    }

    @Test
    void createApplication_Success() {
        when(statusRepository.getStatusByDescription("Pendiente de revisión")).thenReturn(Mono.just(status));
        when(loanTypeRepository.getLoanTypeById(loanTypeId)).thenReturn(Mono.just(loanType));
        when(externalUserService.getUserByEmail("john.doe@email.com")).thenReturn(Mono.just(remoteUser));
        when(applicationRepository.saveApplication(any(Application.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Mono<Application> result = useCase.createApplication(application, "john.doe@email.com");

        StepVerifier.create(result)
                .expectNextMatches(app -> app.getIdUser().equals(userId)
                        && app.getIdStatus().equals(statusId)
                        && app.getIdLoanType().equals(loanTypeId)
                        && app.getEmail().equals(remoteUser.email())
                        && app.getAmount().equals(application.getAmount()))
                .verifyComplete();
    }

    @Test
    void createApplication_Fail_UserNotFound() {
        when(statusRepository.getStatusByDescription("Pendiente de revisión")).thenReturn(Mono.just(status));
        when(loanTypeRepository.getLoanTypeById(loanTypeId)).thenReturn(Mono.just(loanType));
        when(externalUserService.getUserByEmail("john.doe@email.com")).thenReturn(Mono.empty());

        Mono<Application> result = useCase.createApplication(application, "john.doe@email.com");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException
                        && throwable.getMessage().contains("El usuario con email"))
                .verify();
    }

    @Test
    void createApplication_Fail_LoanTypeNotFound() {
        when(statusRepository.getStatusByDescription("Pendiente de revisión"))
                .thenReturn(Mono.just(status));
        when(loanTypeRepository.getLoanTypeById(loanTypeId))
                .thenReturn(Mono.empty());
        when(externalUserService.getUserByEmail(anyString()))
                .thenReturn(Mono.just(remoteUser)); // mockeo necesario

        Mono<Application> result = useCase.createApplication(application, "john.doe@email.com");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException
                        && throwable.getMessage().contains("El tipo de prestamo con ID"))
                .verify();
    }

    @Test
    void createApplication_Fail_StatusNotFound() {
        when(statusRepository.getStatusByDescription("Pendiente de revisión"))
                .thenReturn(Mono.empty());
        when(loanTypeRepository.getLoanTypeById(loanTypeId))
                .thenReturn(Mono.just(loanType));
        when(externalUserService.getUserByEmail(anyString()))
                .thenReturn(Mono.just(remoteUser));

        Mono<Application> result = useCase.createApplication(application, "john.doe@email.com");

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException
                        && throwable.getMessage().contains("Estado 'Pendiente de revisión'"))
                .verify();
    }

    @Test
    void createApplication_Fail_AmountBelowMin() {
        application.setAmount(new BigDecimal("100")); // menos que minAmount

        when(statusRepository.getStatusByDescription("Pendiente de revisión")).thenReturn(Mono.just(status));
        when(loanTypeRepository.getLoanTypeById(loanTypeId)).thenReturn(Mono.just(loanType));
        when(externalUserService.getUserByEmail("john.doe@email.com")).thenReturn(Mono.just(remoteUser));

        Mono<Application> result = useCase.createApplication(application, "john.doe@email.com");

        StepVerifier.create(result)
                .expectError(BusinessRuleViolationException.class)
                .verify();
    }

    @Test
    void createApplication_Fail_AmountAboveMax() {
        application.setAmount(new BigDecimal("10000")); // más que maxAmount

        when(statusRepository.getStatusByDescription("Pendiente de revisión")).thenReturn(Mono.just(status));
        when(loanTypeRepository.getLoanTypeById(loanTypeId)).thenReturn(Mono.just(loanType));
        when(externalUserService.getUserByEmail("john.doe@email.com")).thenReturn(Mono.just(remoteUser));

        Mono<Application> result = useCase.createApplication(application, "john.doe@email.com");

        StepVerifier.create(result)
                .expectError(BusinessRuleViolationException.class)
                .verify();
    }

    @Test
    void getAllApplications_Success() {
        Application app1 = application;
        Application app2 = application.toBuilder().amount(new BigDecimal("2000")).build();

        when(applicationRepository.findAllApplications()).thenReturn(Flux.just(app1, app2));

        StepVerifier.create(useCase.getAllApplications())
                .expectNext(app1)
                .expectNext(app2)
                .verifyComplete();
    }

    @Test
    void getApplicationById_Success() {
        UUID appId = UUID.randomUUID();
        application.setIdApplication(appId);

        when(applicationRepository.findApplicationById(appId)).thenReturn(Mono.just(application));

        StepVerifier.create(useCase.getApplicationById(appId))
                .expectNext(application)
                .verifyComplete();
    }

    @Test
    void getApplicationById_NotFound() {
        UUID appId = UUID.randomUUID();

        when(applicationRepository.findApplicationById(appId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.getApplicationById(appId))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void getApplicationsByPage_Success() {
        ApplicationFilter filter = new ApplicationFilter();
        ApplicationDetails details = ApplicationDetails.builder()
                .amount(application.getAmount())
                .term(application.getTerm())
                .email("john.doe@email.com")
                .interestRate(loanType.getInterestRate())
                .build();

        PaginatedApplications paginated = PaginatedApplications.builder()
                .content(List.of(details))
                .totalElements(1)
                .pageNumber(0)
                .pageSize(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .numberOfElements(1)
                .build();

        when(applicationCustomRepository.listApplicationsByCriteria(filter))
                .thenReturn(Mono.just(paginated));

        StepVerifier.create(useCase.getApplicationsByPage(filter))
                .expectNextMatches(result -> result.getContent().size() == 1 &&
                        result.getContent().get(0).getMonthlyInstallment() != null)
                .verifyComplete();
    }

    // -------------------- changeApplicationStatus: ÉXITO (APROBAR) --------------------
    @Test
    void changeApplicationStatus_Approve_Success() {
        // 1) Buscar app actual (pendiente) y luego con estado actualizado
        when(applicationRepository.findApplicationById(appId))
                .thenReturn(Mono.just(appPending))
                .thenReturn(Mono.just(appPending.toBuilder().idStatus(approvedId).build()));

        // 2) Estado 'Pendiente'
        when(statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus()))
                .thenReturn(Mono.just(pendingStatus));

        // 3) Estado destino 'Aprobado'
        when(statusRepository.getStatusByDescription(ApplicationStatus.APPROVED.getStatus()))
                .thenReturn(Mono.just(approvedStatus));

        // 4) update OK
        when(applicationRepository.updateStatus(appId, pendingId, approvedId))
                .thenReturn(Mono.just(1));

        // 5) guardar en outbox OK
        when(outboxRepository.save(any(OutboxEvent.class))).thenReturn(Mono.empty());

        // 6) IMPORTANTE: al final se consulta el status por ID
        when(statusRepository.getStatusById(approvedId))
                .thenReturn(Mono.just(approvedStatus));

        StepVerifier.create(useCase.changeApplicationStatus(appId, ApplicationStatus.APPROVED.getStatus()))
                .expectNextMatches(details ->
                        details.getId().equals(appId)
                                && ApplicationStatus.APPROVED.getStatus().equals(details.getStatus())
                                && details.getEmail().equals(appPending.getEmail())
                                && details.getAmount().compareTo(appPending.getAmount()) == 0
                                && details.getTerm().equals(appPending.getTerm())
                )
                .verifyComplete();
    }

    // -------------------- changeApplicationStatus: ÉXITO (RECHAZAR) --------------------
    @Test
    void changeApplicationStatus_Reject_Success() {
        when(applicationRepository.findApplicationById(appId))
                .thenReturn(Mono.just(appPending))
                .thenReturn(Mono.just(appPending.toBuilder().idStatus(rejectedId).build()));

        when(statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus()))
                .thenReturn(Mono.just(pendingStatus));
        when(statusRepository.getStatusByDescription(ApplicationStatus.REJECTED.getStatus()))
                .thenReturn(Mono.just(rejectedStatus));

        when(applicationRepository.updateStatus(appId, pendingId, rejectedId))
                .thenReturn(Mono.just(1));

        when(outboxRepository.save(any(OutboxEvent.class))).thenReturn(Mono.empty());

        when(statusRepository.getStatusById(rejectedId))
                .thenReturn(Mono.just(rejectedStatus));

        StepVerifier.create(useCase.changeApplicationStatus(appId, ApplicationStatus.REJECTED.getStatus()))
                .expectNextMatches(details ->
                        details.getId().equals(appId) &&
                                ApplicationStatus.REJECTED.getStatus().equals(details.getStatus()))
                .verifyComplete();
    }

    // -------------------- targetStatus vacío / en blanco --------------------
    @Test
    void changeApplicationStatus_Fail_BlankTargetStatus() {
        StepVerifier.create(useCase.changeApplicationStatus(appId, "   "))
                .expectError(BusinessRuleViolationException.class)
                .verify();
    }

    // -------------------- targetStatus inválido --------------------
    @Test
    void changeApplicationStatus_Fail_InvalidTargetStatus() {
        StepVerifier.create(useCase.changeApplicationStatus(appId, "En estudio"))
                .expectError(BusinessRuleViolationException.class)
                .verify();
    }

    // -------------------- solicitud no existe --------------------
    @Test
    void changeApplicationStatus_Fail_ApplicationNotFound() {
        when(applicationRepository.findApplicationById(appId)).thenReturn(Mono.empty());

        // Debes stubear AMBOS (pendiente y estado destino)
        when(statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus()))
                .thenReturn(Mono.just(pendingStatus));
        when(statusRepository.getStatusByDescription(ApplicationStatus.APPROVED.getStatus()))
                .thenReturn(Mono.just(approvedStatus));

        StepVerifier.create(useCase.changeApplicationStatus(appId, ApplicationStatus.APPROVED.getStatus()))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    // -------------------- estado 'Pendiente' no existe --------------------
    @Test
    void changeApplicationStatus_Fail_PendingStatusNotFound() {
        when(applicationRepository.findApplicationById(appId))
                .thenReturn(Mono.just(appPending));

        // Mock explícito: pendiente vacío (dispara el error esperado)
        when(statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus()))
                .thenReturn(Mono.empty());

        // IMPORTANTE: también mockear la consulta del estado destino
        // (aunque no se llegue lógicamente, se construye el Mono en assembly)
        when(statusRepository.getStatusByDescription(ApplicationStatus.APPROVED.getStatus()))
                .thenReturn(Mono.just(
                        Status.builder()
                                .idStatus(UUID.randomUUID())
                                .description(ApplicationStatus.APPROVED.getStatus())
                                .build()
                ));

        StepVerifier.create(useCase.changeApplicationStatus(appId, ApplicationStatus.APPROVED.getStatus()))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    // -------------------- estado destino no existe --------------------
    @Test
    void changeApplicationStatus_Fail_TargetStatusNotFound() {
        when(applicationRepository.findApplicationById(appId)).thenReturn(Mono.just(appPending));
        when(statusRepository.getStatusByDescription("Pendiente de revisión")).thenReturn(Mono.just(pendingStatus));
        when(statusRepository.getStatusByDescription("Aprobado")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.changeApplicationStatus(appId, "Aprobado"))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    // -------------------- la solicitud ya no está en 'Pendiente' --------------------
    @Test
    void changeApplicationStatus_Fail_NotInPending() {
        Application appNotPending = appPending.toBuilder().idStatus(approvedId).build();

        when(applicationRepository.findApplicationById(appId)).thenReturn(Mono.just(appNotPending));
        when(statusRepository.getStatusByDescription("Pendiente de revisión")).thenReturn(Mono.just(pendingStatus));
        when(statusRepository.getStatusByDescription("Aprobado")).thenReturn(Mono.just(approvedStatus));

        StepVerifier.create(useCase.changeApplicationStatus(appId, "Aprobado"))
                .expectError(BusinessRuleViolationException.class)
                .verify();
    }

    // -------------------- conflicto: updateStatus devuelve 0 filas --------------------
    @Test
    void changeApplicationStatus_Fail_UpdateConflict() {
        when(applicationRepository.findApplicationById(appId)).thenReturn(Mono.just(appPending));
        when(statusRepository.getStatusByDescription("Pendiente de revisión")).thenReturn(Mono.just(pendingStatus));
        when(statusRepository.getStatusByDescription("Aprobado")).thenReturn(Mono.just(approvedStatus));
        when(applicationRepository.updateStatus(appId, pendingId, approvedId)).thenReturn(Mono.just(0));

        StepVerifier.create(useCase.changeApplicationStatus(appId, "Aprobado"))
                .expectError(BusinessRuleViolationException.class)
                .verify();
    }

    // -------------------- error al guardar en outbox (se propaga) --------------------
    @Test
    void changeApplicationStatus_Fail_OutboxSaveError() {
        when(applicationRepository.findApplicationById(appId))
                .thenReturn(Mono.just(appPending)) // pre-update
                .thenReturn(Mono.just(appPending.toBuilder().idStatus(approvedId).build())); // post-update

        when(statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus()))
                .thenReturn(Mono.just(pendingStatus));

        when(statusRepository.getStatusByDescription(ApplicationStatus.APPROVED.getStatus()))
                .thenReturn(Mono.just(approvedStatus));

        when(applicationRepository.updateStatus(appId, pendingId, approvedId))
                .thenReturn(Mono.just(1));

        // 👇 NECESARIO: el flujo consulta el estado por ID antes de guardar en outbox
        when(statusRepository.getStatusById(approvedId))
                .thenReturn(Mono.just(approvedStatus));

        // Forzamos el fallo al guardar en outbox
        when(outboxRepository.save(any(OutboxEvent.class)))
                .thenReturn(Mono.error(new RuntimeException("DB down")));

        StepVerifier.create(useCase.changeApplicationStatus(appId, ApplicationStatus.APPROVED.getStatus()))
                // cualquiera de las dos aserciones es válida:
                .expectErrorMatches(ex -> ex instanceof RuntimeException && "DB down".equals(ex.getMessage()))
                // .expectErrorMessage("DB down")
                .verify();
    }
}
