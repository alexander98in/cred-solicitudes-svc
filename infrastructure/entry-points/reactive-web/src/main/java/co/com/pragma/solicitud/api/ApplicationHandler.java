package co.com.pragma.solicitud.api;

import co.com.pragma.solicitud.api.common.ResponseUtil;
import co.com.pragma.solicitud.api.dto.request.ApplicationRequestDTO;
import co.com.pragma.solicitud.api.exceptions.RequestValidationException;
import co.com.pragma.solicitud.api.facade.ApplicationFacade;
import co.com.pragma.solicitud.model.application.ApplicationFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class ApplicationHandler {

    private static final Logger log = LoggerFactory.getLogger(ApplicationHandler.class);
    private static final String MDC_KEY = "requestId";

    private final ApplicationFacade applicationFacade;
    private final SmartValidator validator;

    public Mono<ServerResponse> registerApplication(ServerRequest req) {
        return Mono.defer(() -> {
            String requestId = MDC.get(MDC_KEY);  // Obtener el requestId desde MDC
            log.info("[{}] POST /api/v1/solicitudes - procesando registro", requestId);

            return req.bodyToMono(ApplicationRequestDTO.class)
                    .doOnNext(body -> log.debug("[{}] payload recibido (masked): monto={}",
                            requestId, body.amount()))
                    .flatMap(this::validate)
                    .flatMap(applicationFacade::registerApplication)
                    .doOnSuccess(resp -> log.info("[{}] solicitud creada id={}", requestId, resp.idApplication()))
                    .doOnError(e -> log.error("[{}] error en registerApplication: {}", requestId, e.toString()))
                    .flatMap(resp -> ResponseUtil.created(
                            req,
                            URI.create("/api/v1/solicitudes/" + resp.idApplication()),
                            "Solicitud creada exitosamente",
                            resp));
        });
    }

    // Listar todas las solicitudes
    public Mono<ServerResponse> listApplications(ServerRequest req) {
        return Mono.defer(() -> {
            String requestId = MDC.get(MDC_KEY);
            log.info("[{}] GET /api/v1/solicitudes/listar", requestId);

            return applicationFacade.getApplications()
                    .collectList()
                    .doOnSuccess(list -> log.info("[{}] listado devuelto tamaño={}", requestId, list.size()))
                    .doOnError(e -> log.error("[{}] error en listApplications: {}", requestId, e.toString()))
                    .flatMap(list -> ResponseUtil.ok(req, "Listado de solicitudes", list));
        });
    }

    public Mono<ServerResponse> listApplicationsPageable(ServerRequest request) {
        return Mono.defer(() -> {
            String requestId = MDC.get(MDC_KEY);
            ApplicationFilter filter = ApplicationFilter.builder()
                    .email(request.queryParam("email").orElse(null))
                    .term(request.queryParam("term").map(Integer::valueOf).orElse(null))
                    .minAmount(request.queryParam("minAmount").map(BigDecimal::new).orElse(null))
                    .maxAmount(request.queryParam("maxAmount").map(BigDecimal::new).orElse(null))
                    .loanTypeName(request.queryParam("loanTypeName").orElse(null))
                    .statusDescription(request.queryParam("statusDescription").orElse(null))
                    .minSalary(request.queryParam("minSalary").map(BigDecimal::new).orElse(null))
                    .maxSalary(request.queryParam("maxSalary").map(BigDecimal::new).orElse(null))
                    .page(request.queryParam("page").map(Integer::valueOf).orElse(0))
                    .size(request.queryParam("size").map(Integer::valueOf).orElse(10))
                    .build();

            log.info("[{}] GET /api/v1/solicitudes - listando con filtros: {}", requestId, filter);

            return applicationFacade.getApplicationsByPage(filter)
                    .doOnSuccess(list -> log.info("[{}] listado devuelto", requestId))
                    .doOnError(e -> log.error("[{}] error en listApplicationsPageable: {}", requestId, e.toString()))
                    .flatMap(paginatedApplications -> ResponseUtil.ok(request, "Listado de solicitudes con filtros", paginatedApplications));
        });

    }

    public Mono<ServerResponse> changeApplicationStatus(ServerRequest req) {
        return Mono.defer(() -> {
            String requestId = MDC.get(MDC_KEY);
            log.info("[{}] PUT /api/v1/solicitudes - procesando cambio de estado", requestId);
            String idApplication = (String) req.pathVariable("idApplication");
            String targetStatus = req.queryParam("targetStatus").orElse(null);
            if (targetStatus == null || targetStatus.isBlank()) {
                return ResponseUtil.badRequest(req, "El parámetro 'targetStatus' es obligatorio", null);
            }
            return applicationFacade.changeApplicationStatus(idApplication, targetStatus)
                .doOnSuccess(resp -> log.info("[{}] solicitud actualizada id={}, nuevo estado={}", requestId, resp.id(), targetStatus))
                .doOnError(e -> log.error("[{}] error en changeApplicationStatus: {}", requestId, e.toString()))
                .flatMap(resp -> ResponseUtil.ok(req, "Estado actualizado exitosamente", resp));
        });
    }

    private Mono<ApplicationRequestDTO> validate(ApplicationRequestDTO dto) {
        var errors = new BeanPropertyBindingResult(dto, ApplicationRequestDTO.class.getName());
        validator.validate(dto, errors);
        if (errors.hasErrors()) {
            var map = new java.util.LinkedHashMap<String, String>();
            errors.getFieldErrors().forEach(fe -> map.put(fe.getField(), fe.getDefaultMessage()));
            log.warn("[{}] Solicitud inválida en registerApplication: {}", MDC.get(MDC_KEY), map);
            throw new RequestValidationException("Datos inválidos en la solicitud", map);
        }
        return Mono.just(dto);
    }
}
