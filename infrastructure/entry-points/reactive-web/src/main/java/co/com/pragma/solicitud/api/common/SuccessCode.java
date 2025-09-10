package co.com.pragma.solicitud.api.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessCode {

    CREATED("CRED-0001", "Recurso creado exitosamente"),
    RETRIEVED("CRED-0002", "Recurso obtenido exitosamente"),
    UPDATED("CRED-0003", "Recurso actualizado exitosamente"),
    DELETED("CRED-0004", "Recurso eliminado exitosamente"),
    OPERATION_COMPLETED("CRED-0005", "Operación realizada con éxito");

    private final String code;
    private final String message;
}
