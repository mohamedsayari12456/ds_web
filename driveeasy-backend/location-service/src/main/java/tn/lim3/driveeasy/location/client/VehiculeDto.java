package tn.lim3.driveeasy.location.client;

import java.math.BigDecimal;

/** Vue d'un vehicule telle que renvoyee par vehicule-service. */
public record VehiculeDto(Long id, String immatriculation, String marque, String modele, Integer annee,
                          String couleur, String categorie, String carburant, String boite, Integer nbPlaces,
                          Integer kilometrage, BigDecimal prixJournalier, String statut, String imageUrl) {
    public String libelle() {
        return marque + " " + modele;
    }
}
