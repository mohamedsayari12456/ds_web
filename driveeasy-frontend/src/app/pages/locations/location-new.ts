import { TndPipe } from '../../shared/tnd.pipe';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Client, Eligibilite } from '../../core/models/client';
import { Location, Simulation } from '../../core/models/location';
import { CATEGORIES_VEHICULE, Vehicule } from '../../core/models/vehicule';
import { ClientService } from '../../core/services/client.service';
import { LocationService } from '../../core/services/location.service';
import { VehiculeService } from '../../core/services/vehicule.service';
import { LibellePipe } from '../../shared/libelle.pipe';
import { aujourdhui } from '../../shared/libelles';

/**
 * /locations/new : assistant en 5 etapes
 * client -> periode -> vehicule disponible -> simulation du prix -> confirmation.
 */
@Component({
  selector: 'app-location-new',
  imports: [TndPipe, FormsModule, RouterLink, DatePipe, DecimalPipe, LibellePipe],
  templateUrl: './location-new.html',
  styleUrl: './locations.css',
})
export class LocationNew implements OnInit {
  private readonly clientService = inject(ClientService);
  private readonly vehiculeService = inject(VehiculeService);
  private readonly locationService = inject(LocationService);
  private readonly route = inject(ActivatedRoute);

  protected readonly etapes = ['Client', 'Période', 'Véhicule', 'Simulation', 'Confirmation'];
  protected readonly etape = signal(0);

  // Etape 1 : client
  protected recherche = '';
  protected readonly clients = signal<Client[]>([]);
  protected readonly client = signal<Client | null>(null);
  protected readonly eligibilite = signal<Eligibilite | null>(null);

  // Etape 2 : periode
  protected readonly minDate = aujourdhui();
  protected dateDebut = aujourdhui(1);
  protected dateFin = aujourdhui(4);
  protected readonly nbJours = signal(3);

  // Etape 3 : vehicule
  protected readonly disponibles = signal<Vehicule[]>([]);
  protected readonly vehicule = signal<Vehicule | null>(null);
  protected categorie = '';
  protected readonly categories = CATEGORIES_VEHICULE;

  // Etapes 4-5
  protected readonly simulation = signal<Simulation | null>(null);
  protected readonly creee = signal<Location | null>(null);
  protected readonly envoi = signal(false);

  protected readonly peutContinuer = computed(() => {
    switch (this.etape()) {
      case 0: return !!this.client() && !!this.eligibilite()?.eligible;
      case 2: return !!this.vehicule();
      case 3: return !!this.simulation();
      default: return true;
    }
  });

  ngOnInit(): void {
    this.chercherClients();
    const clientId = Number(this.route.snapshot.queryParamMap.get('clientId'));
    if (clientId) {
      this.clientService.detail(clientId).subscribe((c) => this.choisirClient(c));
    }
  }

  chercherClients(): void {
    this.clientService.rechercher(this.recherche).subscribe((c) => this.clients.set(c));
  }

  choisirClient(c: Client): void {
    this.client.set(c);
    this.eligibilite.set(null);
    this.clientService.eligibilite(c.id).subscribe((e) => this.eligibilite.set(e));
  }

  periodeValide(): boolean {
    return !!this.dateDebut && !!this.dateFin && this.dateDebut >= this.minDate && this.dateFin >= this.dateDebut;
  }

  calculerJours(): void {
    if (this.periodeValide()) {
      const ms = new Date(this.dateFin).getTime() - new Date(this.dateDebut).getTime();
      this.nbJours.set(Math.max(1, Math.round(ms / 86_400_000)));
    }
  }

  chercherVehicules(): void {
    this.vehicule.set(null);
    this.vehiculeService.disponibles(this.dateDebut, this.dateFin, this.categorie)
      .subscribe((v) => this.disponibles.set(v));
  }

  suivant(): void {
    const e = this.etape();
    if (e === 1) {
      if (!this.periodeValide()) return;
      this.chercherVehicules();
    }
    if (e === 2) {
      this.simulation.set(null);
      this.locationService.simuler(this.requete()).subscribe((s) => this.simulation.set(s));
    }
    this.etape.set(e + 1);
  }

  precedent(): void {
    this.etape.update((e) => Math.max(0, e - 1));
  }

  confirmer(): void {
    this.envoi.set(true);
    this.locationService.creer(this.requete()).subscribe({
      next: (l) => this.creee.set(l),
      error: () => this.envoi.set(false),
    });
  }

  recommencer(): void {
    this.client.set(null);
    this.eligibilite.set(null);
    this.vehicule.set(null);
    this.simulation.set(null);
    this.creee.set(null);
    this.envoi.set(false);
    this.etape.set(0);
  }

  private requete() {
    return { clientId: this.client()!.id, vehiculeId: this.vehicule()!.id, dateDebut: this.dateDebut, dateFin: this.dateFin };
  }
}
