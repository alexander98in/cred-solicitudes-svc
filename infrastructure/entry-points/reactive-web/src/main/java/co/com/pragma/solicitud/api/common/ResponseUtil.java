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
        var api = ApiResponse.of(HttpStatus.CREATED.value(), message, body, req.path());
        return ServerResponse.created(location)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(api);
    }

    public static <T> Mono<ServerResponse> ok(ServerRequest req, String message, T body) {
        var api = ApiResponse.of(HttpStatus.OK.value(), message, body, req.path());
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(api);
    }

    public static Mono<ServerResponse> error(ServerRequest req, HttpStatus status, String message, Object details) {
        var api = ApiResponse.of(status.value(), message, details, req.path());
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(api);
    }
}
