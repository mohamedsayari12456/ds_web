package tn.lim3.driveeasy.location.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.lim3.driveeasy.location.dto.*;
import tn.lim3.driveeasy.location.model.Location;
import tn.lim3.driveeasy.location.model.StatutLocation;
import tn.lim3.driveeasy.location.service.LocationService;

import java.net.URI;
import java.time.LocalDate;

/** API REST des locations. Exposee via la gateway sous /api/locations/**. */
@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    /** GET /locations?statut=&clientId=&vehiculeId=&dateDebut=&dateFin=&page=0&size=10 */
    @GetMapping
    public PageResponse<LocationResponse> lister(
            @RequestParam(required = false) StatutLocation statut,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long vehiculeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return service.rechercher(statut, clientId, vehiculeId, dateDebut, dateFin, page, size);
    }

    /** GET /locations/{id} : detail enrichi (client + vehicule agreges). */
    @GetMapping("/{id}")
    public LocationDetailResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping
    public ResponseEntity<LocationResponse> creer(@Valid @RequestBody LocationRequest req) {
        Location creee = service.creer(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creee.getId()).toUri();
        return ResponseEntity.created(location).body(LocationResponse.from(creee));
    }

    /** POST /locations/simulation : calcul du prix sans creation. */
    @PostMapping("/simulation")
    public SimulationResponse simuler(@Valid @RequestBody LocationRequest req) {
        return service.simuler(req);
    }

    @PatchMapping("/{id}/depart")
    public LocationResponse depart(@PathVariable Long id) {
        return LocationResponse.from(service.depart(id));
    }

    @PatchMapping("/{id}/retour")
    public LocationResponse retour(@PathVariable Long id, @Valid @RequestBody RetourRequest req) {
        return LocationResponse.from(service.retour(id, req));
    }

    @PatchMapping("/{id}/annulation")
    public LocationResponse annuler(@PathVariable Long id) {
        return LocationResponse.from(service.annuler(id));
    }

    /** GET /locations/statistiques : CA, taux d'occupation, top vehicules. */
    @GetMapping("/statistiques")
    public StatistiquesResponse statistiques() {
        return service.statistiques();
    }

    /** GET /locations/actives/count?clientId= : utilise par client-service avant suppression d'un client. */
    @GetMapping("/actives/count")
    public long compterActives(@RequestParam(required = false) Long clientId) {
        return service.compterActives(clientId);
    }
}
