package tn.lim3.driveeasy.location.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/** PATCH /locations/{id}/retour : kilometrage au retour et date de retour (aujourd'hui par defaut). */
public record RetourRequest(
        @NotNull(message = "Le kilometrage de retour est obligatoire")
        @PositiveOrZero(message = "Le kilometrage doit etre positif") Integer kmRetour,
        LocalDate dateRetourEffectif
) {
}
