package co.com.pragma.solicitud.api.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    GENERIC("CRED-1000", "Error generico"),
    VALIDATION("CRED-2000", "Error de validacion"),
    CONFLICT("CRED-3000", "Conflicto de datos"),
    NOT_FOUND("CRED-4000", "Recurso no encontrado"),
    PAYLOAD_INVALID("CRED-5000", "Payload invalido"),
    BUSINESS_RULE("CRED-6000", "Violacion de regla de negocio"),
    INVALID_CREDENTIALS("CRED-7000", "Las credenciales proporcionadas no son validas"),
    UNAUTHORIZED_ACTION("CRED-7001", "El usuario no tiene permisos para realizar esta accion"),
    SERVER_ERROR("CRED-8000", "Error interno del servidor"),
    UNKNOWN_ERROR("CRED-9000", "Ocurrio un error desconocido"),
    DATA_ACCESS("CRED-10000", "Error de acceso a datos"),
    BAD_REQUEST("CRED-11000", "Solicitud incorrecta");

    private final String code;
    private final String message;
}
