package co.com.pragma.solicitud.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplicationUpdateResponseDTO(
        UUID id,
        BigDecimal amount,
        Integer term,
        String email,
        String status
) {
}
