package tn.lim3.driveeasy.location.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.lim3.driveeasy.location.client.*;
import tn.lim3.driveeasy.location.config.LocationProperties;
import tn.lim3.driveeasy.location.dto.*;
import tn.lim3.driveeasy.location.exception.*;
import tn.lim3.driveeasy.location.model.Location;
import tn.lim3.driveeasy.location.model.StatutLocation;
import tn.lim3.driveeasy.location.repository.LocationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

import java.util.stream.Collectors;

/**
 * Cycle de vie des locations et orchestration des microservices.
 *
 * Coherence en cas d'echec d'un appel inter-services (pattern "compensation") :
 *  - creation : le vehicule est bloque dans vehicule-service AVANT l'enregistrement local ; si l'enregistrement
 *    ou l'appel echoue, la periode est liberee (DELETE /vehicules/{id}/reservations/{locationId}) ;
 *  - depart / retour / annulation : l'etat local n'est modifie qu'APRES le succes de l'appel distant ;
 *    si l'etape locale echoue ensuite, l'appel distant est annule (statut du vehicule restaure).
 */
@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final LocationRepository repository;
    private final ClientServiceClient clientService;
    private final VehiculeServiceClient vehiculeService;
    private final TarificationService tarification;
    private final LocationProperties properties;

    public LocationService(LocationRepository repository, ClientServiceClient clientService,
                           VehiculeServiceClient vehiculeService, TarificationService tarification,
                           LocationProperties properties) {
        this.repository = repository;
        this.clientService = clientService;
        this.vehiculeService = vehiculeService;
        this.tarification = tarification;
        this.properties = properties;
    }

    // ================================================================== consultation

    /** Liste paginee et filtree (statut, client, vehicule, periode qui chevauche [dateDebut, dateFin]). */
    public PageResponse<LocationResponse> rechercher(StatutLocation statut, Long clientId, Long vehiculeId,
                                                     LocalDate dateDebut, LocalDate dateFin,
                                                     Integer page, Integer size) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size < 1 ? properties.taillePageDefaut() : Math.min(size, 200);
        List<Location> filtrees = repository.findAll().stream()
                .filter(l -> statut == null || l.getStatut() == statut)
                .filter(l -> clientId == null || l.getClientId().equals(clientId))
                .filter(l -> vehiculeId == null || l.getVehiculeId().equals(vehiculeId))
                .filter(l -> dateFin == null || !l.getDateDebut().isAfter(dateFin))
                .filter(l -> dateDebut == null || !finReelle(l).isBefore(dateDebut))
                .toList();
        int total = filtrees.size();
        List<LocationResponse> contenu = filtrees.stream().skip((long) p * s).limit(s)
                .map(LocationResponse::from).toList();
        return new PageResponse<>(contenu, p, s, total, (int) Math.ceil(total / (double) s));
    }

    public Location trouver(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location introuvable (id=" + id + ")"));
    }

    /** Detail enrichi : agrege les donnees du client et du vehicule (si un service est indisponible, avertissement). */
    public LocationDetailResponse detail(Long id) {
        Location l = trouver(id);
        List<String> avertissements = new ArrayList<>();
        ClientDto client = null;
        VehiculeDto vehicule = null;
        try {
            client = clientService.client(l.getClientId());
        } catch (RuntimeException e) {
            avertissements.add("Client non disponible : " + e.getMessage());
        }
        try {
            vehicule = vehiculeService.vehicule(l.getVehiculeId());
        } catch (RuntimeException e) {
            avertissements.add("Vehicule non disponible : " + e.getMessage());
        }
        return new LocationDetailResponse(LocationResponse.from(l), client, vehicule, avertissements);
    }

    public long compterActives(Long clientId) {
        return repository.findAll().stream()
                .filter(Location::isActive)
                .filter(l -> clientId == null || l.getClientId().equals(clientId))
                .count();
    }

    // ================================================================== simulation & creation

    /** Simulation du prix sans creation (n'echoue pas si le client est inegible : renvoie des avertissements). */
    public SimulationResponse simuler(LocationRequest req) {
        verifierDates(req.dateDebut(), req.dateFin(), false);
        EligibiliteDto elig = clientService.eligibilite(req.clientId());
        VehiculeDto vehicule = vehiculeService.vehicule(req.vehiculeId());
        List<String> avertissements = new ArrayList<>(elig.motifs() == null ? List.of() : elig.motifs());
        boolean dispo = "DISPONIBLE".equals(vehicule.statut());
        if (!dispo) {
            avertissements.add("Vehicule actuellement " + vehicule.statut());
        }
        long nbJours = tarification.nbJours(req.dateDebut(), req.dateFin());
        TarificationService.Devis d = tarification.calculer(nbJours, vehicule.prixJournalier(), vehicule.categorie(),
                elig.categorieClient());
        return new SimulationResponse(req.clientId(), req.vehiculeId(), vehicule.libelle(), vehicule.categorie(),
                elig.categorieClient(), req.dateDebut(), req.dateFin(), d.nbJours(), d.prixJournalier(), d.coefficient(),
                d.sousTotal(), d.tauxRemise(), d.montantRemise(), d.total(), elig.eligible(), dispo, avertissements);
    }

    /**
     * Creation d'une reservation :
     * 1. client eligible (age >= 21, permis valide) - appel client-service ;
     * 2. vehicule existant et DISPONIBLE - appel vehicule-service ;
     * 3. calcul du prix ;
     * 4. blocage atomique de la periode dans vehicule-service (409 si deja reserve) ;
     * 5. enregistrement local ; en cas d'echec -> compensation (liberation de la periode).
     */
    public Location creer(LocationRequest req) {
        verifierDates(req.dateDebut(), req.dateFin(), true);

        EligibiliteDto elig = clientService.eligibilite(req.clientId());
        if (!elig.eligible()) {
            throw new BusinessException("Client non eligible a la location : " + String.join(" ; ", elig.motifs()));
        }
        VehiculeDto vehicule = vehiculeService.vehicule(req.vehiculeId());
        if (!"DISPONIBLE".equals(vehicule.statut())) {
            throw new ConflictException("Le vehicule " + vehicule.immatriculation() + " est " + vehicule.statut()
                    + " et ne peut pas etre reserve");
        }

        long nbJours = tarification.nbJours(req.dateDebut(), req.dateFin());
        TarificationService.Devis devis = tarification.calculer(nbJours, vehicule.prixJournalier(),
                vehicule.categorie(), elig.categorieClient());

        Long id = repository.nextId();
        Location l = new Location();
        l.setId(id);
        l.setClientId(req.clientId());
        l.setVehiculeId(req.vehiculeId());
        l.setDateDebut(req.dateDebut());
        l.setDateFinPrevue(req.dateFin());
        l.setPrixJournalierApplique(vehicule.prixJournalier());
        l.setCoefficientCategorie(devis.coefficient());
        l.setTauxRemise(devis.tauxRemise());
        l.setMontantRemise(devis.montantRemise());
        l.setMontantBase(devis.total());
        l.setPenalite(BigDecimal.ZERO);
        l.setMontantTotal(devis.total());
        l.setStatut(StatutLocation.RESERVEE);
        l.setDateCreation(LocalDateTime.now());

        try {
            vehiculeService.reserver(req.vehiculeId(), id, req.dateDebut(), req.dateFin());
        } catch (ServiceUnavailableException e) {
            // Etat incertain (ex. delai depasse) : on tente une liberation idempotente par securite
            compenserReservation(req.vehiculeId(), id);
            throw e;
        }

        try {
            Location creee = repository.save(l);
            log.info("Location {} creee : client {} / vehicule {} du {} au {} = {} TND", id, req.clientId(),
                    req.vehiculeId(), req.dateDebut(), req.dateFin(), devis.total());
            return creee;
        } catch (RuntimeException e) {
            compenserReservation(req.vehiculeId(), id);
            throw e;
        }
    }

    // ================================================================== cycle de vie

    /** Depart : RESERVEE -> EN_COURS, vehicule -> LOUE, releve du kilometrage de depart. */
    public synchronized Location depart(Long id) {
        Location l = trouver(id);
        if (l.getStatut() != StatutLocation.RESERVEE) {
            throw new ConflictException("Depart impossible : la location est " + l.getStatut());
        }
        VehiculeDto vehicule = vehiculeService.changerStatut(l.getVehiculeId(), "LOUE", null);
        try {
            l.setKmDepart(vehicule.kilometrage());
            l.setStatut(StatutLocation.EN_COURS);
            log.info("Location {} : depart du vehicule {} ({} km)", id, vehicule.immatriculation(), vehicule.kilometrage());
            return repository.save(l);
        } catch (RuntimeException e) {
            // compensation : le vehicule redevient disponible
            l.setStatut(StatutLocation.RESERVEE);
            l.setKmDepart(null);
            try {
                vehiculeService.changerStatut(l.getVehiculeId(), "DISPONIBLE", null);
            } catch (RuntimeException ex) {
                log.error("Compensation impossible pour le depart de la location {}", id, ex);
            }
            throw e;
        }
    }

    /**
     * Retour : EN_COURS -> TERMINEE. Calcul final : penalite = 1,5 x prix journalier x jours de retard.
     * Le vehicule repasse DISPONIBLE avec son nouveau kilometrage, puis la fidelite du client est recalculee.
     */
    public synchronized Location retour(Long id, RetourRequest req) {
        Location l = trouver(id);
        if (l.getStatut() != StatutLocation.EN_COURS) {
            throw new ConflictException("Retour impossible : la location est " + l.getStatut());
        }
        LocalDate dateRetour = req.dateRetourEffectif() != null ? req.dateRetourEffectif() : LocalDate.now();
        if (dateRetour.isBefore(l.getDateDebut())) {
            throw new BusinessException("La date de retour ne peut pas preceder la date de debut (" + l.getDateDebut() + ")");
        }
        if (l.getKmDepart() != null && req.kmRetour() < l.getKmDepart()) {
            throw new BusinessException("Le kilometrage de retour (" + req.kmRetour()
                    + ") est inferieur au kilometrage de depart (" + l.getKmDepart() + ")");
        }

        long retard = tarification.joursDeRetard(l.getDateFinPrevue(), dateRetour);
        BigDecimal penalite = tarification.penalite(l.getPrixJournalierApplique(), retard);

        // 1) appels distants d'abord : l'etat local n'est modifie qu'en cas de succes
        vehiculeService.liberer(l.getVehiculeId(), id);
        vehiculeService.changerStatut(l.getVehiculeId(), "DISPONIBLE", req.kmRetour());

        // 2) mise a jour locale
        l.setDateRetourEffectif(dateRetour);
        l.setKmRetour(req.kmRetour());
        l.setPenalite(penalite);
        l.setMontantTotal(l.getMontantBase().add(penalite));
        l.setStatut(StatutLocation.TERMINEE);
        repository.save(l);
        log.info("Location {} terminee : {} jour(s) de retard, penalite {} TND, total {} TND", id, retard, penalite,
                l.getMontantTotal());

        // 3) fidelite (non bloquant : la location reste terminee meme si client-service ne repond pas)
        mettreAJourFidelite(l.getClientId());
        return l;
    }

    /** Annulation : uniquement une reservation (RESERVEE). La periode du vehicule est liberee. */
    public synchronized Location annuler(Long id) {
        Location l = trouver(id);
        if (l.getStatut() != StatutLocation.RESERVEE) {
            throw new ConflictException("Annulation impossible : seule une reservation peut etre annulee (statut actuel "
                    + l.getStatut() + ")");
        }
        vehiculeService.liberer(l.getVehiculeId(), id);
        l.setStatut(StatutLocation.ANNULEE);
        log.info("Location {} annulee", id);
        return repository.save(l);
    }

    // ================================================================== statistiques

    public StatistiquesResponse statistiques() {
        List<Location> toutes = repository.findAll();
        List<String> avertissements = new ArrayList<>();
        LocalDate aujourdhui = LocalDate.now();
        YearMonth moisCourant = YearMonth.from(aujourdhui);

        List<Location> terminees = toutes.stream().filter(l -> l.getStatut() == StatutLocation.TERMINEE).toList();
        BigDecimal caTotal = somme(terminees);
        BigDecimal caMois = somme(terminees.stream()
                .filter(l -> YearMonth.from(l.getDateRetourEffectif()).equals(moisCourant)).toList());

        // CA des 6 derniers mois (graphique)
        List<StatistiquesResponse.CaMensuel> parMois = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth m = moisCourant.minusMonths(i);
            List<Location> duMois = terminees.stream()
                    .filter(l -> YearMonth.from(l.getDateRetourEffectif()).equals(m)).toList();
            parMois.add(new StatistiquesResponse.CaMensuel(m.toString(), somme(duMois), duMois.size()));
        }

        Map<String, Long> parStatut = new LinkedHashMap<>();
        for (StatutLocation s : StatutLocation.values()) {
            parStatut.put(s.name(), toutes.stream().filter(l -> l.getStatut() == s).count());
        }
        long enRetard = toutes.stream()
                .filter(l -> l.getStatut() == StatutLocation.EN_COURS && aujourdhui.isAfter(l.getDateFinPrevue()))
                .count();

        // Donnees du parc (appel vehicule-service) - degradation propre en cas de panne
        Integer nbVehicules = null;
        Map<String, Long> vehiculesParStatut = new LinkedHashMap<>();
        BigDecimal tauxOccupation = null;
        BigDecimal taux30 = null;
        Map<Long, VehiculeDto> parc = new HashMap<>();
        try {
            List<VehiculeDto> vehicules = vehiculeService.tous();
            vehicules.forEach(v -> parc.put(v.id(), v));
            nbVehicules = vehicules.size();
            for (String s : List.of("DISPONIBLE", "LOUE", "MAINTENANCE")) {
                vehiculesParStatut.put(s, vehicules.stream().filter(v -> s.equals(v.statut())).count());
            }
            if (nbVehicules > 0) {
                tauxOccupation = pourcentage(vehiculesParStatut.get("LOUE"), nbVehicules);
                taux30 = pourcentage(joursLouesSur30Jours(toutes, aujourdhui), nbVehicules * 30L);
            }
        } catch (RuntimeException e) {
            avertissements.add("Indicateurs du parc indisponibles : " + e.getMessage());
        }

        // Top 5 des vehicules les plus loues (hors annulations)
        Map<Long, List<Location>> parVehicule = toutes.stream()
                .filter(l -> l.getStatut() != StatutLocation.ANNULEE)
                .collect(Collectors.groupingBy(Location::getVehiculeId));
        List<StatistiquesResponse.TopVehicule> top = parVehicule.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Long, List<Location>>>comparingInt(e -> e.getValue().size()).reversed()
                        .thenComparingLong(Map.Entry::getKey))
                .limit(5)
                .map(e -> {
                    VehiculeDto v = parc.get(e.getKey());
                    return new StatistiquesResponse.TopVehicule(e.getKey(),
                            v != null ? v.libelle() : "Vehicule #" + e.getKey(),
                            v != null ? v.immatriculation() : null,
                            e.getValue().size(),
                            somme(e.getValue().stream().filter(l -> l.getStatut() == StatutLocation.TERMINEE).toList()));
                })
                .toList();

        return new StatistiquesResponse(caTotal, caMois, parMois, parStatut, parStatut.get("EN_COURS"), enRetard,
                nbVehicules, vehiculesParStatut, tauxOccupation, taux30, top, avertissements);
    }

    // ================================================================== utilitaires

    private void mettreAJourFidelite(Long clientId) {
        long nbTerminees = repository.findAll().stream()
                .filter(l -> l.getClientId().equals(clientId) && l.getStatut() == StatutLocation.TERMINEE)
                .count();
        String cible = tarification.categoriePour(nbTerminees);
        try {
            ClientDto client = clientService.client(clientId);
            if (rang(cible) > rang(client.categorieClient())) {
                clientService.changerCategorie(clientId, cible);
                log.info("Client {} promu {} ({} locations terminees)", clientId, cible, nbTerminees);
            }
        } catch (RuntimeException e) {
            log.warn("Mise a jour de la fidelite du client {} reportee : {}", clientId, e.getMessage());
        }
    }

    private static int rang(String categorie) {
        return switch (categorie == null ? "STANDARD" : categorie) {
            case "VIP" -> 2;
            case "FIDELE" -> 1;
            default -> 0;
        };
    }

    private void compenserReservation(Long vehiculeId, Long locationId) {
        try {
            vehiculeService.liberer(vehiculeId, locationId);
            log.warn("Compensation : periode du vehicule {} liberee pour la location {}", vehiculeId, locationId);
        } catch (RuntimeException ex) {
            log.error("Compensation impossible (vehicule {}, location {})", vehiculeId, locationId, ex);
        }
    }

    private void verifierDates(LocalDate debut, LocalDate fin, boolean creation) {
        if (fin.isBefore(debut)) {
            throw new BusinessException("La date de fin doit etre posterieure ou egale a la date de debut");
        }
        if (creation && debut.isBefore(LocalDate.now())) {
            throw new BusinessException("La date de debut ne peut pas etre dans le passe");
        }
    }

    private static LocalDate finReelle(Location l) {
        return l.getDateRetourEffectif() != null ? l.getDateRetourEffectif() : l.getDateFinPrevue();
    }

    /** Jours-vehicule loues sur les 30 derniers jours (EN_COURS et TERMINEE). */
    private static long joursLouesSur30Jours(List<Location> locations, LocalDate aujourdhui) {
        LocalDate debutFenetre = aujourdhui.minusDays(30);
        return locations.stream()
                .filter(l -> l.getStatut() == StatutLocation.EN_COURS || l.getStatut() == StatutLocation.TERMINEE)
                .mapToLong(l -> {
                    LocalDate fin = l.getStatut() == StatutLocation.EN_COURS ? aujourdhui : finReelle(l);
                    LocalDate d = l.getDateDebut().isBefore(debutFenetre) ? debutFenetre : l.getDateDebut();
                    LocalDate f = fin.isAfter(aujourdhui) ? aujourdhui : fin;
                    return Math.max(0, ChronoUnit.DAYS.between(d, f));
                })
                .sum();
    }

    private static BigDecimal pourcentage(long valeur, long total) {
        return BigDecimal.valueOf(valeur * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }

    private static BigDecimal somme(List<Location> locations) {
        return locations.stream().map(Location::getMontantTotal).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
