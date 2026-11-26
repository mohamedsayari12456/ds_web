import { Injectable, computed, signal } from '@angular/core';

/** Compteur de requetes HTTP en cours (barre de chargement globale). */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly enCours = signal(0);
  readonly actif = computed(() => this.enCours() > 0);

  debut(): void { this.enCours.update((n) => n + 1); }
  fin(): void { this.enCours.update((n) => Math.max(0, n - 1)); }
}
