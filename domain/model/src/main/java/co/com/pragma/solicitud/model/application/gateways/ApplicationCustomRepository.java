package co.com.pragma.solicitud.model.application.gateways;

import co.com.pragma.solicitud.model.application.ApplicationFilter;
import co.com.pragma.solicitud.model.application.PaginatedApplications;
import reactor.core.publisher.Mono;

public interface ApplicationCustomRepository {

    Mono<PaginatedApplications> listApplicationsByCriteria(ApplicationFilter filter);
}
