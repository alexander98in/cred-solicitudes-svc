package co.com.pragma.solicitud.model.user;
//import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RemoteUser(
        UUID id,
        String name,
        String lastName,
        String email,
        String documentId,
        String phone,
        BigDecimal salary,
        LocalDate birthDate,
        String address,
        UUID idRol
) {}