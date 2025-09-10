package co.com.pragma.solicitud.r2dbc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {


    @Bean("userServiceClient")
    public WebClient userServiceWebClient(
        WebClient.Builder builder,
        @Value("${clients.user-service.base-url}") String baseUrl
    ) {
        ExchangeFilterFunction propagateBearer =
                (request, next) ->
                        ReactiveSecurityContextHolder.getContext()
                                .map(sc -> sc.getAuthentication())
                                .flatMap(auth -> {
                                    final String bearer =
                                            (auth != null && auth.getCredentials() instanceof String s && !s.isBlank())
                                                    ? s
                                                    : null;

                                    ClientRequest mutated = ClientRequest.from(request)
                                            .headers(h -> { if (bearer != null) h.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearer); })
                                            .build();

                                    return next.exchange(mutated);
                                })
                                // Si no hay contexto/usuario, sigue sin header (o usa un token técnico si quieres)
                                .switchIfEmpty(
                                        // serviceToken != null && !serviceToken.isBlank()
                                        //     ? next.exchange(ClientRequest.from(request)
                                        //         .headers(h -> h.setBearerAuth(serviceToken))
                                        //         .build())
                                        //     :
                                        next.exchange(request)
                                );

        return builder
                .baseUrl(baseUrl)
                .filter(propagateBearer)
                .build();
    }
}
