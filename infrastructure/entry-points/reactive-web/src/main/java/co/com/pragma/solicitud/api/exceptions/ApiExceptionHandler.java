package co.com.pragma.solicitud.api.exceptions;

import co.com.pragma.solicitud.api.common.ApiResponse;
import co.com.pragma.solicitud.api.common.ErrorCode;
import co.com.pragma.solicitud.api.common.ErrorResponse;
import co.com.pragma.solicitud.usecase.exceptions.BusinessRuleViolationException;
import co.com.pragma.solicitud.usecase.exceptions.ResourceAlreadyExistsException;
import co.com.pragma.solicitud.usecase.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.DecodingException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ApiExceptionHandler implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        var pair = toStatusAndBody(exchange, ex);
        var status = pair.status();
        var body = pair.body();

        // --- LOG: una línea compacta por error ---
        var req = exchange.getRequest();
        var method = req.getMethod() != null ? req.getMethod().name() : "?";
        var path = req.getURI().getPath();
        var exName = ex.getClass().getSimpleName();
        var reqId = exchange.getLogPrefix().trim(); // ej: [efbcac04-1] si está disponible

        if (status.is5xxServerError()) {
            // 5xx: log con stacktrace
            log.error("{} {} {} -> {} {}", reqId, method, path, status.value(), exName, ex);
        } else {
            // 4xx: sin stacktrace (ruido), solo warn
            log.warn("{} {} {} -> {} {} : {}", reqId, method, path, status.value(), exName, ex.getMessage());
        }
        // -----------------------------------------

        var resp = exchange.getResponse();
        resp.setStatusCode(status);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            var bytes = objectMapper.writeValueAsBytes(body);
            var buffer = resp.bufferFactory().wrap(bytes);
            return resp.writeWith(Mono.just(buffer));
        } catch (Exception writeErr) {
            // fallback mínimo si falla la serialización
            var fallback = ("{\"httpStatus\":" + status.value() + ",\"message\":\"Unexpected error\"}")
                    .getBytes(StandardCharsets.UTF_8);
            return resp.writeWith(Mono.just(resp.bufferFactory().wrap(fallback)));
        }
    }

    private record StatusAndBody(HttpStatus status, ApiResponse<?> body) {}

    private StatusAndBody toStatusAndBody(ServerWebExchange ex, Throwable error) {
        HttpStatus status;
        String code = ErrorCode.GENERIC.getCode();
        String message = error.getMessage();
        Object details = null;

        if (error instanceof RequestValidationException ve) {
            status = HttpStatus.BAD_REQUEST;
            code = ErrorCode.VALIDATION.getCode();
            details = ve.getErrors();
            message = (message != null) ? message : "Datos inválidos";
        } else if (error instanceof ResourceAlreadyExistsException) {
            status = HttpStatus.CONFLICT;
            code = ErrorCode.CONFLICT.getCode();
        } else if (error instanceof ResourceNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            code = ErrorCode.NOT_FOUND.getCode();
        } else if (error instanceof BusinessRuleViolationException) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
            code = ErrorCode.BUSINESS_RULE.getCode();
        } else if (error instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            code = ErrorCode.VALIDATION.getCode();
        } else if (error instanceof IllegalStateException || error instanceof DuplicateKeyException) {
            status = HttpStatus.CONFLICT;
            code = ErrorCode.CONFLICT.getCode();
        } else if (error instanceof NoSuchElementException
                || error instanceof ResponseStatusException rse && rse.getStatusCode().is4xxClientError()) {
            status = HttpStatus.NOT_FOUND;
            code = ErrorCode.NOT_FOUND.getCode();
            if (error instanceof ResponseStatusException rse2) message = rse2.getReason();
        } else if (error instanceof ServerWebInputException || error instanceof DecodingException) {
            status = HttpStatus.BAD_REQUEST;
            code = ErrorCode.PAYLOAD_INVALID.getCode();
            if (message == null) message = "JSON de entrada inválido";
        } else {
            if (error instanceof BadSqlGrammarException) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                message = "Error de acceso a datos";
            } else {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                if (message == null) message = "Error interno del servidor";
            }
        }

        var req = ex.getRequest();
        var err = ErrorResponse.builder()
                .errorCode(code)
                .message(message)
                .httpStatus(status.value())
                .url(req.getURI().getPath())
                .method(req.getMethod() != null ? req.getMethod().name() : null)
                .data(details)
                .build();

        var api = ApiResponse.of(
                status.value(),
                (status.is4xxClientError() ? "Solicitud inválida" : "Error interno"),
                err,
                req.getURI().getPath()
        );

        return new StatusAndBody(status, api);
    }
}
