package tn.lim3.driveeasy.location.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Appels synchrones vers client-service (resolu par Eureka + load balancer : http://client-service). */
@Component
public class ClientServiceClient {

    private static final String SERVICE = "client-service";
    private final RestClient restClient;

    public ClientServiceClient(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.baseUrl("http://" + SERVICE).build();
    }

    /** GET /clients/{id}/eligibilite */
    public EligibiliteDto eligibilite(Long clientId) {
        try {
            return restClient.get().uri("/clients/{id}/eligibilite", clientId).retrieve().body(EligibiliteDto.class);
        } catch (Exception e) {
            throw RemoteErrorHelper.traduire(e, SERVICE);
        }
    }

    /** GET /clients/{id} */
    public ClientDto client(Long clientId) {
        try {
            return restClient.get().uri("/clients/{id}", clientId).retrieve().body(ClientDto.class);
        } catch (Exception e) {
            throw RemoteErrorHelper.traduire(e, SERVICE);
        }
    }

    /** PATCH /clients/{id}/categorie : mise a jour de la fidelite. */
    public ClientDto changerCategorie(Long clientId, String categorie) {
        try {
            return restClient.patch().uri("/clients/{id}/categorie", clientId)
                    .body(Map.of("categorie", categorie))
                    .retrieve().body(ClientDto.class);
        } catch (Exception e) {
            throw RemoteErrorHelper.traduire(e, SERVICE);
        }
    }
}
