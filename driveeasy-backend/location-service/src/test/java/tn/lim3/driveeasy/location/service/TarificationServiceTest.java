package tn.lim3.driveeasy.location.service;

import org.junit.jupiter.api.Test;
import tn.lim3.driveeasy.location.config.LocationProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests unitaires des regles tarifaires du cahier des charges. */
class TarificationServiceTest {

    private final TarificationService service = new TarificationService(new LocationProperties(
            false, 10, new BigDecimal("1.5"), 5, 15,
            Map.of("STANDARD", BigDecimal.ZERO, "FIDELE", BigDecimal.valueOf(5), "VIP", BigDecimal.TEN),
            Map.of("ECONOMIQUE", new BigDecimal("1.00"), "SUV", new BigDecimal("1.20"))));

    @Test
    void prixStandard_sansRemise() {
        var devis = service.calculer(4, new BigDecimal("100.00"), "ECONOMIQUE", "STANDARD");
        assertThat(devis.sousTotal()).isEqualByComparingTo("400.00");
        assertThat(devis.montantRemise()).isEqualByComparingTo("0");
        assertThat(devis.total()).isEqualByComparingTo("400.00");
    }

    @Test
    void prixVip_avecCoefficientEtRemiseDe10Pourcent() {
        // 3 jours x 160 x 1,20 = 576 ; remise 10 % = 57,60 ; total 518,40
        var devis = service.calculer(3, new BigDecimal("160.00"), "SUV", "VIP");
        assertThat(devis.sousTotal()).isEqualByComparingTo("576.00");
        assertThat(devis.montantRemise()).isEqualByComparingTo("57.60");
        assertThat(devis.total()).isEqualByComparingTo("518.40");
    }

    @Test
    void remiseFidele_5Pourcent() {
        var devis = service.calculer(2, new BigDecimal("100.00"), "ECONOMIQUE", "FIDELE");
        assertThat(devis.total()).isEqualByComparingTo("190.00");
    }

    @Test
    void categorieInconnue_coefficient1() {
        var devis = service.calculer(1, new BigDecimal("50.00"), "INCONNUE", "STANDARD");
        assertThat(devis.total()).isEqualByComparingTo("50.00");
    }

    @Test
    void penaliteDeRetard_1virgule5ParJour() {
        LocalDate fin = LocalDate.of(2026, 11, 10);
        long retard = service.joursDeRetard(fin, LocalDate.of(2026, 11, 13));
        assertThat(retard).isEqualTo(3);
        assertThat(service.penalite(new BigDecimal("100.00"), retard)).isEqualByComparingTo("450.00");
        assertThat(service.joursDeRetard(fin, LocalDate.of(2026, 11, 9))).isZero();
    }

    @Test
    void nbJours_minimumUnJour() {
        LocalDate d = LocalDate.of(2026, 12, 1);
        assertThat(service.nbJours(d, d)).isEqualTo(1);
        assertThat(service.nbJours(d, d.plusDays(5))).isEqualTo(5);
    }

    @Test
    void seuilsDeFidelite() {
        assertThat(service.categoriePour(4)).isEqualTo("STANDARD");
        assertThat(service.categoriePour(5)).isEqualTo("FIDELE");
        assertThat(service.categoriePour(15)).isEqualTo("VIP");
    }
}
