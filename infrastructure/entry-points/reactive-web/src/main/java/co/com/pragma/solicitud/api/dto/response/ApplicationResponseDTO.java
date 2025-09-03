package co.com.pragma.solicitud.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplicationResponseDTO(
        UUID idApplication,
        BigDecimal amount,
        Integer term,
        String email,
        UUID idStatus,
        UUID idLoanType,
        UUID idUser
) {
}
