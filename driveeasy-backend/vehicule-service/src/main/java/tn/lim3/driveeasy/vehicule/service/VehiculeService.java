package tn.lim3.driveeasy.vehicule.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.lim3.driveeasy.vehicule.config.VehiculeProperties;
import tn.lim3.driveeasy.vehicule.dto.ReservationRequest;
import tn.lim3.driveeasy.vehicule.dto.StatutRequest;
import tn.lim3.driveeasy.vehicule.dto.VehiculeRequest;
import tn.lim3.driveeasy.vehicule.exception.BusinessException;
import tn.lim3.driveeasy.vehicule.exception.ConflictException;
import tn.lim3.driveeasy.vehicule.exception.ResourceNotFoundException;
import tn.lim3.driveeasy.vehicule.model.*;
import tn.lim3.driveeasy.vehicule.repository.VehiculeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

/** Logique metier du parc automobile : validations, unicite, statuts et planning de disponibilite. */
@Service
public class VehiculeService {

    private static final Logger log = LoggerFactory.getLogger(VehiculeService.class);

    private final VehiculeRepository repository;
    private final VehiculeProperties properties;

    public VehiculeService(VehiculeRepository repository, VehiculeProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Liste filtree : categorie, carburant, statut, prix maximum et recherche texte (marque/modele/immat). */
    public List<Vehicule> rechercher(Categorie categorie, Carburant carburant, StatutVehicule statut,
                                     BigDecimal prixMax, String q) {
        String texte = q == null ? null : q.trim().toLowerCase();
        return repository.findAll().stream()
                .filter(v -> categorie == null || v.getCategorie() == categorie)
                .filter(v -> carburant == null || v.getCarburant() == carburant)
                .filter(v -> statut == null || v.getStatut() == statut)
                .filter(v -> prixMax == null || v.getPrixJournalier().compareTo(prixMax) <= 0)
                .filter(v -> texte == null || texte.isEmpty()
                        || (v.getMarque() + " " + v.getModele() + " " + v.getImmatriculation()).toLowerCase().contains(texte))
                .toList();
    }

    public Vehicule trouver(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicule introuvable (id=" + id + ")"));
    }

    public synchronized Vehicule creer(VehiculeRequest req) {
        valider(req);
        repository.findByImmatriculation(req.immatriculation().trim()).ifPresent(v -> {
            throw new ConflictException("L'immatriculation " + req.immatriculation() + " existe deja");
        });
        Vehicule v = new Vehicule();
        appliquer(v, req);
        v.setStatut(req.statut() != null ? req.statut() : StatutVehicule.DISPONIBLE);
        if (v.getStatut() == StatutVehicule.LOUE) {
            throw new BusinessException("Un nouveau vehicule ne peut pas etre cree avec le statut LOUE");
        }
        Vehicule cree = repository.save(v);
        log.info("Vehicule cree : {} ({} {})", cree.getImmatriculation(), cree.getMarque(), cree.getModele());
        return cree;
    }

    public synchronized Vehicule modifier(Long id, VehiculeRequest req) {
        Vehicule v = trouver(id);
        valider(req);
        repository.findByImmatriculation(req.immatriculation().trim())
                .filter(autre -> !autre.getId().equals(id))
                .ifPresent(autre -> {
                    throw new ConflictException("L'immatriculation " + req.immatriculation() + " existe deja");
                });
        appliquer(v, req);
        // Le statut LOUE est pilote uniquement par location-service (depart / retour)
        if (req.statut() != null && req.statut() != v.getStatut()) {
            changerStatutManuel(v, req.statut());
        }
        return repository.save(v);
    }

    public synchronized void supprimer(Long id) {
        Vehicule v = trouver(id);
        if (v.getStatut() == StatutVehicule.LOUE) {
            throw new ConflictException("Suppression refusee : le vehicule " + v.getImmatriculation() + " est actuellement loue");
        }
        if (!v.getReservations().isEmpty()) {
            throw new ConflictException("Suppression refusee : le vehicule a " + v.getReservations().size()
                    + " reservation(s) a venir");
        }
        repository.deleteById(id);
        log.info("Vehicule supprime : {}", v.getImmatriculation());
    }

    /** PATCH /vehicules/{id}/statut : utilise par location-service au depart (LOUE) et au retour (DISPONIBLE). */
    public synchronized Vehicule changerStatut(Long id, StatutRequest req) {
        Vehicule v = trouver(id);
        if (req.kilometrage() != null) {
            if (req.kilometrage() < v.getKilometrage()) {
                throw new BusinessException("Le kilometrage (" + req.kilometrage()
                        + ") ne peut pas etre inferieur au kilometrage actuel (" + v.getKilometrage() + ")");
            }
            v.setKilometrage(req.kilometrage());
        }
        if (req.statut() == StatutVehicule.LOUE && v.getStatut() == StatutVehicule.MAINTENANCE) {
            throw new ConflictException("Le vehicule est en MAINTENANCE et ne peut pas etre loue");
        }
        if (req.statut() == StatutVehicule.MAINTENANCE && v.getStatut() == StatutVehicule.LOUE) {
            throw new ConflictException("Le vehicule est LOUE : attendre son retour avant la maintenance");
        }
        log.info("Vehicule {} : statut {} -> {}", v.getImmatriculation(), v.getStatut(), req.statut());
        v.setStatut(req.statut());
        return repository.save(v);
    }

    /** Vehicules reservables sur [dateDebut, dateFin] : statut DISPONIBLE et aucun chevauchement de planning. */
    public List<Vehicule> disponibles(LocalDate dateDebut, LocalDate dateFin, Categorie categorie) {
        verifierPeriode(dateDebut, dateFin);
        return repository.findAll().stream()
                .filter(v -> v.getStatut() == StatutVehicule.DISPONIBLE)
                .filter(v -> categorie == null || v.getCategorie() == categorie)
                .filter(v -> v.getReservations().stream().noneMatch(p -> p.chevauche(dateDebut, dateFin)))
                .toList();
    }

    /**
     * Bloque un vehicule sur une periode pour une location. Operation atomique (synchronized) :
     * deux reservations simultanees du meme vehicule ne peuvent pas aboutir toutes les deux.
     */
    public synchronized Vehicule reserver(Long id, ReservationRequest req) {
        Vehicule v = trouver(id);
        verifierPeriode(req.dateDebut(), req.dateFin());
        if (v.getStatut() != StatutVehicule.DISPONIBLE) {
            throw new ConflictException("Le vehicule " + v.getImmatriculation() + " est " + v.getStatut()
                    + " et ne peut pas etre reserve");
        }
        boolean dejaPris = v.getReservations().stream()
                .anyMatch(p -> !p.locationId().equals(req.locationId()) && p.chevauche(req.dateDebut(), req.dateFin()));
        if (dejaPris) {
            throw new ConflictException("Le vehicule " + v.getImmatriculation() + " est deja reserve sur cette periode");
        }
        v.getReservations().removeIf(p -> p.locationId().equals(req.locationId()));
        v.getReservations().add(new PeriodeReservation(req.locationId(), req.dateDebut(), req.dateFin()));
        log.info("Vehicule {} reserve du {} au {} (location {})", v.getImmatriculation(),
                req.dateDebut(), req.dateFin(), req.locationId());
        return repository.save(v);
    }

    /** Libere la periode bloquee par une location (annulation, retour ou compensation). Idempotent. */
    public synchronized void libererReservation(Long id, Long locationId) {
        Vehicule v = trouver(id);
        boolean retire = v.getReservations().removeIf(p -> p.locationId().equals(locationId));
        log.info("Vehicule {} : liberation de la location {} ({})", v.getImmatriculation(), locationId,
                retire ? "ok" : "deja liberee");
    }

    // ------------------------------------------------------------------ utilitaires

    private void changerStatutManuel(Vehicule v, StatutVehicule nouveau) {
        if (nouveau == StatutVehicule.LOUE || v.getStatut() == StatutVehicule.LOUE) {
            throw new ConflictException("Le statut LOUE est gere automatiquement par les locations (depart / retour)");
        }
        v.setStatut(nouveau);
    }

    private void valider(VehiculeRequest req) {
        int anneeCourante = Year.now().getValue();
        if (req.annee() > anneeCourante) {
            throw new BusinessException("L'annee (" + req.annee() + ") ne peut pas depasser l'annee courante (" + anneeCourante + ")");
        }
        if (req.annee() < properties.anneeMin()) {
            throw new BusinessException("L'annee doit etre superieure ou egale a " + properties.anneeMin());
        }
        if (req.prixJournalier().signum() <= 0) {
            throw new BusinessException("Le prix journalier doit etre strictement positif");
        }
    }

    private void verifierPeriode(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null) {
            throw new BusinessException("Les dates de debut et de fin sont obligatoires");
        }
        if (fin.isBefore(debut)) {
            throw new BusinessException("La date de fin doit etre posterieure ou egale a la date de debut");
        }
    }

    private void appliquer(Vehicule v, VehiculeRequest req) {
        v.setImmatriculation(req.immatriculation().trim().toUpperCase());
        v.setMarque(req.marque().trim());
        v.setModele(req.modele().trim());
        v.setAnnee(req.annee());
        v.setCouleur(req.couleur());
        v.setCategorie(req.categorie());
        v.setCarburant(req.carburant());
        v.setBoite(req.boite());
        v.setNbPlaces(req.nbPlaces());
        v.setKilometrage(req.kilometrage());
        v.setPrixJournalier(req.prixJournalier());
        v.setImageUrl(req.imageUrl() == null || req.imageUrl().isBlank()
                ? "assets/vehicules/" + req.categorie().name().toLowerCase() + ".svg"
                : req.imageUrl().trim());
    }
}
