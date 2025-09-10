package co.com.pragma.solicitud.api.exceptions;

import co.com.pragma.solicitud.api.common.ApiResponse;
import co.com.pragma.solicitud.api.common.ErrorCode;
import co.com.pragma.solicitud.api.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements ServerAccessDeniedHandler {
    private final ObjectMapper mapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException ex) {
        var status = HttpStatus.FORBIDDEN; // 403
        var req = exchange.getRequest();
        var method = (req.getMethod() != null) ? req.getMethod().name() : null;

        var err = ErrorResponse.builder()
                .errorCode(ErrorCode.UNAUTHORIZED_ACTION.getCode())
                .message(ErrorCode.UNAUTHORIZED_ACTION.getMessage())
                .url(req.getURI().getPath())
                .method(method)
                .build();

        var body = ApiResponse.of(
                ErrorCode.UNAUTHORIZED_ACTION.getCode(),
                ErrorCode.UNAUTHORIZED_ACTION.getMessage(),
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
