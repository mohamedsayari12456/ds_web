package tn.lim3.driveeasy.client.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.lim3.driveeasy.client.exception.ServiceUnavailableException;

/**
 * Appel synchrone vers location-service (resolu via Eureka : http://location-service).
 * Sert a refuser la suppression d'un client ayant une location active.
 */
@Component
public class LocationClient {

    private final RestClient restClient;

    public LocationClient(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.baseUrl("http://location-service").build();
    }

    /** Nombre de locations RESERVEE ou EN_COURS du client. */
    public long compterLocationsActives(Long clientId) {
        try {
            Long nb = restClient.get()
                    .uri("/locations/actives/count?clientId={id}", clientId)
                    .retrieve()
                    .body(Long.class);
            return nb == null ? 0 : nb;
        } catch (RestClientException | IllegalStateException e) {
            throw new ServiceUnavailableException(
                    "Impossible de verifier les locations du client : location-service indisponible", e);
        }
    }
}
