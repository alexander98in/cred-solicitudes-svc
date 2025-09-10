package co.com.pragma.solicitud.api.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    GENERIC("CRED-1000", "Error genérico"),
    VALIDATION("CRED-2000", "Error de validación"),
    CONFLICT("CRED-3000", "Conflicto de datos"),
    NOT_FOUND("CRED-4000", "Recurso no encontrado"),
    PAYLOAD_INVALID("CRED-5000", "Payload inválido"),
    BUSINESS_RULE("CRED-6000", "Violación de regla de negocio"),
    INVALID_CREDENTIALS("CRED-7000", "Las credenciales proporcionadas no son válidas"),
    UNAUTHORIZED_ACTION("CRED-7001", "El usuario no tiene permisos para realizar esta acción"),
    SERVER_ERROR("CRED-8000", "Error interno del servidor"),
    UNKNOWN_ERROR("CRED-9000", "Ocurrió un error desconocido"),
    DATA_ACCESS("CRED-10000", "Error de acceso a datos");

    private final String code;
    private final String message;
}
