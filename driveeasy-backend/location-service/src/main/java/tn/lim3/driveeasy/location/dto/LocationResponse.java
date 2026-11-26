package tn.lim3.driveeasy.location.dto;

import tn.lim3.driveeasy.location.model.Location;
import tn.lim3.driveeasy.location.model.StatutLocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Representation d'une location renvoyee par l'API. */
public record LocationResponse(
        Long id,
        Long clientId,
        Long vehiculeId,
        LocalDate dateDebut,
        LocalDate dateFinPrevue,
        LocalDate dateRetourEffectif,
        long nbJours,
        Integer kmDepart,
        Integer kmRetour,
        Integer kmParcourus,
        BigDecimal prixJournalierApplique,
        BigDecimal coefficientCategorie,
        BigDecimal tauxRemise,
        BigDecimal montantRemise,
        BigDecimal montantBase,
        BigDecimal penalite,
        BigDecimal montantTotal,
        StatutLocation statut,
        boolean enRetard,
        LocalDateTime dateCreation
) {
    public static LocationResponse from(Location l) {
        Integer parcourus = (l.getKmDepart() != null && l.getKmRetour() != null) ? l.getKmRetour() - l.getKmDepart() : null;
        boolean enRetard = l.getStatut() == StatutLocation.EN_COURS && LocalDate.now().isAfter(l.getDateFinPrevue());
        return new LocationResponse(l.getId(), l.getClientId(), l.getVehiculeId(), l.getDateDebut(),
                l.getDateFinPrevue(), l.getDateRetourEffectif(),
                Math.max(1, ChronoUnit.DAYS.between(l.getDateDebut(), l.getDateFinPrevue())),
                l.getKmDepart(), l.getKmRetour(), parcourus, l.getPrixJournalierApplique(), l.getCoefficientCategorie(),
                l.getTauxRemise(), l.getMontantRemise(), l.getMontantBase(), l.getPenalite(), l.getMontantTotal(),
                l.getStatut(), enRetard, l.getDateCreation());
    }
}
