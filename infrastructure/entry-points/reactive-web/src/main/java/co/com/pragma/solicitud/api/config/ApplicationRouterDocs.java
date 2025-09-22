package co.com.pragma.solicitud.api.config;

import co.com.pragma.solicitud.api.ApplicationHandler;
import co.com.pragma.solicitud.api.dto.request.ApplicationRequestDTO;
import co.com.pragma.solicitud.api.dto.response.ApplicationResponseDTO;
import co.com.pragma.solicitud.api.dto.response.ApplicationUpdateResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static co.com.pragma.solicitud.api.docs.ApplicationApiDocs.*;

@Configuration
public class ApplicationRouterDocs {

    private final RouterFunction<ServerResponse> applicationRoutes;

    public ApplicationRouterDocs(@Qualifier("applicationRoutes") RouterFunction<ServerResponse> applicationRoutes) {
        this.applicationRoutes = applicationRoutes;
    }

    @Bean("applicationRoutesOpenApi")
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/solicitudes",
                    produces = { "application/json" },
                    method = RequestMethod.POST,
                    beanClass = ApplicationHandler.class,
                    beanMethod = "registerApplication",
                    operation = @Operation(
                            operationId = "registerApplication",
                            tags = { TAG },
                            security = { @SecurityRequirement(name = "bearerAuth") },
                            summary = REG_SUMMARY,
                            description = REG_DESC,
                            requestBody = @RequestBody(required = true, content = @Content(
                                    schema = @Schema(implementation = ApplicationRequestDTO.class)
                            )),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Creado",
                                            content = @Content(schema = @Schema(implementation = ApplicationResponseDTO.class))),
                                    @ApiResponse(responseCode = "400", description = "Solicitud invalida"),
                                    @ApiResponse(responseCode = "409", description = "Conflicto por duplicados"),
                                    @ApiResponse(responseCode = "422", description = "Regla de negocio violada")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitudes/listar",
                    produces = { "application/json" },
                    method = RequestMethod.GET,
                    beanClass = ApplicationHandler.class,
                    beanMethod = "listApplications",
                    operation = @Operation(
                            operationId = "listApplications",
                            tags = { TAG },
                            security = { @SecurityRequirement(name = "bearerAuth") },
                            summary = LIST_SUMMARY,
                            description = LIST_DESC,
                            responses = @ApiResponse(responseCode = "200",
                                    description = "OK",
                                    content = @Content(schema = @Schema(implementation = ApplicationResponseDTO.class)))
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitudes/{id}",
                    produces = { "application/json" },
                    method = RequestMethod.GET,
                    beanClass = ApplicationHandler.class,
                    beanMethod = "getApplicationById",
                    operation = @Operation(
                            operationId = "getApplicationById",
                            tags = { TAG },
                            security = { @SecurityRequirement(name = "bearerAuth") },
                            summary = GET_BY_ID_SUMMARY,
                            description = GET_BY_ID_DESC,
                            parameters = {
                                    @Parameter(name = "id", in = ParameterIn.PATH, required = true,
                                            description = "ID de la solicitud")
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "OK",
                                            content = @Content(schema = @Schema(implementation = ApplicationResponseDTO.class))),
                                    @ApiResponse(responseCode = "404", description = "No encontrado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitudes/lista-paginada",
                    produces = { "application/json" },
                    method = RequestMethod.GET,
                    beanClass = ApplicationHandler.class,
                    beanMethod = "listApplicationsPageable",
                    operation = @Operation(
                            operationId = "listApplicationsPageable",
                            tags = { TAG },
                            security = { @SecurityRequirement(name = "bearerAuth") },
                            summary = "Listar solicitudes con filtros y paginacion",
                            description = "Permite listar solicitudes aplicando filtros opcionales por email, monto, tipo de prestamo, estado y salario, soportando paginacion.",
                            parameters = {
                                    @Parameter(name = "email", in = ParameterIn.QUERY, description = "Filtrar por email del solicitante"),
                                    @Parameter(name = "term", in = ParameterIn.QUERY, description = "Filtrar por plazo del prestamo (en meses)"),
                                    @Parameter(name = "minAmount", in = ParameterIn.QUERY, description = "Monto minimo del prestamo"),
                                    @Parameter(name = "maxAmount", in = ParameterIn.QUERY, description = "Monto maximo del prestamo"),
                                    @Parameter(name = "loanTypeName", in = ParameterIn.QUERY, description = "Filtrar por tipo de prestamo"),
                                    @Parameter(name = "statusDescription", in = ParameterIn.QUERY, description = "Filtrar por estado de la solicitud"),
                                    @Parameter(name = "minSalary", in = ParameterIn.QUERY, description = "Salario minimo del solicitante"),
                                    @Parameter(name = "maxSalary", in = ParameterIn.QUERY, description = "Salario maximo del solicitante"),
                                    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Numero de pagina, por defecto 0"),
                                    @Parameter(name = "size", in = ParameterIn.QUERY, description = "Tamaño de pagina, por defecto 10")
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "OK",
                                            content = @Content(schema = @Schema(implementation = ApplicationResponseDTO.class))),
                                    @ApiResponse(responseCode = "400", description = "Solicitud invalida"),
                                    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitudes/{idApplication}",
                    produces = { "application/json" },
                    method = RequestMethod.PUT,
                    beanClass = ApplicationHandler.class,
                    beanMethod = "changeApplicationStatus",
                    operation = @Operation(
                            operationId = "changeApplicationStatus",
                            tags = { TAG },
                            security = { @SecurityRequirement(name = "bearerAuth") },
                            summary = "Actualizar estado de la solicitud",
                            description = "Actualiza el estado de una solicitud existente, aplicando las reglas de negocio correspondientes.",
                            parameters = {
                                    @Parameter(name = "idApplication", in = ParameterIn.PATH, required = true,
                                            description = "ID de la solicitud a actualizar"),
                                    @Parameter(name = "targetStatus", in = ParameterIn.QUERY, description = "Estado destino: 'Aprobada' o 'Rechazada'")
                            },
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "OK",
                                            content = @Content(schema = @Schema(implementation = ApplicationUpdateResponseDTO.class))),
                                    @ApiResponse(responseCode = "400", description = "Solicitud invalida"),
                                    @ApiResponse(responseCode = "404", description = "No encontrado"),
                                    @ApiResponse(responseCode = "422", description = "Regla de negocio violada")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> applicationRoutesOpenApi() {
        return applicationRoutes;
    }
}
