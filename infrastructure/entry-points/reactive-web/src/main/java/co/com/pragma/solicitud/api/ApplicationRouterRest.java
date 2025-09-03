package co.com.pragma.solicitud.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class ApplicationRouterRest {

    @Bean
    public RouterFunction<ServerResponse> applicationRoutes(ApplicationHandler handler) {
        return route(POST("/api/v1/solicitudes"), handler::registerApplication)
                .andRoute(GET("/api/v1/solicitudes/listar"), handler::listApplications);
    }
}
