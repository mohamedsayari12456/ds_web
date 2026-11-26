package tn.lim3.driveeasy.vehicule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Blocage d'un vehicule sur une periode pour une location (appele par location-service). */
public record ReservationRequest(
        @NotNull(message = "locationId obligatoire") Long locationId,
        @NotNull(message = "dateDebut obligatoire") LocalDate dateDebut,
        @NotNull(message = "dateFin obligatoire") LocalDate dateFin
) {
}
