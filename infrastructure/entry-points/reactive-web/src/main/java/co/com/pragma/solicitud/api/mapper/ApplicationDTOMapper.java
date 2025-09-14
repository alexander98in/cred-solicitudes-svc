package co.com.pragma.solicitud.api.mapper;

import co.com.pragma.solicitud.api.dto.request.ApplicationRequestDTO;
import co.com.pragma.solicitud.api.dto.response.ApplicationResponseDTO;
import co.com.pragma.solicitud.api.dto.response.ApplicationUpdateResponseDTO;
import co.com.pragma.solicitud.model.application.Application;
import co.com.pragma.solicitud.model.application.ApplicationDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ApplicationDTOMapper {

    ApplicationResponseDTO toResponse(Application app);

    ApplicationUpdateResponseDTO toUpdateResponse(ApplicationDetails app);

    @Named("strToUuid")
    static UUID strToUuid(String v) {
        return (v == null || v.isBlank()) ? null : UUID.fromString(v);
    }

    // Request DTO + userId -> Dominio
    @Mapping(target = "idApplication", ignore = true)
    @Mapping(target = "amount", source = "dto.amount")
    @Mapping(target = "term", source = "dto.term")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "idStatus", ignore = true)
    @Mapping(target = "idLoanType", source = "dto.idLoanType", qualifiedByName = "strToUuid")
    @Mapping(target = "idUser", ignore = true)
    Application toDomain(ApplicationRequestDTO dto);
}
