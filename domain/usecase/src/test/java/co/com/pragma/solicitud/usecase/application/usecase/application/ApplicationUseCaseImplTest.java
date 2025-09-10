package co.com.pragma.solicitud.usecase.application.usecase.application;

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
import co.com.pragma.solicitud.usecase.application.ApplicationUseCaseImpl;
import co.com.pragma.solicitud.usecase.exceptions.BusinessRuleViolationException;
import co.com.pragma.solicitud.usecase.exceptions.ResourceNotFoundException;
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

    @InjectMocks
    private ApplicationUseCaseImpl useCase;

    private UUID userId;
    private UUID loanTypeId;
    private UUID statusId;
    private Application application;
    private RemoteUser remoteUser;
    private LoanType loanType;
    private Status status;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        userId = UUID.randomUUID();
        loanTypeId = UUID.randomUUID();
        statusId = UUID.randomUUID();

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
}
