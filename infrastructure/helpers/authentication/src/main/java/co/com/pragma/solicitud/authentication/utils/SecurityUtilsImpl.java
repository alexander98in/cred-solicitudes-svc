package co.com.pragma.solicitud.authentication.utils;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SecurityUtilsImpl implements SecurityUtils {

    @Override
    public Mono<String> getEmailFromContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(auth -> (String) auth.getPrincipal())
                .switchIfEmpty(Mono.error(new RuntimeException("No se pudo obtener el email")));
    }
}
