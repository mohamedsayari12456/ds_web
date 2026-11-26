import { TndPipe } from '../../shared/tnd.pipe';
import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Location, LocationFiltres, Page, STATUTS_LOCATION } from '../../core/models/location';
import { Client } from '../../core/models/client';
import { Vehicule } from '../../core/models/vehicule';
import { LocationService } from '../../core/services/location.service';
import { ClientService } from '../../core/services/client.service';
import { VehiculeService } from '../../core/services/vehicule.service';
import { NotificationService } from '../../core/services/notification.service';
import { Pagination } from '../../shared/pagination';
import { LibellePipe } from '../../shared/libelle.pipe';

/** /locations : liste paginee (serveur) filtrable par statut et periode ; actions depart / retour / annulation. */
@Component({
  selector: 'app-location-list',
  imports: [TndPipe, FormsModule, RouterLink, DatePipe, Pagination, LibellePipe],
  templateUrl: './location-list.html',
})
export class LocationList implements OnInit {
  private readonly locationService = inject(LocationService);
  private readonly clientService = inject(ClientService);
  private readonly vehiculeService = inject(VehiculeService);
  private readonly notifications = inject(NotificationService);

  protected readonly statuts = STATUTS_LOCATION;
  protected filtres: LocationFiltres = { statut: '', dateDebut: '', dateFin: '', page: 0, size: 10 };
  protected readonly resultat = signal<Page<Location> | null>(null);
  protected readonly clients = signal<Map<number, Client>>(new Map());
  protected readonly vehicules = signal<Map<number, Vehicule>>(new Map());

  ngOnInit(): void {
    forkJoin({ c: this.clientService.rechercher(), v: this.vehiculeService.lister() }).subscribe(({ c, v }) => {
      this.clients.set(new Map(c.map((x) => [x.id, x])));
      this.vehicules.set(new Map(v.map((x) => [x.id, x])));
    });
    this.charger();
  }

  charger(page = 0): void {
    this.filtres.page = page;
    this.locationService.lister(this.filtres).subscribe((r) => this.resultat.set(r));
  }

  reinitialiser(): void {
    this.filtres = { statut: '', dateDebut: '', dateFin: '', page: 0, size: 10 };
    this.charger();
  }

  depart(l: Location): void {
    if (!confirm(`Confirmer le départ du véhicule pour la location #${l.id} ?`)) return;
    this.locationService.depart(l.id).subscribe(() => {
      this.notifications.success(`Location #${l.id} démarrée : véhicule passé au statut LOUÉ`);
      this.charger(this.filtres.page);
    });
  }

  annuler(l: Location): void {
    if (!confirm(`Annuler la réservation #${l.id} ?`)) return;
    this.locationService.annuler(l.id).subscribe(() => {
      this.notifications.success(`Réservation #${l.id} annulée`);
      this.charger(this.filtres.page);
    });
  }

  vehicule(id: number): string {
    const v = this.vehicules().get(id);
    return v ? `${v.marque} ${v.modele}` : `#${id}`;
  }
}
