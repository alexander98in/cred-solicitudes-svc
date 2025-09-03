package co.com.pragma.solicitud.usecase.application.usecase.application;

import co.com.pragma.solicitud.model.application.Application;
import co.com.pragma.solicitud.model.application.gateways.ApplicationRepository;
import co.com.pragma.solicitud.model.loantype.gateways.LoanTypeRepository;
import co.com.pragma.solicitud.model.status.gateways.StatusRepository;
import co.com.pragma.solicitud.model.user.RemoteUser;
import co.com.pragma.solicitud.model.user.gateways.ExternalUserService;
import co.com.pragma.solicitud.usecase.application.ApplicationUseCaseImpl;
import co.com.pragma.solicitud.usecase.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ApplicationUseCaseImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private LoanTypeRepository loanTypeRepository;

    @Mock
    private ExternalUserService externalUserService;

    private ApplicationUseCaseImpl applicationUseCase;

    @BeforeEach
    void setUp() {
        applicationUseCase = new ApplicationUseCaseImpl(applicationRepository, externalUserService, statusRepository, loanTypeRepository);
    }

    private Application baseApplication() {
        return Application.builder()
                .idApplication(UUID.randomUUID())
                .amount(new BigDecimal("1000000"))
                .term(12)
                .email("test@domain.com")
                .idStatus(UUID.randomUUID())
                .idLoanType(UUID.randomUUID())
                .idUser(UUID.randomUUID())
                .build();
    }

    @Test
    void createApplication_success() {
        // given
        Application application = baseApplication();
        String documentId = "1061811110";

        // Mocking
        when(statusRepository.existsStatusById(application.getIdStatus())).thenReturn(Mono.just(true));
        when(loanTypeRepository.existsLoanTypeById(application.getIdLoanType())).thenReturn(Mono.just(true));
        when(externalUserService.getUserByDocumentId(documentId))
                .thenReturn(Mono.just(new RemoteUser(UUID.randomUUID(), "Juan", "Perez", "juan@domain.com", documentId, "3234703198", new BigDecimal("1500000"), null, "Calle 25", UUID.randomUUID())));

        when(applicationRepository.saveApplication(application)).thenReturn(Mono.just(application));

        // when & then
        StepVerifier.create(applicationUseCase.createApplication(application, documentId))
                .expectNext(application)
                .verifyComplete();

        // Verify interactions
        verify(statusRepository).existsStatusById(application.getIdStatus());
        verify(loanTypeRepository).existsLoanTypeById(application.getIdLoanType());
        verify(externalUserService).getUserByDocumentId(documentId);
        verify(applicationRepository).saveApplication(application);
        verifyNoMoreInteractions(statusRepository, loanTypeRepository, externalUserService, applicationRepository);
    }

    @Test
    void createApplication_loanTypeNotFound() {
        // given
        Application application = baseApplication();
        String documentId = "1061811110";

        // Mocking
        when(statusRepository.existsStatusById(application.getIdStatus())).thenReturn(Mono.just(true));
        when(loanTypeRepository.existsLoanTypeById(application.getIdLoanType())).thenReturn(Mono.just(false));
        when(externalUserService.getUserByDocumentId(documentId))
                .thenReturn(Mono.just(new RemoteUser(UUID.randomUUID(),
                        "Juan", "Perez", "juan.perez@gmail.com" + documentId, "1061811110",
                        "3234704755", new BigDecimal(1011222), LocalDate.now(), "Calle 25 N", UUID.randomUUID())));

        // when & then
        StepVerifier.create(applicationUseCase.createApplication(application, documentId))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
                    assertThat(ex.getMessage()).contains("No existe el tipo de crédito");
                })
                .verify();

        // Verify interactions
        verify(statusRepository).existsStatusById(application.getIdStatus());
        verify(loanTypeRepository).existsLoanTypeById(application.getIdLoanType());
        verifyNoMoreInteractions(statusRepository, loanTypeRepository);
    }

    @Test
    void createApplication_userNotFound() {
        // given
        Application application = baseApplication();
        String documentId = "1061811110";

        // Mocking
        when(statusRepository.existsStatusById(application.getIdStatus())).thenReturn(Mono.just(true));
        when(loanTypeRepository.existsLoanTypeById(application.getIdLoanType())).thenReturn(Mono.just(true));
        when(externalUserService.getUserByDocumentId(documentId))
                .thenReturn(Mono.error(new ResourceNotFoundException("Usuario no encontrado con el documento: " + documentId)));

        // when & then
        StepVerifier.create(applicationUseCase.createApplication(application, documentId))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ResourceNotFoundException.class);
                    assertThat(ex.getMessage()).contains("Usuario no encontrado");
                })
                .verify();

        // Verify interactions
        verify(statusRepository).existsStatusById(application.getIdStatus());
        verify(loanTypeRepository).existsLoanTypeById(application.getIdLoanType());
        verify(externalUserService).getUserByDocumentId(documentId);
        verifyNoMoreInteractions(statusRepository, loanTypeRepository, externalUserService);
    }
}
