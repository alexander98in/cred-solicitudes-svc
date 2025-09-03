package co.com.pragma.solicitud.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table(name = "solicitudes")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ApplicationEntity {

    @Id
    @Column("id_solicitud")
    private UUID idApplication;

    @Column("monto")
    private BigDecimal amount;

    @Column("plazo")
    private Integer term;

    @Column("email")
    private String email;

    @Column("id_estado")
    private UUID idStatus;

    @Column("id_tipo_prestamo")
    private UUID idLoanType;

    @Column("id_usuario")
    private UUID idUser;
}
