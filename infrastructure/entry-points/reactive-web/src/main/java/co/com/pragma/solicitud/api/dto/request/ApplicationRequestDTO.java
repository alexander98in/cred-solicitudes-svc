package co.com.pragma.solicitud.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ApplicationRequestDTO(

    @Schema(example = "1500000")
    @NotNull(message = "{application.amount.required}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{application.amount.min}")
    BigDecimal amount,

    @Schema(example = "12")
    @NotNull(message = "{application.term.required}")
    @DecimalMin(value = "1", inclusive = true, message = "{application.term.min}")
    Integer term,

    @Schema(example = "d5f3c1a2-3e4b-4c5d-9f6a-7b8c9d0e1f2a")
    @NotBlank(message = "{application.loan_type.required}")
    String idLoanType
) {
}
