package co.com.pragma.solicitud.usecase.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeDomain {

    AMOUNT_MINIMUM("CRED-6001", "El monto es menor al minimo permitido"),
    AMOUNT_MAXIMUM("CRED-6002", "El monto es mayor al maximo permitido"),
    STATUS_OBLIGATORY("CRED-6003", "El estado a actualizar es obligatorio"),
    STATUS_NOT_VALID("CRED-6004", "Solo se permite actualizar a los estados 'Aprobado' o 'Rechazado'"),
    APPLICATION_CHANGE_STATUS("CRED-6005", "Solo se pueden actualizar solicitudes en estado 'Pendiente de revision'"),
    APPLICATION_CONFLICT_CHANGE_STATUS("CRED-6006", "La solicitud cambio de estado por otro proceso, intente de nuevo"),
    PENDING_STATUS_NOT_FOUND("CRED-4001", "Estado 'Pendiente de revision' no encontrado"),
    LOAN_TYPE_NOT_FOUND("CRED-4002", "El tipo de prestamo con ID %s no existe"),
    USER_NOT_FOUND_WITH_EMAIL("CRED-4003", "El usuario con email %s no existe"),
    APPLICATION_NOT_FOUND("CRED-4004", "La solicitud con ID %s no existe"),
    STATUS_NOT_FOUND("CRED-4005", "El estado destino: %s no existe");

    private final String code;
    private final String message;
}
