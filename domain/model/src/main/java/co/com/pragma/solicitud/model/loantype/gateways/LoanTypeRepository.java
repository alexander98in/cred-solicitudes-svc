package co.com.pragma.solicitud.model.loantype.gateways;

import co.com.pragma.solicitud.model.loantype.LoanType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface LoanTypeRepository {
    Mono<LoanType> findLoanTypeById(UUID id);
    Flux<LoanType> findLoanTypeAll();
    Mono<Boolean> existsLoanTypeById(UUID id);
}
