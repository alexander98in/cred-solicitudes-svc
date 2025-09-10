package co.com.pragma.solicitud.api.exceptions;

import co.com.pragma.solicitud.api.common.ApiResponse;
import co.com.pragma.solicitud.api.common.ErrorCode;
import co.com.pragma.solicitud.api.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper mapper;

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        var status = HttpStatus.UNAUTHORIZED; // 401
        var req = exchange.getRequest();
        var method = (req.getMethod() != null) ? req.getMethod().name() : null;

        var err = ErrorResponse.builder()
                .errorCode(ErrorCode.INVALID_CREDENTIALS.getCode()) // agrega en tu enum si quieres
                .message(ErrorCode.INVALID_CREDENTIALS.getMessage())
                .url(req.getURI().getPath())
                .method(method)
                .build();

        var body = ApiResponse.of(
                ErrorCode.INVALID_CREDENTIALS.getCode(),
                ErrorCode.INVALID_CREDENTIALS.getMessage(),
                err,
                req.getURI().getPath()
        );

        var resp = exchange.getResponse();
        resp.setStatusCode(status);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            var bytes = mapper.writeValueAsBytes(body);
            return resp.writeWith(Mono.just(resp.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            return resp.setComplete();
        }
    }
}
