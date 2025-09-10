package co.com.pragma.solicitud.r2dbc;

import co.com.pragma.solicitud.model.user.RemoteUser;
import co.com.pragma.solicitud.model.user.gateways.ExternalUserService;
import co.com.pragma.solicitud.r2dbc.client.ApiResponse;
import co.com.pragma.solicitud.r2dbc.exceptions.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class ExternalUserServiceImpl implements ExternalUserService {

    private final WebClient userServiceWebClient;

    @Override
    public Mono<RemoteUser> getUserByEmail(String email) {
        return userServiceWebClient.get()
                .uri("/api/v1/usuarios/email/{email}", email)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<RemoteUser>>() {})
                .flatMap(api -> {
                    if (api == null || api.data() == null) {
                        return Mono.error(new ExternalServiceException("Usuario no encontrado"));
                    }
                    return Mono.just(api.data());
                })
                .onErrorMap(e -> new ExternalServiceException("Error al obtener usuario: " + e.getMessage(), e));
    }
}
