import { TndPipe } from '../../shared/tnd.pipe';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LocationDetail } from '../../core/models/location';
import { LocationService } from '../../core/services/location.service';
import { NotificationService } from '../../core/services/notification.service';
import { LibellePipe } from '../../shared/libelle.pipe';
import { aujourdhui } from '../../shared/libelles';

/**
 * /locations/:id/retour : saisie du kilometrage, apercu des penalites,
 * puis recapitulatif final imprimable (calcul definitif fait par location-service).
 */
@Component({
  selector: 'app-location-retour',
  imports: [TndPipe, FormsModule, RouterLink, DatePipe, DecimalPipe, LibellePipe],
  templateUrl: './location-retour.html',
  styleUrl: './locations.css',
})
export class LocationRetour implements OnInit {
  readonly id = input.required<string>();

  private readonly service = inject(LocationService);
  private readonly notifications = inject(NotificationService);

  protected readonly detail = signal<LocationDetail | null>(null);
  protected readonly kmRetour = signal<number | null>(null);
  protected readonly dateRetour = signal(aujourdhui());
  protected readonly envoi = signal(false);

  /** Apercu cote client (le calcul de reference est fait par le serveur). */
  protected readonly joursRetard = computed(() => {
    const l = this.detail()?.location;
    if (!l) return 0;
    const ms = new Date(this.dateRetour()).getTime() - new Date(l.dateFinPrevue).getTime();
    return Math.max(0, Math.round(ms / 86_400_000));
  });
  protected readonly penaliteEstimee = computed(() =>
    (this.detail()?.location.prixJournalierApplique ?? 0) * 1.5 * this.joursRetard());
  protected readonly kmInvalide = computed(() => {
    const km = this.kmRetour();
    const depart = this.detail()?.location.kmDepart ?? 0;
    return km === null || km < depart;
  });

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.service.detail(Number(this.id())).subscribe((d) => {
      this.detail.set(d);
      if (this.kmRetour() === null) {
        this.kmRetour.set(d.location.kmDepart ?? d.vehicule?.kilometrage ?? 0);
      }
    });
  }

  valider(): void {
    if (this.kmInvalide()) return;
    this.envoi.set(true);
    this.service.retour(Number(this.id()), this.kmRetour()!, this.dateRetour()).subscribe({
      next: (l) => {
        this.notifications.success(`Retour enregistré : ${new TndPipe().transform(l.montantTotal)}`);
        this.envoi.set(false);
        this.charger();
      },
      error: () => this.envoi.set(false),
    });
  }

  imprimer(): void {
    window.print();
  }
}
