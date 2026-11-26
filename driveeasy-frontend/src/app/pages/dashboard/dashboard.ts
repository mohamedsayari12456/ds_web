import { TndPipe } from '../../shared/tnd.pipe';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { forkJoin } from 'rxjs';
import { Statistiques, Location } from '../../core/models/location';
import { Client } from '../../core/models/client';
import { Vehicule } from '../../core/models/vehicule';
import { LocationService } from '../../core/services/location.service';
import { ClientService } from '../../core/services/client.service';
import { VehiculeService } from '../../core/services/vehicule.service';
import { ChartComponent } from '../../shared/chart';

/** /dashboard : indicateurs du parc, chiffre d'affaires et locations en cours (graphiques Chart.js). */
@Component({
  selector: 'app-dashboard',
  imports: [TndPipe, ChartComponent, DatePipe, DecimalPipe, RouterLink],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private readonly locationService = inject(LocationService);
  private readonly clientService = inject(ClientService);
  private readonly vehiculeService = inject(VehiculeService);

  protected readonly stats = signal<Statistiques | null>(null);
  protected readonly enCours = signal<Location[]>([]);
  protected readonly clients = signal<Map<number, Client>>(new Map());
  protected readonly vehicules = signal<Map<number, Vehicule>>(new Map());

  protected readonly graphiqueCa = computed<ChartConfiguration | null>(() => {
    const s = this.stats();
    if (!s) return null;
    return {
      type: 'bar',
      data: {
        labels: s.chiffreAffairesParMois.map((m) => this.nomMois(m.mois)),
        datasets: [{ label: "Chiffre d'affaires (TND)", data: s.chiffreAffairesParMois.map((m) => m.montant),
          backgroundColor: '#e4572e', borderRadius: 6 }],
      },
      options: { plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } },
    };
  });

  protected readonly graphiqueParc = computed<ChartConfiguration | null>(() => {
    const s = this.stats();
    if (!s?.vehiculesParStatut) return null;
    return {
      type: 'doughnut',
      data: {
        labels: ['Disponibles', 'Loués', 'Maintenance'],
        datasets: [{ data: [s.vehiculesParStatut['DISPONIBLE'] ?? 0, s.vehiculesParStatut['LOUE'] ?? 0,
          s.vehiculesParStatut['MAINTENANCE'] ?? 0], backgroundColor: ['#16a34a', '#2563eb', '#d97706'] }],
      },
      options: { plugins: { legend: { position: 'bottom' } }, cutout: '62%' },
    };
  });

  protected readonly graphiqueStatuts = computed<ChartConfiguration | null>(() => {
    const s = this.stats();
    if (!s) return null;
    const statuts = ['RESERVEE', 'EN_COURS', 'TERMINEE', 'ANNULEE'] as const;
    return {
      type: 'bar',
      data: {
        labels: ['Réservées', 'En cours', 'Terminées', 'Annulées'],
        datasets: [{ data: statuts.map((k) => s.locationsParStatut[k] ?? 0),
          backgroundColor: ['#d97706', '#2563eb', '#16a34a', '#dc2626'], borderRadius: 6 }],
      },
      options: { indexAxis: 'y', plugins: { legend: { display: false } } },
    };
  });

  ngOnInit(): void {
    forkJoin({
      stats: this.locationService.statistiques(),
      enCours: this.locationService.lister({ statut: 'EN_COURS', size: 50 }),
      clients: this.clientService.rechercher(),
      vehicules: this.vehiculeService.lister(),
    }).subscribe(({ stats, enCours, clients, vehicules }) => {
      this.stats.set(stats);
      this.enCours.set(enCours.content);
      this.clients.set(new Map(clients.map((c) => [c.id, c])));
      this.vehicules.set(new Map(vehicules.map((v) => [v.id, v])));
    });
  }

  private nomMois(m: string): string {
    const [annee, mois] = m.split('-').map(Number);
    return new Date(annee, mois - 1, 1).toLocaleDateString('fr-FR', { month: 'short', year: '2-digit' });
  }
}
