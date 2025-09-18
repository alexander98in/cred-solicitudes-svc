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
        var status = pair.httpStatus();
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

        var resp = exchange.getResponse();
        resp.setStatusCode(status);
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            var bytes = objectMapper.writeValueAsBytes(body);
            var buffer = resp.bufferFactory().wrap(bytes);
            return resp.writeWith(Mono.just(buffer));
        } catch (Exception writeErr) {
            // fallback mínimo si falla la serialización
            var fallback = ("{\"status\":" + status.value() + ",\"message\":\"Unexpected error\"}")
                    .getBytes(StandardCharsets.UTF_8);
            return resp.writeWith(Mono.just(resp.bufferFactory().wrap(fallback)));
        }
    }

    private record StatusAndBody(HttpStatus httpStatus, ApiResponse<?> body) {}

    private StatusAndBody toStatusAndBody(ServerWebExchange ex, Throwable error) {
        String status;
        HttpStatus httpStatus;
        String code = ErrorCode.UNKNOWN_ERROR.getMessage();
        String message = error.getMessage();
        String messageGeneral = ErrorCode.UNKNOWN_ERROR.getMessage();
        Object details = null;

        if (error instanceof RequestValidationException ve) {
            httpStatus = HttpStatus.BAD_REQUEST;
            status = ErrorCode.VALIDATION.getCode();
            messageGeneral = ErrorCode.VALIDATION.getMessage();
            code = ErrorCode.VALIDATION.getCode();
            details = ve.getErrors();
            message = (message != null) ? message : "Datos inválidos";
        } else if (error instanceof ResourceAlreadyExistsException) {
            httpStatus = HttpStatus.CONFLICT;
            status = ErrorCode.CONFLICT.getCode();
            messageGeneral = ErrorCode.CONFLICT.getMessage();
            code = ((ResourceAlreadyExistsException) error).getCode();
        } else if (error instanceof ResourceNotFoundException) {
            status = ErrorCode.NOT_FOUND.getCode();
            httpStatus = HttpStatus.NOT_FOUND;
            messageGeneral = ErrorCode.NOT_FOUND.getMessage();
            code = ((ResourceNotFoundException) error).getCode();
        } else if (error instanceof BusinessRuleViolationException) {
            httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;
            status = ErrorCode.BUSINESS_RULE.getCode();
            messageGeneral = ErrorCode.BUSINESS_RULE.getMessage();
            code = ((BusinessRuleViolationException) error).getCode();
        } else if (error instanceof IllegalArgumentException) {
            httpStatus = HttpStatus.BAD_REQUEST;
            status = ErrorCode.VALIDATION.getCode();
            messageGeneral = ErrorCode.VALIDATION.getMessage();
            code = ErrorCode.VALIDATION.getCode();
        } else if (error instanceof IllegalStateException || error instanceof DuplicateKeyException) {
            httpStatus = HttpStatus.CONFLICT;
            status = ErrorCode.CONFLICT.getCode();
            messageGeneral = ErrorCode.CONFLICT.getMessage();
            code = ErrorCode.CONFLICT.getCode();
        } else if (error instanceof NoSuchElementException
                || error instanceof ResponseStatusException rse && rse.getStatusCode().is4xxClientError()) {
            httpStatus = HttpStatus.NOT_FOUND;
            status = ErrorCode.NOT_FOUND.getCode();
            messageGeneral = ErrorCode.NOT_FOUND.getMessage();
            code = ErrorCode.NOT_FOUND.getCode();
            if (error instanceof ResponseStatusException rse2) message = rse2.getReason();
        } else if (error instanceof ServerWebInputException || error instanceof DecodingException) {
            httpStatus = HttpStatus.BAD_REQUEST;
            status = ErrorCode.PAYLOAD_INVALID.getCode();
            messageGeneral = ErrorCode.PAYLOAD_INVALID.getMessage();
            code = ErrorCode.PAYLOAD_INVALID.getCode();
            if (message == null) message = "JSON de entrada inválido";
        } else {
            if (error instanceof BadSqlGrammarException) {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                status = ErrorCode.DATA_ACCESS.getCode();
                messageGeneral = ErrorCode.DATA_ACCESS.getMessage();
                message = ErrorCode.DATA_ACCESS.getMessage();
                code = ErrorCode.DATA_ACCESS.getCode();
            } else {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
                status = ErrorCode.SERVER_ERROR.getCode();
                messageGeneral = ErrorCode.SERVER_ERROR.getMessage();
                message = ErrorCode.SERVER_ERROR.getMessage();
                code = ErrorCode.SERVER_ERROR.getCode();
                if (message == null) message = "Error interno del servidor";
            }
        }

        var req = ex.getRequest();
        var err = ErrorResponse.builder()
                .errorCode(code)
                .message(message)
                .url(req.getURI().getPath())
                .method(req.getMethod() != null ? req.getMethod().name() : null)
                .data(details)
                .build();
        //status.is4xxClientError() ? "Solicitud inválida" : "Error interno"
        var api = ApiResponse.of(
                status,
                messageGeneral,
                err,
                req.getURI().getPath()
        );
        //return new StatusAndBody(status, api);
        return new StatusAndBody(httpStatus, api);
    }
}
