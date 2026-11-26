package tn.lim3.driveeasy.location.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** Demande de reservation (POST /locations) ou de simulation (POST /locations/simulation). */
public record LocationRequest(
        @NotNull(message = "Le client est obligatoire") @Positive Long clientId,
        @NotNull(message = "Le vehicule est obligatoire") @Positive Long vehiculeId,
        @NotNull(message = "La date de debut est obligatoire") LocalDate dateDebut,
        @NotNull(message = "La date de fin prevue est obligatoire") LocalDate dateFin
) {
}
