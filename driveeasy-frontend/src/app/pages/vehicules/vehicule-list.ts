import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  CARBURANTS, CATEGORIES_VEHICULE, STATUTS_VEHICULE, Vehicule, VehiculeFiltres,
} from '../../core/models/vehicule';
import { VehiculeService } from '../../core/services/vehicule.service';
import { NotificationService } from '../../core/services/notification.service';
import { Pagination } from '../../shared/pagination';
import { LibellePipe } from '../../shared/libelle.pipe';

type Tri = 'marque' | 'prix-asc' | 'prix-desc' | 'annee-desc' | 'km-asc';

/** /vehicules : catalogue en grille de cartes, filtres (serveur), tri et pagination (client). */
@Component({
  selector: 'app-vehicule-list',
  imports: [FormsModule, RouterLink, DecimalPipe, Pagination, LibellePipe],
  templateUrl: './vehicule-list.html',
  styleUrl: './vehicules.css',
})
export class VehiculeList implements OnInit {
  private readonly service = inject(VehiculeService);
  private readonly notifications = inject(NotificationService);

  protected readonly carburants = CARBURANTS;
  protected readonly categories = CATEGORIES_VEHICULE;
  protected readonly statuts = STATUTS_VEHICULE;
  protected readonly taillePage = 8;

  protected filtres: VehiculeFiltres = { categorie: '', carburant: '', statut: '', prixMax: null, q: '' };
  protected readonly tri = signal<Tri>('marque');
  protected readonly page = signal(0);
  protected readonly vehicules = signal<Vehicule[]>([]);
  protected readonly charge = signal(false);

  protected readonly tries = computed(() => {
    const liste = [...this.vehicules()];
    const comparateurs: Record<Tri, (a: Vehicule, b: Vehicule) => number> = {
      'marque': (a, b) => `${a.marque} ${a.modele}`.localeCompare(`${b.marque} ${b.modele}`),
      'prix-asc': (a, b) => a.prixJournalier - b.prixJournalier,
      'prix-desc': (a, b) => b.prixJournalier - a.prixJournalier,
      'annee-desc': (a, b) => b.annee - a.annee,
      'km-asc': (a, b) => a.kilometrage - b.kilometrage,
    };
    return liste.sort(comparateurs[this.tri()]);
  });
  protected readonly nbPages = computed(() => Math.ceil(this.tries().length / this.taillePage));
  protected readonly pageCourante = computed(() =>
    this.tries().slice(this.page() * this.taillePage, (this.page() + 1) * this.taillePage));

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.service.lister(this.filtres).subscribe((v) => {
      this.vehicules.set(v);
      this.page.set(0);
      this.charge.set(true);
    });
  }

  reinitialiser(): void {
    this.filtres = { categorie: '', carburant: '', statut: '', prixMax: null, q: '' };
    this.charger();
  }

  changerTri(t: Tri): void {
    this.tri.set(t);
    this.page.set(0);
  }

  basculerMaintenance(v: Vehicule): void {
    const statut = v.statut === 'MAINTENANCE' ? 'DISPONIBLE' : 'MAINTENANCE';
    this.service.changerStatut(v.id, statut).subscribe(() => {
      this.notifications.success(`${v.marque} ${v.modele} : statut ${statut}`);
      this.charger();
    });
  }

  supprimer(v: Vehicule): void {
    if (!confirm(`Supprimer le véhicule ${v.marque} ${v.modele} (${v.immatriculation}) ?`)) return;
    this.service.supprimer(v.id).subscribe(() => {
      this.notifications.success('Véhicule supprimé');
      this.charger();
    });
  }

  imageParDefaut(event: Event, v: Vehicule): void {
    (event.target as HTMLImageElement).src = `assets/vehicules/${v.categorie.toLowerCase()}.svg`;
  }
}
