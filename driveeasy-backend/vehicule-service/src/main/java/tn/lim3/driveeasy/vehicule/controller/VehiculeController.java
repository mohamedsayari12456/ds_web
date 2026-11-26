package tn.lim3.driveeasy.vehicule.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.lim3.driveeasy.vehicule.dto.ReservationRequest;
import tn.lim3.driveeasy.vehicule.dto.StatutRequest;
import tn.lim3.driveeasy.vehicule.dto.VehiculeRequest;
import tn.lim3.driveeasy.vehicule.dto.VehiculeResponse;
import tn.lim3.driveeasy.vehicule.model.Carburant;
import tn.lim3.driveeasy.vehicule.model.Categorie;
import tn.lim3.driveeasy.vehicule.model.StatutVehicule;
import tn.lim3.driveeasy.vehicule.model.Vehicule;
import tn.lim3.driveeasy.vehicule.service.VehiculeService;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** API REST du parc automobile. Exposee via la gateway sous /api/vehicules/**. */
@RestController
@RequestMapping("/vehicules")
public class VehiculeController {

    private final VehiculeService service;

    public VehiculeController(VehiculeService service) {
        this.service = service;
    }

    /** GET /vehicules?categorie=&carburant=&statut=&prixMax=&q= */
    @GetMapping
    public List<VehiculeResponse> lister(@RequestParam(required = false) Categorie categorie,
                                         @RequestParam(required = false) Carburant carburant,
                                         @RequestParam(required = false) StatutVehicule statut,
                                         @RequestParam(required = false) BigDecimal prixMax,
                                         @RequestParam(required = false) String q) {
        return map(service.rechercher(categorie, carburant, statut, prixMax, q));
    }

    /** GET /vehicules/disponibles?dateDebut=2026-12-01&dateFin=2026-12-05[&categorie=SUV] */
    @GetMapping("/disponibles")
    public List<VehiculeResponse> disponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) Categorie categorie) {
        return map(service.disponibles(dateDebut, dateFin, categorie));
    }

    /** GET /vehicules/categories : valeurs possibles des enums (utile au frontend). */
    @GetMapping("/categories")
    public List<String> categories() {
        return Arrays.stream(Categorie.values()).map(Enum::name).toList();
    }

    @GetMapping("/{id}")
    public VehiculeResponse detail(@PathVariable Long id) {
        return VehiculeResponse.from(service.trouver(id));
    }

    @PostMapping
    public ResponseEntity<VehiculeResponse> creer(@Valid @RequestBody VehiculeRequest req) {
        Vehicule cree = service.creer(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(cree.getId()).toUri();
        return ResponseEntity.created(location).body(VehiculeResponse.from(cree));
    }

    @PutMapping("/{id}")
    public VehiculeResponse modifier(@PathVariable Long id, @Valid @RequestBody VehiculeRequest req) {
        return VehiculeResponse.from(service.modifier(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }

    /** PATCH /vehicules/{id}/statut  {"statut":"LOUE","kilometrage":45000} */
    @PatchMapping("/{id}/statut")
    public VehiculeResponse changerStatut(@PathVariable Long id, @Valid @RequestBody StatutRequest req) {
        return VehiculeResponse.from(service.changerStatut(id, req));
    }

    /** POST /vehicules/{id}/reservations : bloque le vehicule sur une periode (appel de location-service). */
    @PostMapping("/{id}/reservations")
    public VehiculeResponse reserver(@PathVariable Long id, @Valid @RequestBody ReservationRequest req) {
        return VehiculeResponse.from(service.reserver(id, req));
    }

    /** DELETE /vehicules/{id}/reservations/{locationId} : libere la periode (annulation / retour / compensation). */
    @DeleteMapping("/{id}/reservations/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void liberer(@PathVariable Long id, @PathVariable Long locationId) {
        service.libererReservation(id, locationId);
    }

    private List<VehiculeResponse> map(List<Vehicule> vehicules) {
        return vehicules.stream().map(VehiculeResponse::from).toList();
    }
}
