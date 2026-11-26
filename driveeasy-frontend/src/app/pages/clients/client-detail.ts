import { TndPipe } from '../../shared/tnd.pipe';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Client, Eligibilite } from '../../core/models/client';
import { Location } from '../../core/models/location';
import { Vehicule } from '../../core/models/vehicule';
import { ClientService } from '../../core/services/client.service';
import { LocationService } from '../../core/services/location.service';
import { VehiculeService } from '../../core/services/vehicule.service';
import { LibellePipe } from '../../shared/libelle.pipe';

/** /clients/:id : informations, eligibilite (appel client-service) et historique des locations. */
@Component({
  selector: 'app-client-detail',
  imports: [TndPipe, RouterLink, DatePipe, LibellePipe],
  templateUrl: './client-detail.html',
})
export class ClientDetail implements OnInit {
  readonly id = input.required<string>();

  private readonly clientService = inject(ClientService);
  private readonly locationService = inject(LocationService);
  private readonly vehiculeService = inject(VehiculeService);

  protected readonly client = signal<Client | null>(null);
  protected readonly eligibilite = signal<Eligibilite | null>(null);
  protected readonly locations = signal<Location[]>([]);
  protected readonly vehicules = signal<Map<number, Vehicule>>(new Map());
  protected readonly nbTerminees = computed(() => this.locations().filter((l) => l.statut === 'TERMINEE').length);
  protected readonly totalDepense = computed(() =>
    this.locations().filter((l) => l.statut === 'TERMINEE').reduce((s, l) => s + l.montantTotal, 0));

  ngOnInit(): void {
    const id = Number(this.id());
    forkJoin({
      client: this.clientService.detail(id),
      eligibilite: this.clientService.eligibilite(id),
      locations: this.locationService.lister({ clientId: id, size: 200 }),
      vehicules: this.vehiculeService.lister(),
    }).subscribe((r) => {
      this.client.set(r.client);
      this.eligibilite.set(r.eligibilite);
      this.locations.set(r.locations.content);
      this.vehicules.set(new Map(r.vehicules.map((v) => [v.id, v])));
    });
  }
}
