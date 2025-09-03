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

    @Schema(example = "juan.perez@correo.com")
    @NotBlank(message = "{application.email.required}")
    @Size(min=8, max=150, message = "{application.email.size}")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "{application.email.invalid}")
    String email,

    @Schema(example = "be066aae-0556-47c8-9e4b-e2f3b5c63f08")
    @NotBlank(message = "{application.status.required}")
    String idStatus,

    @Schema(example = "d5f3c1a2-3e4b-4c5d-9f6a-7b8c9d0e1f2a")
    @NotBlank(message = "{application.loan_type.required}")
    String idLoanType,

    @Schema(example = "1061811110")
    @NotBlank(message = "{application.user_id.required}")
    String documentId
) {
}
