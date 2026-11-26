package tn.lim3.driveeasy.location.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Appels synchrones vers vehicule-service (http://vehicule-service via Eureka). */
@Component
public class VehiculeServiceClient {

    private static final String SERVICE = "vehicule-service";
    private final RestClient restClient;

    public VehiculeServiceClient(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.baseUrl("http://" + SERVICE).build();
    }

    /** GET /vehicules/{id} : recuperation du tarif, de la categorie, du kilometrage et du statut. */
    public VehiculeDto vehicule(Long id) {
        try {
            return restClient.get().uri("/vehicules/{id}", id).retrieve().body(VehiculeDto.class);
        } catch (Exception e) {
            throw RemoteErrorHelper.traduire(e, SERVICE);
        }
    }

    /** GET /vehicules : tout le parc (statistiques). */
    public List<VehiculeDto> tous() {
        try {
            return restClient.get().uri("/vehicules").retrieve().body(new ParameterizedTypeReference<List<VehiculeDto>>() {
            });
        } catch (Exception e) {
            throw RemoteErrorHelper.traduire(e, SERVICE);
        }
    }

    /** POST /vehicules/{id}/reservations : verifie la disponibilite ET bloque la periode (atomique cote vehicule). */
    public void reserver(Long vehiculeId, Long locationId, LocalDate debut, LocalDate fin) {
        try {
            restClient.post().uri("/vehicules/{id}/reservations", vehiculeId)
                    .body(Map.of("locationId", locationId, "dateDebut", debut.toString(), "dateFin", fin.toString()))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            throw RemoteErrorHelper.traduire(e, SERVICE);
        }
    }

    /** DELETE /vehicules/{id}/reservations/{locationId} : liberation (idempotente). */
    public void liberer(Long vehiculeId, Long locationId) {
        try {
            restClient.delete().uri("/vehicules/{id}/reservations/{loc}", vehiculeId, locationId)
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            throw RemoteErrorHelper.traduire(e, SERVICE);
        }
    }

    /** PATCH /vehicules/{id}/statut : LOUE au depart, DISPONIBLE au retour (avec le nouveau kilometrage). */
    public VehiculeDto changerStatut(Long vehiculeId, String statut, Integer kilometrage) {
        Map<String, Object> body = new HashMap<>();
        body.put("statut", statut);
        if (kilometrage != null) {
            body.put("kilometrage", kilometrage);
        }
        try {
            return restClient.patch().uri("/vehicules/{id}/statut", vehiculeId)
                    .body(body).retrieve().body(VehiculeDto.class);
        } catch (Exception e) {
            throw RemoteErrorHelper.traduire(e, SERVICE);
        }
    }
}
