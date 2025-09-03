package co.com.pragma.solicitud.api.config;

import co.com.pragma.solicitud.api.ApplicationHandler;
import co.com.pragma.solicitud.api.dto.request.ApplicationRequestDTO;
import co.com.pragma.solicitud.api.dto.response.ApplicationResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
                            summary = REG_SUMMARY,
                            description = REG_DESC,
                            requestBody = @RequestBody(required = true, content = @Content(
                                    schema = @Schema(implementation = ApplicationRequestDTO.class)
                            )),
                            responses = {
                                    @ApiResponse(responseCode = "201", description = "Creado",
                                            content = @Content(schema = @Schema(implementation = ApplicationResponseDTO.class))),
                                    @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
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
            )
    })
    public RouterFunction<ServerResponse> applicationRoutesOpenApi() {
        return applicationRoutes;
    }
}
