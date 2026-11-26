package tn.lim3.driveeasy.vehicule.dto;

import tn.lim3.driveeasy.vehicule.model.*;

import java.math.BigDecimal;
import java.util.List;

/** Representation d'un vehicule renvoyee par l'API. */
public record VehiculeResponse(
        Long id,
        String immatriculation,
        String marque,
        String modele,
        Integer annee,
        String couleur,
        Categorie categorie,
        Carburant carburant,
        Boite boite,
        Integer nbPlaces,
        Integer kilometrage,
        BigDecimal prixJournalier,
        StatutVehicule statut,
        String imageUrl,
        List<PeriodeReservation> reservations
) {
    public static VehiculeResponse from(Vehicule v) {
        return new VehiculeResponse(v.getId(), v.getImmatriculation(), v.getMarque(), v.getModele(),
                v.getAnnee(), v.getCouleur(), v.getCategorie(), v.getCarburant(), v.getBoite(),
                v.getNbPlaces(), v.getKilometrage(), v.getPrixJournalier(), v.getStatut(), v.getImageUrl(),
                List.copyOf(v.getReservations()));
    }
}
