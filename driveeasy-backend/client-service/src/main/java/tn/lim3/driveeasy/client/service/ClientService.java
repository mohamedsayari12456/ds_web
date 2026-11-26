package tn.lim3.driveeasy.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.lim3.driveeasy.client.client.LocationClient;
import tn.lim3.driveeasy.client.config.ClientProperties;
import tn.lim3.driveeasy.client.dto.ClientRequest;
import tn.lim3.driveeasy.client.dto.EligibiliteResponse;
import tn.lim3.driveeasy.client.exception.ConflictException;
import tn.lim3.driveeasy.client.exception.ResourceNotFoundException;
import tn.lim3.driveeasy.client.model.CategorieClient;
import tn.lim3.driveeasy.client.model.Client;
import tn.lim3.driveeasy.client.repository.ClientRepository;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Logique metier des clients : unicite (CIN, email, permis), eligibilite, fidelite. */
@Service
public class ClientService {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    private final ClientRepository repository;
    private final ClientProperties properties;
    private final LocationClient locationClient;

    public ClientService(ClientRepository repository, ClientProperties properties, LocationClient locationClient) {
        this.repository = repository;
        this.properties = properties;
        this.locationClient = locationClient;
    }

    /** Recherche par texte libre (nom, prenom, CIN, email) et/ou par champ precis. */
    public List<Client> rechercher(String q, String nom, String cin, String email, CategorieClient categorie) {
        return repository.findAll().stream()
                .filter(c -> contient(c.getNom() + " " + c.getPrenom() + " " + c.getCin() + " " + c.getEmail(), q))
                .filter(c -> contient(c.getNom() + " " + c.getPrenom(), nom))
                .filter(c -> contient(c.getCin(), cin))
                .filter(c -> contient(c.getEmail(), email))
                .filter(c -> categorie == null || c.getCategorieClient() == categorie)
                .toList();
    }

    public Client trouver(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable (id=" + id + ")"));
    }

    public synchronized Client creer(ClientRequest req) {
        verifierUnicite(req, null);
        Client c = new Client();
        appliquer(c, req);
        c.setCategorieClient(req.categorieClient() != null ? req.categorieClient() : CategorieClient.STANDARD);
        c.setDateInscription(LocalDate.now());
        Client cree = repository.save(c);
        log.info("Client cree : {} {} (CIN {})", cree.getPrenom(), cree.getNom(), cree.getCin());
        return cree;
    }

    public synchronized Client modifier(Long id, ClientRequest req) {
        Client c = trouver(id);
        verifierUnicite(req, id);
        appliquer(c, req);
        if (req.categorieClient() != null) {
            c.setCategorieClient(req.categorieClient());
        }
        return repository.save(c);
    }

    /** Suppression refusee (409) si le client a une location RESERVEE ou EN_COURS (appel a location-service). */
    public synchronized void supprimer(Long id) {
        Client c = trouver(id);
        long actives = locationClient.compterLocationsActives(id);
        if (actives > 0) {
            throw new ConflictException("Suppression refusee : le client a " + actives + " location(s) active(s)");
        }
        repository.deleteById(id);
        log.info("Client supprime : {} {}", c.getPrenom(), c.getNom());
    }

    /** Eligibilite : age >= age minimum (21 par defaut) ET permis non expire. */
    public EligibiliteResponse eligibilite(Long id) {
        Client c = trouver(id);
        LocalDate aujourdhui = LocalDate.now();
        int age = Period.between(c.getDateNaissance(), aujourdhui).getYears();
        boolean ageOk = age >= properties.ageMinimum();
        boolean permisOk = !c.getDateExpirationPermis().isBefore(aujourdhui);
        List<String> motifs = new ArrayList<>();
        if (!ageOk) {
            motifs.add("Age insuffisant : " + age + " ans (minimum " + properties.ageMinimum() + " ans)");
        }
        if (!permisOk) {
            motifs.add("Permis de conduire expire depuis le " + c.getDateExpirationPermis());
        }
        return new EligibiliteResponse(id, ageOk && permisOk, age, ageOk, permisOk, c.getCategorieClient(), motifs);
    }

    /** PATCH /clients/{id}/categorie : mise a jour de la fidelite (pilotee par location-service). */
    public synchronized Client changerCategorie(Long id, CategorieClient categorie) {
        Client c = trouver(id);
        if (c.getCategorieClient() != categorie) {
            log.info("Client {} : categorie {} -> {}", id, c.getCategorieClient(), categorie);
            c.setCategorieClient(categorie);
        }
        return repository.save(c);
    }

    // ------------------------------------------------------------------ utilitaires

    private void verifierUnicite(ClientRequest req, Long idCourant) {
        verifierChamp(Client::getCin, req.cin(), "CIN", idCourant);
        verifierChamp(Client::getEmail, req.email(), "email", idCourant);
        verifierChamp(Client::getNumeroPermis, req.numeroPermis(), "numero de permis", idCourant);
    }

    private void verifierChamp(Function<Client, String> champ, String valeur, String libelle, Long idCourant) {
        repository.findBy(champ, valeur.trim())
                .filter(existant -> !existant.getId().equals(idCourant))
                .ifPresent(existant -> {
                    throw new ConflictException("Un client avec ce " + libelle + " existe deja (" + valeur + ")");
                });
    }

    private static boolean contient(String source, String recherche) {
        return recherche == null || recherche.isBlank()
                || (source != null && source.toLowerCase().contains(recherche.trim().toLowerCase()));
    }

    private void appliquer(Client c, ClientRequest req) {
        c.setCin(req.cin().trim());
        c.setNom(req.nom().trim().toUpperCase());
        c.setPrenom(req.prenom().trim());
        c.setDateNaissance(req.dateNaissance());
        c.setEmail(req.email().trim().toLowerCase());
        c.setTelephone(req.telephone().trim());
        c.setAdresse(req.adresse());
        c.setNumeroPermis(req.numeroPermis().trim().toUpperCase());
        c.setDateExpirationPermis(req.dateExpirationPermis());
    }
}
