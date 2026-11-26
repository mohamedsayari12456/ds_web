package tn.lim3.driveeasy.location.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.lim3.driveeasy.location.model.Location;
import tn.lim3.driveeasy.location.model.StatutLocation;
import tn.lim3.driveeasy.location.repository.LocationRepository;
import tn.lim3.driveeasy.location.service.TarificationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Historique de demonstration charge en RAM, coherent avec les jeux de donnees de vehicule-service
 * et client-service (memes identifiants, tarifs et categories).
 *  1 : EN_COURS  (client 2, Hyundai Tucson)         2 : EN_COURS en retard (client 3, Mercedes Classe C)
 *  3 : RESERVEE  (client 4, Renault Clio)           4 : RESERVEE (client 1, Tesla Model 3)
 *  5 : ANNULEE   (client 6, VW Golf)                6+ : TERMINEE sur les 6 derniers mois
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Tarif et categorie des vehicules de demo (identiques a vehicule-service). */
    private static final Map<Long, Object[]> PARC = Map.ofEntries(
            Map.entry(1L, new Object[]{"85.00", "ECONOMIQUE"}), Map.entry(2L, new Object[]{"95.00", "COMPACTE"}),
            Map.entry(3L, new Object[]{"110.00", "COMPACTE"}), Map.entry(4L, new Object[]{"130.00", "BERLINE"}),
            Map.entry(5L, new Object[]{"160.00", "SUV"}), Map.entry(6L, new Object[]{"155.00", "SUV"}),
            Map.entry(7L, new Object[]{"260.00", "LUXE"}), Map.entry(8L, new Object[]{"290.00", "LUXE"}),
            Map.entry(9L, new Object[]{"65.00", "ECONOMIQUE"}), Map.entry(10L, new Object[]{"120.00", "UTILITAIRE"}),
            Map.entry(11L, new Object[]{"220.00", "BERLINE"}), Map.entry(12L, new Object[]{"70.00", "ECONOMIQUE"}));

    private final LocationRepository repository;
    private final TarificationService tarification;
    private final LocationProperties properties;

    public DataInitializer(LocationRepository repository, TarificationService tarification,
                           LocationProperties properties) {
        this.repository = repository;
        this.tarification = tarification;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!properties.donneesDemo() || repository.count() > 0) {
            return;
        }
        LocalDate t = LocalDate.now();
        creer(2L, 5L, t.minusDays(3), t.plusDays(2), "FIDELE", StatutLocation.EN_COURS, null, 30100, null);
        creer(3L, 7L, t.minusDays(6), t.minusDays(1), "STANDARD", StatutLocation.EN_COURS, null, 12750, null);
        creer(4L, 2L, t.plusDays(2), t.plusDays(5), "STANDARD", StatutLocation.RESERVEE, null, null, null);
        creer(1L, 11L, t.plusDays(7), t.plusDays(10), "STANDARD", StatutLocation.RESERVEE, null, null, null);
        creer(6L, 3L, t.plusDays(1), t.plusDays(4), "STANDARD", StatutLocation.ANNULEE, null, null, null);

        // Historique TERMINEE : client 5 (VIP) 16 locations, client 2 (FIDELE) 6, client 9 (FIDELE) 5, autres
        long[][] historique = {
                // client, vehicule, debut (jours avant aujourd'hui), duree, retard
                {5, 8, 178, 3, 0}, {5, 7, 167, 2, 0}, {5, 4, 155, 4, 1}, {5, 8, 141, 3, 0}, {5, 11, 130, 2, 0},
                {5, 7, 118, 5, 0}, {5, 6, 104, 3, 0}, {5, 8, 92, 2, 0}, {5, 11, 80, 4, 2}, {5, 7, 69, 3, 0},
                {5, 8, 57, 2, 0}, {5, 4, 46, 3, 0}, {5, 7, 35, 4, 0}, {5, 11, 24, 2, 0}, {5, 8, 16, 3, 1},
                {5, 6, 9, 2, 0},
                {2, 1, 170, 5, 0}, {2, 3, 140, 3, 0}, {2, 5, 112, 7, 0}, {2, 1, 84, 4, 1}, {2, 6, 52, 3, 0},
                {2, 4, 20, 2, 0},
                {9, 9, 160, 6, 0}, {9, 10, 121, 2, 0}, {9, 2, 95, 4, 0}, {9, 9, 61, 3, 0}, {9, 1, 28, 5, 0},
                {1, 2, 150, 3, 0}, {1, 4, 75, 4, 0}, {3, 10, 133, 2, 3}, {3, 5, 41, 5, 0}, {4, 1, 66, 3, 0},
                {6, 9, 99, 4, 0}, {8, 3, 180, 3, 0}, {10, 2, 14, 3, 0}, {1, 6, 6, 2, 0}
        };
        for (long[] h : historique) {
            LocalDate debut = t.minusDays(h[2]);
            LocalDate fin = debut.plusDays(h[3]);
            String categorieClient = h[0] == 5 ? (h[2] < 60 ? "VIP" : "FIDELE") : (h[0] == 2 || h[0] == 9) && h[2] < 70 ? "FIDELE" : "STANDARD";
            int kmDepart = 10000 + (int) (h[2] * 137 % 50000);
            creer(h[0], h[1], debut, fin, categorieClient, StatutLocation.TERMINEE, fin.plusDays(h[4]), kmDepart,
                    kmDepart + (int) (h[3] * 180));
        }
        log.info("{} locations de demonstration chargees en memoire", repository.count());
    }

    private void creer(long clientId, long vehiculeId, LocalDate debut, LocalDate fin, String categorieClient,
                       StatutLocation statut, LocalDate retour, Integer kmDepart, Integer kmRetour) {
        Object[] infos = PARC.get(vehiculeId);
        BigDecimal prix = new BigDecimal((String) infos[0]);
        TarificationService.Devis devis = tarification.calculer(tarification.nbJours(debut, fin), prix,
                (String) infos[1], categorieClient);
        Location l = new Location();
        l.setClientId(clientId);
        l.setVehiculeId(vehiculeId);
        l.setDateDebut(debut);
        l.setDateFinPrevue(fin);
        l.setPrixJournalierApplique(prix);
        l.setCoefficientCategorie(devis.coefficient());
        l.setTauxRemise(devis.tauxRemise());
        l.setMontantRemise(devis.montantRemise());
        l.setMontantBase(devis.total());
        BigDecimal penalite = retour != null
                ? tarification.penalite(prix, tarification.joursDeRetard(fin, retour)) : BigDecimal.ZERO;
        l.setPenalite(penalite);
        l.setMontantTotal(devis.total().add(penalite));
        l.setDateRetourEffectif(retour);
        l.setKmDepart(kmDepart);
        l.setKmRetour(kmRetour);
        l.setStatut(statut);
        l.setDateCreation(LocalDateTime.of(debut.minusDays(2), java.time.LocalTime.of(10, 0)));
        repository.save(l);
    }
}
