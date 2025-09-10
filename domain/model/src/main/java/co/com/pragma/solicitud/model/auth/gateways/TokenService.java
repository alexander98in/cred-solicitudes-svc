package co.com.pragma.solicitud.model.auth.gateways;

import co.com.pragma.solicitud.model.auth.TokenVerification;
import reactor.core.publisher.Mono;

public interface TokenService {

    Mono<TokenVerification> verify(String token);

    Mono<String> getEmailFromContext();
}
