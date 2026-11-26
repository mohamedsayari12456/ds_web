package tn.lim3.driveeasy.location.client;

import java.util.List;

/** Reponse de GET /clients/{id}/eligibilite. */
public record EligibiliteDto(Long clientId, boolean eligible, int age, boolean ageSuffisant, boolean permisValide,
                             String categorieClient, List<String> motifs) {
}
