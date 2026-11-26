package tn.lim3.driveeasy.vehicule.dto;

import jakarta.validation.constraints.*;
import tn.lim3.driveeasy.vehicule.model.Boite;
import tn.lim3.driveeasy.vehicule.model.Carburant;
import tn.lim3.driveeasy.vehicule.model.Categorie;
import tn.lim3.driveeasy.vehicule.model.StatutVehicule;

import java.math.BigDecimal;

/** Donnees recues pour creer / modifier un vehicule (POST, PUT). */
public record VehiculeRequest(
        @NotBlank(message = "L'immatriculation est obligatoire")
        @Size(max = 20, message = "L'immatriculation ne doit pas depasser 20 caracteres")
        String immatriculation,
        @NotBlank(message = "La marque est obligatoire") String marque,
        @NotBlank(message = "Le modele est obligatoire") String modele,
        @NotNull(message = "L'annee est obligatoire") Integer annee,
        String couleur,
        @NotNull(message = "La categorie est obligatoire") Categorie categorie,
        @NotNull(message = "Le carburant est obligatoire") Carburant carburant,
        @NotNull(message = "La boite est obligatoire") Boite boite,
        @NotNull(message = "Le nombre de places est obligatoire")
        @Min(value = 1, message = "Au moins 1 place") @Max(value = 9, message = "9 places maximum")
        Integer nbPlaces,
        @NotNull(message = "Le kilometrage est obligatoire")
        @PositiveOrZero(message = "Le kilometrage doit etre positif ou nul")
        Integer kilometrage,
        @NotNull(message = "Le prix journalier est obligatoire")
        @DecimalMin(value = "0.0", inclusive = false, message = "Le prix journalier doit etre strictement positif")
        @Digits(integer = 6, fraction = 2, message = "Prix invalide (2 decimales max)")
        BigDecimal prixJournalier,
        StatutVehicule statut,
        String imageUrl
) {
}
