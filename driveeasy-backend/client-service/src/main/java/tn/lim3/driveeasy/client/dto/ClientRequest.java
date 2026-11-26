package tn.lim3.driveeasy.client.dto;

import jakarta.validation.constraints.*;
import tn.lim3.driveeasy.client.model.CategorieClient;

import java.time.LocalDate;

/** Donnees recues pour creer / modifier un client (POST, PUT). */
public record ClientRequest(
        @NotBlank(message = "Le CIN est obligatoire")
        @Pattern(regexp = "\\d{8}", message = "Le CIN doit contenir exactement 8 chiffres")
        String cin,
        @NotBlank(message = "Le nom est obligatoire") @Size(max = 60) String nom,
        @NotBlank(message = "Le prenom est obligatoire") @Size(max = 60) String prenom,
        @NotNull(message = "La date de naissance est obligatoire")
        @Past(message = "La date de naissance doit etre dans le passe")
        LocalDate dateNaissance,
        @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email,
        @NotBlank(message = "Le telephone est obligatoire")
        @Pattern(regexp = "\\+?[0-9 ]{8,15}", message = "Telephone invalide")
        String telephone,
        String adresse,
        @NotBlank(message = "Le numero de permis est obligatoire") String numeroPermis,
        @NotNull(message = "La date d'expiration du permis est obligatoire") LocalDate dateExpirationPermis,
        CategorieClient categorieClient
) {
}
