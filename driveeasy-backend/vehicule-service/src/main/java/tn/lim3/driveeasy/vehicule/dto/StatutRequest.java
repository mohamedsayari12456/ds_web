package tn.lim3.driveeasy.vehicule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tn.lim3.driveeasy.vehicule.model.StatutVehicule;

/**
 * Changement de statut (PATCH /vehicules/{id}/statut), utilise par location-service.
 * Le kilometrage est optionnel : il est mis a jour au retour d'une location.
 */
public record StatutRequest(
        @NotNull(message = "Le statut est obligatoire") StatutVehicule statut,
        @PositiveOrZero(message = "Le kilometrage doit etre positif ou nul") Integer kilometrage
) {
}
