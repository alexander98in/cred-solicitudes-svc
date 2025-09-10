package co.com.pragma.solicitud.usecase.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeDomain {

    AMOUNT_MINIMUM("CRED-6001", "El monto es menor al mínimo permitido"),
    AMOUNT_MAXIMUM("CRED-6002", "El monto es mayor al máximo permitido"),
    PENDING_STATUS_NOT_FOUND("CRED-4001", "Estado 'Pendiente de revisión' no encontrado"),
    LOAN_TYPE_NOT_FOUND("CRED-4002", "El tipo de prestamo con ID %s no existe"),
    USER_NOT_FOUND_WITH_EMAIL("CRED-4003", "El usuario con email %s no existe");

    private final String code;
    private final String message;
}
