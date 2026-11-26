package tn.lim3.driveeasy.location.service;

import org.springframework.stereotype.Service;
import tn.lim3.driveeasy.location.config.LocationProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calculs tarifaires (regles du cahier des charges) :
 *  prix     = nbJours x prixJournalier x coefficientCategorie - remise fidelite (FIDELE 5 %, VIP 10 %)
 *  penalite = 1,5 x prixJournalier x joursDeRetard
 */
@Service
public class TarificationService {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private final LocationProperties properties;

    public TarificationService(LocationProperties properties) {
        this.properties = properties;
    }

    /** Nombre de jours factures : ecart entre les deux dates, minimum 1 (location sur la journee). */
    public long nbJours(LocalDate debut, LocalDate fin) {
        return Math.max(1, ChronoUnit.DAYS.between(debut, fin));
    }

    public Devis calculer(long nbJours, BigDecimal prixJournalier, String categorieVehicule, String categorieClient) {
        BigDecimal coefficient = properties.coefficientPour(categorieVehicule);
        BigDecimal sousTotal = prixJournalier.multiply(BigDecimal.valueOf(nbJours)).multiply(coefficient)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal taux = properties.remisePour(categorieClient);
        BigDecimal remise = sousTotal.multiply(taux).divide(CENT, 2, RoundingMode.HALF_UP);
        return new Devis(nbJours, prixJournalier, coefficient, sousTotal, taux, remise, sousTotal.subtract(remise));
    }

    public long joursDeRetard(LocalDate finPrevue, LocalDate retourEffectif) {
        return Math.max(0, ChronoUnit.DAYS.between(finPrevue, retourEffectif));
    }

    public BigDecimal penalite(BigDecimal prixJournalier, long joursRetard) {
        return prixJournalier.multiply(properties.penaliteCoefficient()).multiply(BigDecimal.valueOf(joursRetard))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Categorie de fidelite correspondant a un nombre de locations terminees. */
    public String categoriePour(long nbTerminees) {
        if (nbTerminees >= properties.seuilVip()) return "VIP";
        if (nbTerminees >= properties.seuilFidele()) return "FIDELE";
        return "STANDARD";
    }

    /** Resultat d'un calcul de prix. */
    public record Devis(long nbJours, BigDecimal prixJournalier, BigDecimal coefficient, BigDecimal sousTotal,
                        BigDecimal tauxRemise, BigDecimal montantRemise, BigDecimal total) {
    }
}
