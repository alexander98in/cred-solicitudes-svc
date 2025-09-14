package co.com.pragma.solicitud.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequestDTO(
        @Schema(example = "d5f3c1a2-3e4b-4c5d-9f6a-7b8c9d0e1f2a")
        @NotNull(message = "El id de la solicitud es requerido")
        String idApplication,

        @Schema(example = "Aprobada")
        @NotBlank(message = "El estado destino es requerido")
        String targetStatus
) {}


