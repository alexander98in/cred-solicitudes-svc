package co.com.pragma.solicitud.usecase.application.usecase.application;

import co.com.pragma.solicitud.model.application.Application;
import co.com.pragma.solicitud.model.application.ApplicationDetails;
import co.com.pragma.solicitud.model.application.ApplicationFilter;
import co.com.pragma.solicitud.model.application.PaginatedApplications;
import co.com.pragma.solicitud.model.application.events.ApplicationAutoValidationEvent;
import co.com.pragma.solicitud.model.application.events.ApprovedApplicationSummary;
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
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    private UUID newAppId;
    private LoanType loanTypeAuto; // validationAutomatic = TRUE
    private Application newApp;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        loanTypeId = UUID.randomUUID();
        newAppId = UUID.randomUUID();
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

        approvedStatus = Status.builder()
                .idStatus(UUID.randomUUID())
                .description(ApplicationStatus.APPROVED.getStatus())
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

        loanTypeAuto = LoanType.builder()
                .idLoanType(loanTypeId)
                .name("Préstamo Personal")
                .minAmount(new BigDecimal("500"))
                .maxAmount(new BigDecimal("5000"))
                .interestRate(new BigDecimal("5"))
                .validationAutomatic(true) // <- CLAVE
                .build();

        newApp = Application.builder()
                .amount(new BigDecimal("1000"))
                .term(12)
                .idLoanType(loanTypeId)
                .build();

        // Mocks comunes
        when(statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus()))
                .thenReturn(Mono.just(pendingStatus));
        when(loanTypeRepository.getLoanTypeById(loanTypeId))
                .thenReturn(Mono.just(loanTypeAuto));
        when(externalUserService.getUserByEmail(remoteUser.email()))
                .thenReturn(Mono.just(remoteUser));

        // Simula que el repo retorna la app con ID asignado tras guardar
        when(applicationRepository.saveApplication(any(Application.class)))
                .thenAnswer(inv -> {
                    Application a = inv.getArgument(0);
                    a.setIdApplication(newAppId);
                    return Mono.just(a);
                });

        when(statusRepository.getStatusByDescription(ApplicationStatus.APPROVED.getStatus()))
                .thenReturn(Mono.just(approvedStatus));
    }

    @Test
    void createApplication_AutoValidation_WithApprovedHistory_PublishesOutbox() {
        // 1) Historia aprobada del usuario
        UUID prevApp1Id = UUID.randomUUID();
        UUID prevApp2Id = UUID.randomUUID();

        Application approved1 = Application.builder()
                .idApplication(prevApp1Id)
                .amount(new BigDecimal("2000"))
                .term(10)
                .email(remoteUser.email())
                .idStatus(approvedStatus.getIdStatus())
                .idLoanType(loanTypeId) // mismo tipo para simplificar
                .idUser(userId)
                .build();

        Application approved2 = Application.builder()
                .idApplication(prevApp2Id)
                .amount(new BigDecimal("3500"))
                .term(24)
                .email(remoteUser.email())
                .idStatus(approvedStatus.getIdStatus())
                .idLoanType(loanTypeId)
                .idUser(userId)
                .build();

        when(applicationRepository.findByUserAndStatud(userId, approvedStatus.getIdStatus()))
                .thenReturn(Flux.just(approved1, approved2));

        // loanType por cada approved (aquí mismo)
        when(loanTypeRepository.getLoanTypeById(loanTypeId))
                .thenReturn(Mono.just(loanTypeAuto));

        // outbox OK
        when(outboxRepository.save(any(OutboxEvent.class)))
                .thenReturn(Mono.empty());

        Mono<Application> result = useCase.createApplication(newApp, remoteUser.email());

        // Verifica retorno de la nueva solicitud
        StepVerifier.create(result)
                .expectNextMatches(app ->
                        app.getIdApplication().equals(newAppId) &&
                                app.getEmail().equals(remoteUser.email()) &&
                                app.getIdUser().equals(userId) &&
                                app.getIdLoanType().equals(loanTypeId) &&
                                app.getIdStatus().equals(pendingStatus.getIdStatus()))
                .verifyComplete();

        // Captura el outbox para validar payload
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository, times(1)).save(captor.capture());

        OutboxEvent outbox = captor.getValue();
        assertEquals("ApplicationAutoValidationEvent", outbox.getEventType());
        assertEquals(newAppId, outbox.getAggregateId());
        assertNotNull(outbox.getOccurredAt());
        assertFalse(outbox.isProcessed());

        // Valida payload
        assertTrue(outbox.getPayload() instanceof ApplicationAutoValidationEvent);
        ApplicationAutoValidationEvent evt = (ApplicationAutoValidationEvent) outbox.getPayload();

        // datos solicitud actual
        assertEquals(newAppId, evt.idApplication());
        assertEquals(new BigDecimal("1000"), evt.amount());
        assertEquals(12, evt.term());

        // loan type actual
        assertEquals(loanTypeId, evt.idLoanType());
        assertEquals("Préstamo Personal", evt.loanTypeName());
        assertEquals(new BigDecimal("5"), evt.interestRate());
        assertEquals(new BigDecimal("500"), evt.minAmount());
        assertEquals(new BigDecimal("5000"), evt.maxAmount());

        // user
        assertEquals(userId, evt.idUser());
        assertEquals(remoteUser.email(), evt.emailUser());
        assertEquals("John Doe", evt.fullNameUser());

        // approved history
        assertNotNull(evt.approvedApplicationSummaries());
        assertEquals(2, evt.approvedApplicationSummaries().size());

        // ejemplo: validar primer summary
        ApprovedApplicationSummary s1 = evt.approvedApplicationSummaries().get(0);
        assertEquals(prevApp1Id, s1.idApplication());
        assertEquals(new BigDecimal("2000"), s1.amount());
        assertEquals(10, s1.term());
        assertEquals(new BigDecimal("5"), s1.interestRate());
        assertEquals(new BigDecimal("500"), s1.minAmount());
        assertEquals(new BigDecimal("5000"), s1.maxAmount());
    }

    @Test
    void createApplication_AutoValidation_WithEmptyApprovedHistory_PublishesOutboxWithEmptyList() {
        when(applicationRepository.findByUserAndStatud(userId, approvedStatus.getIdStatus()))
                .thenReturn(Flux.empty());

        when(loanTypeRepository.getLoanTypeById(loanTypeId))
                .thenReturn(Mono.just(loanTypeAuto));

        when(outboxRepository.save(any(OutboxEvent.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.createApplication(newApp, remoteUser.email()))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository, times(1)).save(captor.capture());

        ApplicationAutoValidationEvent evt = (ApplicationAutoValidationEvent) captor.getValue().getPayload();
        assertNotNull(evt);
        assertNotNull(evt.approvedApplicationSummaries());
        assertTrue(evt.approvedApplicationSummaries().isEmpty());
    }

    @Test
    void createApplication_AutoValidation_ApprovedStatusNotFound_Error() {
        // Forzamos que no exista el estado "Aprobada"
        when(statusRepository.getStatusByDescription(ApplicationStatus.APPROVED.getStatus()))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.createApplication(newApp, remoteUser.email()))
                .expectError(ResourceNotFoundException.class)
                .verify();

        // No debería intentar guardar en outbox
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void createApplication_WhenValidationAutomaticFalse_DoesNotPublishOutbox() {
        // Muta el loan type para que NO requiera validación automática
        LoanType loanTypeManual = loanTypeAuto.toBuilder().validationAutomatic(false).build();
        when(loanTypeRepository.getLoanTypeById(loanTypeId)).thenReturn(Mono.just(loanTypeManual));

        StepVerifier.create(useCase.createApplication(newApp, remoteUser.email()))
                .expectNextCount(1)
                .verifyComplete();

        verify(outboxRepository, never()).save(any());
        verify(applicationRepository, never()).findByUserAndStatud(any(), any());
    }

    @Test
    void createApplication_AutoValidation_OutboxSaveError_Propagates() {
        when(applicationRepository.findByUserAndStatud(userId, approvedStatus.getIdStatus()))
                .thenReturn(Flux.empty());

        when(outboxRepository.save(any(OutboxEvent.class)))
                .thenReturn(Mono.error(new RuntimeException("DB down")));

        StepVerifier.create(useCase.createApplication(newApp, remoteUser.email()))
                .expectErrorMessage("DB down")
                .verify();
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
        // === Arrange ===
        // IDs consistentes para toda la prueba
        UUID appId       = UUID.randomUUID();
        UUID pendingId   = UUID.randomUUID();
        UUID approvedId  = UUID.randomUUID();

        // Application en estado "Pendiente"
        Application appPending = Application.builder()
                .idApplication(appId)
                .idStatus(pendingId)
                .email("john.doe@email.com")
                .amount(new BigDecimal("1500"))
                .term(12)
                .build();

        // Status coherentes con esos IDs
        Status pendingStatus  = Status.builder().idStatus(pendingId).description(ApplicationStatus.PENDING.getStatus()).build();
        Status approvedStatus = Status.builder().idStatus(approvedId).description(ApplicationStatus.APPROVED.getStatus()).build();

        // 1) Buscar app actual (pendiente) y luego con estado actualizado a "Aprobado"
        when(applicationRepository.findApplicationById(appId))
                .thenReturn(Mono.just(appPending)) // pre-update
                .thenReturn(Mono.just(appPending.toBuilder().idStatus(approvedId).build())); // post-update

        // 2) Id del estado 'Pendiente'
        when(statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus()))
                .thenReturn(Mono.just(pendingStatus));

        // 3) Id del estado destino 'Aprobado'
        when(statusRepository.getStatusByDescription(ApplicationStatus.APPROVED.getStatus()))
                .thenReturn(Mono.just(approvedStatus));

        // 4) updateStatus exitoso (1 fila)
        //    Usamos thenAnswer para ver/validar exactamente qué llega al mock.
        when(applicationRepository.updateStatus(any(UUID.class), any(UUID.class), any(UUID.class)))
                .thenAnswer(inv -> {
                    UUID a0 = inv.getArgument(0); // idApp
                    UUID a1 = inv.getArgument(1); // currentStatus
                    UUID a2 = inv.getArgument(2); // newStatus

                    System.out.println("updateStatus args => appId=" + a0 + " pending=" + a1 + " target=" + a2);

                    // Aserciones para depurar si algo no cuadra:
                    org.junit.jupiter.api.Assertions.assertEquals(appId,     a0, "appId distinto");
                    org.junit.jupiter.api.Assertions.assertEquals(pendingId, a1, "pendingId distinto");
                    org.junit.jupiter.api.Assertions.assertEquals(approvedId,a2, "approvedId distinto");

                    return Mono.just(1);
                });

        // 5) guardar en outbox OK (no bloquea el flujo)
        when(outboxRepository.save(any(OutboxEvent.class))).thenReturn(Mono.empty());

        // 6) Al final el caso de uso hace getStatusById(newStatusId) para armar ApplicationDetails
        when(statusRepository.getStatusById(approvedId))
                .thenReturn(Mono.just(approvedStatus));

        // === Act & Assert ===
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
        when(statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus())).thenReturn(Mono.just(pendingStatus));
        when(statusRepository.getStatusByDescription(ApplicationStatus.APPROVED.getStatus())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.changeApplicationStatus(appId, ApplicationStatus.APPROVED.getStatus()))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    // -------------------- la solicitud ya no está en 'Pendiente' --------------------
    @Test
    void changeApplicationStatus_Fail_NotInPending() {
        Application appNotPending = appPending.toBuilder().idStatus(approvedId).build();

        when(applicationRepository.findApplicationById(appId)).thenReturn(Mono.just(appNotPending));
        when(statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus())).thenReturn(Mono.just(pendingStatus));
        when(statusRepository.getStatusByDescription(ApplicationStatus.APPROVED.getStatus())).thenReturn(Mono.just(approvedStatus));

        StepVerifier.create(useCase.changeApplicationStatus(appId, ApplicationStatus.APPROVED.getStatus()))
                .expectError(BusinessRuleViolationException.class)
                .verify();
    }

    // -------------------- conflicto: updateStatus devuelve 0 filas --------------------
    @Test
    void changeApplicationStatus_Fail_UpdateConflict() {
        // IDs consistentes
        UUID appId      = UUID.randomUUID();
        UUID pendingId  = UUID.randomUUID();
        UUID approvedId = UUID.randomUUID();

        // App en pendiente (debe tener pendingId)
        Application appPending = Application.builder()
                .idApplication(appId)
                .idStatus(pendingId)
                .email("john.doe@email.com")
                .amount(new BigDecimal("1500"))
                .term(12)
                .build();

        Status pendingStatus  = Status.builder()
                .idStatus(pendingId)
                .description(ApplicationStatus.PENDING.getStatus())
                .build();

        Status approvedStatus = Status.builder()
                .idStatus(approvedId)
                .description(ApplicationStatus.APPROVED.getStatus())
                .build();

        // 1) app encontrada (pre-update)
        when(applicationRepository.findApplicationById(appId))
                .thenReturn(Mono.just(appPending));

        // 2) id de 'Pendiente'
        when(statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus()))
                .thenReturn(Mono.just(pendingStatus));

        // 3) id de 'Aprobado'
        when(statusRepository.getStatusByDescription(ApplicationStatus.APPROVED.getStatus()))
                .thenReturn(Mono.just(approvedStatus));

        // 4) Conflicto: 0 filas actualizadas (usar eq para que matchee exacto)
        when(applicationRepository.updateStatus(eq(appId), eq(pendingId), eq(approvedId)))
                .thenReturn(Mono.just(0));

        // Ejecutar y verificar que emite BusinessRuleViolationException
        StepVerifier.create(useCase.changeApplicationStatus(appId, ApplicationStatus.APPROVED.getStatus()))
                .expectError(BusinessRuleViolationException.class)
                .verify();
    }

    // -------------------- error al guardar en outbox (se propaga) --------------------
    @Test
    void changeApplicationStatus_Fail_OutboxSaveError() {
        // Asegura IDs consistentes en la app base
        appPending = appPending.toBuilder()
                .idApplication(appId)
                .idStatus(pendingId)
                .build();

        // 1) pre y post update
        when(applicationRepository.findApplicationById(appId))
                .thenReturn(Mono.just(appPending))
                .thenReturn(Mono.just(appPending.toBuilder().idStatus(approvedId).build()));

        // 2) estado 'Pendiente'
        when(statusRepository.getStatusByDescription(ApplicationStatus.PENDING.getStatus()))
                .thenReturn(Mono.just(pendingStatus));

        // 3) estado destino 'Aprobado'
        when(statusRepository.getStatusByDescription(ApplicationStatus.APPROVED.getStatus()))
                .thenReturn(Mono.just(approvedStatus));

        // 4) fallback (por si no matchea exacto) + stub específico con eq(...)
        when(applicationRepository.updateStatus(any(UUID.class), any(UUID.class), any(UUID.class)))
                .thenReturn(Mono.just(1));
        when(applicationRepository.updateStatus(eq(appId), eq(pendingId), eq(approvedId)))
                .thenReturn(Mono.just(1));

        // 5) después del update, se consulta el status por ID
        when(statusRepository.getStatusById(approvedId))
                .thenReturn(Mono.just(approvedStatus));

        // 6) forzamos el fallo al guardar en outbox
        when(outboxRepository.save(any(OutboxEvent.class)))
                .thenReturn(Mono.error(new RuntimeException("DB down")));

        StepVerifier.create(useCase.changeApplicationStatus(appId, ApplicationStatus.APPROVED.getStatus()))
                .expectErrorMatches(ex -> ex instanceof RuntimeException && "DB down".equals(ex.getMessage()))
                .verify();
    }
}
