package tn.lim3.driveeasy.client.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.lim3.driveeasy.client.dto.CategorieRequest;
import tn.lim3.driveeasy.client.dto.ClientRequest;
import tn.lim3.driveeasy.client.dto.ClientResponse;
import tn.lim3.driveeasy.client.dto.EligibiliteResponse;
import tn.lim3.driveeasy.client.model.CategorieClient;
import tn.lim3.driveeasy.client.model.Client;
import tn.lim3.driveeasy.client.service.ClientService;

import java.net.URI;
import java.util.List;

/** API REST des clients. Exposee via la gateway sous /api/clients/**. */
@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    /** GET /clients?q=  (ou nom= / cin= / email= / categorie=) */
    @GetMapping
    public List<ClientResponse> rechercher(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) String nom,
                                           @RequestParam(required = false) String cin,
                                           @RequestParam(required = false) String email,
                                           @RequestParam(required = false) CategorieClient categorie) {
        return service.rechercher(q, nom, cin, email, categorie).stream().map(ClientResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ClientResponse detail(@PathVariable Long id) {
        return ClientResponse.from(service.trouver(id));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> creer(@Valid @RequestBody ClientRequest req) {
        Client cree = service.creer(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(cree.getId()).toUri();
        return ResponseEntity.created(location).body(ClientResponse.from(cree));
    }

    @PutMapping("/{id}")
    public ClientResponse modifier(@PathVariable Long id, @Valid @RequestBody ClientRequest req) {
        return ClientResponse.from(service.modifier(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        service.supprimer(id);
    }

    @GetMapping("/{id}/eligibilite")
    public EligibiliteResponse eligibilite(@PathVariable Long id) {
        return service.eligibilite(id);
    }

    @PatchMapping("/{id}/categorie")
    public ClientResponse changerCategorie(@PathVariable Long id, @Valid @RequestBody CategorieRequest req) {
        return ClientResponse.from(service.changerCategorie(id, req.categorie()));
    }
}
