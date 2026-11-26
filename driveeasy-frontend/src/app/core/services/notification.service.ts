import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  type: 'success' | 'error' | 'info';
  message: string;
}

/** Notifications ephemeres (toasts) affichees en haut a droite. */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private compteur = 0;
  readonly toasts = signal<Toast[]>([]);

  success(message: string): void { this.ajouter('success', message); }
  error(message: string): void { this.ajouter('error', message, 6000); }
  info(message: string): void { this.ajouter('info', message); }

  fermer(id: number): void {
    this.toasts.update((t) => t.filter((x) => x.id !== id));
  }

  private ajouter(type: Toast['type'], message: string, duree = 3500): void {
    const id = ++this.compteur;
    this.toasts.update((t) => [...t, { id, type, message }]);
    setTimeout(() => this.fermer(id), duree);
  }
}
