package co.com.pragma.solicitud.r2dbc.client;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiResponse<T>(
        int httpStatus,
        String message,
        T data,
        String path,
        @JsonInclude(JsonInclude.Include.NON_NULL) String timestamp
) {}
