package tn.lim3.driveeasy.client.dto;

import tn.lim3.driveeasy.client.model.CategorieClient;

import java.util.List;

/** Resultat de GET /clients/{id}/eligibilite (consomme par location-service). */
public record EligibiliteResponse(
        Long clientId,
        boolean eligible,
        int age,
        boolean ageSuffisant,
        boolean permisValide,
        CategorieClient categorieClient,
        List<String> motifs
) {
}
