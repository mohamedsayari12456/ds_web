package tn.lim3.driveeasy.vehicule.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.lim3.driveeasy.vehicule.model.*;
import tn.lim3.driveeasy.vehicule.repository.VehiculeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Jeu de donnees de demonstration (12 vehicules), charge en RAM au demarrage
 * si driveeasy.vehicule.donnees-demo=true.
 * Les plannings (reservations) sont coherents avec les locations de demo de location-service :
 *  location 1 = vehicule 5 en cours, location 2 = vehicule 7 en cours (en retard),
 *  location 3 = vehicule 2 reserve, location 4 = vehicule 11 reserve.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final VehiculeRepository repository;
    private final VehiculeProperties properties;

    public DataInitializer(VehiculeRepository repository, VehiculeProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!properties.donneesDemo() || repository.count() > 0) {
            return;
        }
        LocalDate t = LocalDate.now();
        ajouter("123 TU 4567", "Peugeot", "208", 2022, "Blanc", Categorie.ECONOMIQUE, Carburant.ESSENCE, Boite.MANUELLE, 5, 38500, "85.00", StatutVehicule.DISPONIBLE);
        Vehicule clio = ajouter("145 TU 2210", "Renault", "Clio 5", 2023, "Gris", Categorie.COMPACTE, Carburant.DIESEL, Boite.MANUELLE, 5, 21400, "95.00", StatutVehicule.DISPONIBLE);
        ajouter("138 TU 9001", "Volkswagen", "Golf 8", 2021, "Bleu", Categorie.COMPACTE, Carburant.ESSENCE, Boite.AUTOMATIQUE, 5, 52300, "110.00", StatutVehicule.DISPONIBLE);
        ajouter("150 TU 1188", "Toyota", "Corolla Hybrid", 2024, "Noir", Categorie.BERLINE, Carburant.HYBRIDE, Boite.AUTOMATIQUE, 5, 9800, "130.00", StatutVehicule.DISPONIBLE);
        Vehicule tucson = ajouter("141 TU 7744", "Hyundai", "Tucson", 2023, "Rouge", Categorie.SUV, Carburant.DIESEL, Boite.AUTOMATIQUE, 5, 30100, "160.00", StatutVehicule.LOUE);
        ajouter("147 TU 3321", "Kia", "Sportage", 2022, "Blanc", Categorie.SUV, Carburant.ESSENCE, Boite.AUTOMATIQUE, 5, 41250, "155.00", StatutVehicule.DISPONIBLE);
        Vehicule classeC = ajouter("152 TU 0505", "Mercedes-Benz", "Classe C", 2024, "Noir", Categorie.LUXE, Carburant.DIESEL, Boite.AUTOMATIQUE, 5, 12750, "260.00", StatutVehicule.LOUE);
        ajouter("149 TU 8080", "BMW", "Serie 5", 2023, "Gris", Categorie.LUXE, Carburant.ESSENCE, Boite.AUTOMATIQUE, 5, 19900, "290.00", StatutVehicule.DISPONIBLE);
        ajouter("130 TU 6542", "Dacia", "Logan", 2020, "Beige", Categorie.ECONOMIQUE, Carburant.GPL, Boite.MANUELLE, 5, 98700, "65.00", StatutVehicule.DISPONIBLE);
        ajouter("136 TU 4410", "Renault", "Kangoo", 2021, "Blanc", Categorie.UTILITAIRE, Carburant.DIESEL, Boite.MANUELLE, 2, 76400, "120.00", StatutVehicule.DISPONIBLE);
        Vehicule tesla = ajouter("153 TU 2024", "Tesla", "Model 3", 2024, "Bleu", Categorie.BERLINE, Carburant.ELECTRIQUE, Boite.AUTOMATIQUE, 5, 8300, "220.00", StatutVehicule.DISPONIBLE);
        ajouter("128 TU 3377", "Fiat", "500", 2019, "Jaune", Categorie.ECONOMIQUE, Carburant.ESSENCE, Boite.MANUELLE, 4, 112300, "70.00", StatutVehicule.MAINTENANCE);

        // Plannings coherents avec location-service
        tucson.getReservations().add(new PeriodeReservation(1L, t.minusDays(3), t.plusDays(2)));
        classeC.getReservations().add(new PeriodeReservation(2L, t.minusDays(6), t.minusDays(1)));
        clio.getReservations().add(new PeriodeReservation(3L, t.plusDays(2), t.plusDays(5)));
        tesla.getReservations().add(new PeriodeReservation(4L, t.plusDays(7), t.plusDays(10)));
        log.info("{} vehicules de demonstration charges en memoire", repository.count());
    }

    private Vehicule ajouter(String immat, String marque, String modele, int annee, String couleur, Categorie cat,
                             Carburant carb, Boite boite, int places, int km, String prix, StatutVehicule statut) {
        Vehicule v = new Vehicule();
        v.setImmatriculation(immat);
        v.setMarque(marque);
        v.setModele(modele);
        v.setAnnee(annee);
        v.setCouleur(couleur);
        v.setCategorie(cat);
        v.setCarburant(carb);
        v.setBoite(boite);
        v.setNbPlaces(places);
        v.setKilometrage(km);
        v.setPrixJournalier(new BigDecimal(prix));
        v.setStatut(statut);
        v.setImageUrl("assets/vehicules/" + cat.name().toLowerCase() + ".svg");
        return repository.save(v);
    }
}
