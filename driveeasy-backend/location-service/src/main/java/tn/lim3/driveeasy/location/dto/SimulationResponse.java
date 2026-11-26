package tn.lim3.driveeasy.location.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Detail du calcul de prix : nbJours x prixJournalier x coefficient - remise fidelite. */
public record SimulationResponse(
        Long clientId,
        Long vehiculeId,
        String vehicule,
        String categorieVehicule,
        String categorieClient,
        LocalDate dateDebut,
        LocalDate dateFin,
        long nbJours,
        BigDecimal prixJournalier,
        BigDecimal coefficientCategorie,
        BigDecimal sousTotal,
        BigDecimal tauxRemise,
        BigDecimal montantRemise,
        BigDecimal montantTotal,
        boolean clientEligible,
        boolean vehiculeDisponible,
        java.util.List<String> avertissements
) {
}
