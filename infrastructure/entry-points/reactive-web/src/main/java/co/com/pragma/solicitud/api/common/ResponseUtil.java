package co.com.pragma.solicitud.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

public final class ResponseUtil {

    private ResponseUtil() {}

    public static <T> Mono<ServerResponse> created(ServerRequest req, URI location, String message, T body) {
        var api = ApiResponse.of(SuccessCode.CREATED.getCode(), message, body, req.path());
        return ServerResponse.created(location)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(api);
    }

    public static <T> Mono<ServerResponse> ok(ServerRequest req, String message, T body) {
        var api = ApiResponse.of(SuccessCode.OPERATION_COMPLETED.getCode(), message, body, req.path());
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(api);
    }

    public static Mono<ServerResponse> error(ServerRequest req, String status, HttpStatus httpStatus, String message, Object details) {
        var api = ApiResponse.of(status, message, details, req.path());
        return ServerResponse.status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(api);
    }

    public static Mono<ServerResponse> badRequest(ServerRequest req, String message, Object details) {
        return error(req, ErrorCode.BAD_REQUEST.getCode(), HttpStatus.BAD_REQUEST, message, details);
    }
}
