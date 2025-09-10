package co.com.pragma.solicitud.authentication.utils;

import reactor.core.publisher.Mono;

public interface SecurityUtils {

    Mono<String> getEmailFromContext();
}
