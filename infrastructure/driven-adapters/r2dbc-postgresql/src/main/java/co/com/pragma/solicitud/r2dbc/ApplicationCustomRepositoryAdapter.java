package co.com.pragma.solicitud.r2dbc;

import co.com.pragma.solicitud.model.application.ApplicationDetails;
import co.com.pragma.solicitud.model.application.ApplicationFilter;
import co.com.pragma.solicitud.model.application.PaginatedApplications;
import co.com.pragma.solicitud.model.application.gateways.ApplicationCustomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ApplicationCustomRepositoryAdapter implements ApplicationCustomRepository {

    private final DatabaseClient client;

    @Override
    public Mono<PaginatedApplications> listApplicationsByCriteria(ApplicationFilter filter) {
        return Mono.defer(() -> {
            String baseQuery = """
                SELECT s.id_solicitud,
                       s.monto,
                       s.plazo,
                       s.email,
                       u.nombres || ' ' || u.apellidos AS full_name,
                       t.nombre AS loan_type_name,
                       t.tasa_interes AS interest_rate,
                       e.descripcion AS status,
                       u.salario_base AS salary,
                       0::NUMERIC AS monthly_debt,
                       0::NUMERIC AS monthly_installment
                FROM solicitudes s
                JOIN usuarios u ON s.id_usuario = u.id
                JOIN tipo_prestamos t ON s.id_tipo_prestamo = t.id_tipo_prestamo
                JOIN estados e ON s.id_estado = e.id_estado
                WHERE 1=1
            """;

            StringBuilder filters = new StringBuilder();
            if (filter.getEmail() != null) filters.append(" AND s.email = :email");
            if (filter.getTerm() != null) filters.append(" AND s.plazo = :term");
            if (filter.getMinAmount() != null) filters.append(" AND s.monto >= :minAmount");
            if (filter.getMaxAmount() != null) filters.append(" AND s.monto <= :maxAmount");
            if (filter.getLoanTypeName() != null) filters.append(" AND t.nombre = :loanTypeName");
            if (filter.getStatusDescription() != null) filters.append(" AND e.descripcion = :statusDescription");
            if (filter.getMinSalary() != null) filters.append(" AND u.salario_base >= :minSalary");
            if (filter.getMaxSalary() != null) filters.append(" AND u.salario_base <= :maxSalary");

            String countQuery = "SELECT COUNT(*) FROM solicitudes s " +
                    "JOIN usuarios u ON s.id_usuario = u.id " +
                    "JOIN tipo_prestamos t ON s.id_tipo_prestamo = t.id_tipo_prestamo " +
                    "JOIN estados e ON s.id_estado = e.id_estado " +
                    "WHERE 1=1" + filters;

            String pagedQuery = baseQuery + filters +
                    " ORDER BY s.monto " +
                    " OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY";

            int offset = filter.getPage() * filter.getSize();

            // Total de registros
            Mono<Long> totalMono = bindFilters(client.sql(countQuery), filter)
                    .map((row, meta) -> row.get(0, Long.class))
                    .one();

            // Contenido paginado
            Flux<ApplicationDetails> contentFlux = bindFilters(client.sql(pagedQuery), filter)
                    .bind("offset", offset)
                    .bind("size", filter.getSize())
                    .map((row, meta) -> ApplicationDetails.builder()
                            .id(row.get("id_solicitud", UUID.class))
                            .amount(row.get("monto", BigDecimal.class))
                            .term(row.get("plazo", Integer.class))
                            .email(row.get("email", String.class))
                            .fullName(row.get("full_name", String.class))
                            .loanTypeName(row.get("loan_type_name", String.class))
                            .interestRate(row.get("interest_rate", BigDecimal.class))
                            .status(row.get("status", String.class))
                            .salary(row.get("salary", BigDecimal.class))
                            .monthlyDebt(BigDecimal.ZERO)          // futura lógica
                            .monthlyInstallment(BigDecimal.ZERO)   // futura lógica
                            .build())
                    .all();

            // Combinar total + contenido en un solo Mono
            return totalMono.flatMap(total ->
                    contentFlux.collectList().map(list ->
                            PaginatedApplications.builder()
                                    .content(list)
                                    .totalElements(total)
                                    .pageNumber(filter.getPage())
                                    .pageSize(filter.getSize())
                                    .totalPages((int) Math.ceil((double) total / filter.getSize()))
                                    .first(filter.getPage() == 0)
                                    .last((long) (filter.getPage() + 1) * filter.getSize() >= total)
                                    .numberOfElements(list.size())
                                    .build()
                    )
            );
        });
    }

    /**
     * Método utilitario para bind dinámico de filtros opcionales
     */
    private DatabaseClient.GenericExecuteSpec bindFilters(DatabaseClient.GenericExecuteSpec spec, ApplicationFilter filter) {
        if (filter.getEmail() != null) spec = spec.bind("email", filter.getEmail());
        if (filter.getTerm() != null) spec = spec.bind("term", filter.getTerm());
        if (filter.getMinAmount() != null) spec = spec.bind("minAmount", filter.getMinAmount());
        if (filter.getMaxAmount() != null) spec = spec.bind("maxAmount", filter.getMaxAmount());
        if (filter.getLoanTypeName() != null) spec = spec.bind("loanTypeName", filter.getLoanTypeName());
        if (filter.getStatusDescription() != null) spec = spec.bind("statusDescription", filter.getStatusDescription());
        if (filter.getMinSalary() != null) spec = spec.bind("minSalary", filter.getMinSalary());
        if (filter.getMaxSalary() != null) spec = spec.bind("maxSalary", filter.getMaxSalary());
        return spec;
    }
}
