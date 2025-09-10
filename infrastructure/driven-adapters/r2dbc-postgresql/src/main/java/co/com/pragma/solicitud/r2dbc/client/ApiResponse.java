package co.com.pragma.solicitud.r2dbc.client;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
        int Code,
        String message,
        T data,
        String path,
        @JsonInclude(JsonInclude.Include.NON_NULL) String timestamp
) {}
