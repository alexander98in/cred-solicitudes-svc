package co.com.pragma.solicitud.model.application;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDetails {

    private UUID id;
    private BigDecimal amount;
    private Integer term;
    private String email;
    private String fullName;
    private String loanTypeName;
    private BigDecimal interestRate;
    private String status;
    private BigDecimal salary;
    private BigDecimal monthlyDebt;
    private BigDecimal monthlyInstallment;
}
