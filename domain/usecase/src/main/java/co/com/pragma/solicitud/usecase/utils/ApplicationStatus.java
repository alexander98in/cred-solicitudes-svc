package co.com.pragma.solicitud.usecase.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApplicationStatus {

    PENDING("Pendiente de revisión"),
    APPROVED("Aprobado"),
    REJECTED("Rechazado");

    private final String status;
}
