package tn.lim3.driveeasy.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.lim3.driveeasy.client.model.CategorieClient;
import tn.lim3.driveeasy.client.model.Client;
import tn.lim3.driveeasy.client.repository.ClientRepository;

import java.time.LocalDate;

/**
 * 10 clients de demonstration (charges en RAM). Cas couverts :
 *  client 7 = moins de 21 ans (non eligible), client 8 = permis expire (non eligible),
 *  client 2 et 9 = FIDELE, client 5 = VIP (coherent avec l'historique de location-service).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final ClientRepository repository;
    private final ClientProperties properties;

    public DataInitializer(ClientRepository repository, ClientProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (!properties.donneesDemo() || repository.count() > 0) {
            return;
        }
        LocalDate t = LocalDate.now();
        ajouter("09876543", "BEN ALI", "Sami", t.minusYears(35).minusDays(40), "sami.benali@mail.tn", "+216 98 123 456", "12 rue de Marseille, Tunis", "TN-2010-11223", t.plusYears(4), CategorieClient.STANDARD, t.minusMonths(8));
        ajouter("08765432", "TRABELSI", "Amira", t.minusYears(29).minusDays(100), "amira.trabelsi@mail.tn", "+216 22 334 455", "Av. Habib Bourguiba, Sousse", "TN-2015-44556", t.plusYears(6), CategorieClient.FIDELE, t.minusMonths(7));
        ajouter("07654321", "GHARBI", "Youssef", t.minusYears(42).minusDays(10), "youssef.gharbi@mail.tn", "+216 55 667 788", "Route de la Marsa, La Marsa", "TN-2005-77889", t.plusYears(2), CategorieClient.STANDARD, t.minusMonths(5));
        ajouter("06543210", "JAZIRI", "Ines", t.minusYears(26).minusDays(200), "ines.jaziri@mail.tn", "+216 97 889 900", "Cite El Ghazela, Ariana", "TN-2019-99001", t.plusYears(8), CategorieClient.STANDARD, t.minusMonths(3));
        ajouter("05432109", "MANSOUR", "Karim", t.minusYears(48).minusDays(5), "karim.mansour@mail.tn", "+216 20 112 233", "Les Berges du Lac, Tunis", "TN-1998-11002", t.plusYears(3), CategorieClient.VIP, t.minusMonths(7));
        ajouter("04321098", "HADDAD", "Leila", t.minusYears(31).minusDays(70), "leila.haddad@mail.tn", "+216 50 445 566", "Rue Ibn Khaldoun, Sfax", "TN-2012-33445", t.plusYears(5), CategorieClient.STANDARD, t.minusMonths(2));
        ajouter("13210987", "BOUAZIZI", "Omar", t.minusYears(19).minusDays(30), "omar.bouazizi@mail.tn", "+216 93 556 677", "Av. de la Republique, Bizerte", "TN-2024-55667", t.plusYears(9), CategorieClient.STANDARD, t.minusMonths(1));
        ajouter("12109876", "KHELIFI", "Nour", t.minusYears(37).minusDays(15), "nour.khelifi@mail.tn", "+216 24 778 899", "Rue de Rome, Nabeul", "TN-2008-77880", t.minusMonths(2), CategorieClient.STANDARD, t.minusMonths(6));
        ajouter("11098765", "CHAABANE", "Mehdi", t.minusYears(33).minusDays(90), "mehdi.chaabane@mail.tn", "+216 98 990 011", "Cite Ennasr, Ariana", "TN-2011-99112", t.plusYears(7), CategorieClient.FIDELE, t.minusMonths(7));
        ajouter("10987654", "SAIDI", "Rania", t.minusYears(24).minusDays(120), "rania.saidi@mail.tn", "+216 21 223 344", "Av. Farhat Hached, Monastir", "TN-2021-22334", t.plusDays(20), CategorieClient.STANDARD, t.minusMonths(1));
        log.info("{} clients de demonstration charges en memoire", repository.count());
    }

    private void ajouter(String cin, String nom, String prenom, LocalDate naissance, String email, String tel,
                         String adresse, String permis, LocalDate expiration, CategorieClient cat, LocalDate inscription) {
        Client c = new Client();
        c.setCin(cin);
        c.setNom(nom);
        c.setPrenom(prenom);
        c.setDateNaissance(naissance);
        c.setEmail(email);
        c.setTelephone(tel);
        c.setAdresse(adresse);
        c.setNumeroPermis(permis);
        c.setDateExpirationPermis(expiration);
        c.setCategorieClient(cat);
        c.setDateInscription(inscription);
        repository.save(c);
    }
}
