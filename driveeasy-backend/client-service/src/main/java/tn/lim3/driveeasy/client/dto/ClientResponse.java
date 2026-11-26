package tn.lim3.driveeasy.client.dto;

import tn.lim3.driveeasy.client.model.CategorieClient;
import tn.lim3.driveeasy.client.model.Client;

import java.time.LocalDate;
import java.time.Period;

/** Representation d'un client renvoyee par l'API (avec champs calcules : age, permis expire). */
public record ClientResponse(
        Long id,
        String cin,
        String nom,
        String prenom,
        LocalDate dateNaissance,
        Integer age,
        String email,
        String telephone,
        String adresse,
        String numeroPermis,
        LocalDate dateExpirationPermis,
        boolean permisExpire,
        CategorieClient categorieClient,
        LocalDate dateInscription
) {
    public static ClientResponse from(Client c) {
        LocalDate aujourdhui = LocalDate.now();
        return new ClientResponse(c.getId(), c.getCin(), c.getNom(), c.getPrenom(), c.getDateNaissance(),
                Period.between(c.getDateNaissance(), aujourdhui).getYears(), c.getEmail(), c.getTelephone(),
                c.getAdresse(), c.getNumeroPermis(), c.getDateExpirationPermis(),
                c.getDateExpirationPermis().isBefore(aujourdhui), c.getCategorieClient(), c.getDateInscription());
    }
}
