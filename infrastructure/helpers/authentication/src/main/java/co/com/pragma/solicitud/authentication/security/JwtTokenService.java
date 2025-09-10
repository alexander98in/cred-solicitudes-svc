package co.com.pragma.solicitud.authentication.security;

import co.com.pragma.solicitud.model.auth.TokenVerification;
import co.com.pragma.solicitud.model.auth.gateways.TokenService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

@Component
public class JwtTokenService implements TokenService {

    private final SecretKey key;

    public JwtTokenService(@Value("${security.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public Mono<TokenVerification> verify(String token) {
        try {
            var jws = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setAllowedClockSkewSeconds(30)
                    .build()
                    .parseClaimsJws(token);

            var subject = jws.getBody().getSubject();
            @SuppressWarnings("unchecked")
            var authorities = (List<String>) jws.getBody().get("authorities", List.class);
            return Mono.just(new TokenVerification(subject, authorities, true));
        } catch (JwtException e) {
            return Mono.just(new TokenVerification(null, List.of(), false));
        }
    }

    @Override
    public Mono<String> getEmailFromContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(auth -> (String) auth.getPrincipal())
                .switchIfEmpty(Mono.error(new RuntimeException("No se pudo obtener el email")));
    }
}
