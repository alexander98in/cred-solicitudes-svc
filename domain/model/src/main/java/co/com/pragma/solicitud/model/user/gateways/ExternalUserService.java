package co.com.pragma.solicitud.model.user.gateways;

import co.com.pragma.solicitud.model.user.RemoteUser;
import reactor.core.publisher.Mono;

public interface ExternalUserService {
    Mono<RemoteUser> getUserByEmail(String email);
}
