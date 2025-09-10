package co.com.pragma.solicitud.model.application;

import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationFilter {

    private String email;
    private Integer term;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String loanTypeName;
    private String statusDescription;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private int page = 0;
    private int size = 10;
}
