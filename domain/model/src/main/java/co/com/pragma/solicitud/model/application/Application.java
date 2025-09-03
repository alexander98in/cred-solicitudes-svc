package co.com.pragma.solicitud.model.application;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Application {

    private UUID idApplication;
    private BigDecimal amount;
    private Integer term;
    private String email;
    private UUID idStatus;
    private UUID idLoanType;
    private UUID idUser;
}
