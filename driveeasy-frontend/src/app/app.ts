import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { LoadingService } from './core/services/loading.service';
import { NotificationService } from './core/services/notification.service';

/** Coquille de l'application : menu lateral, barre de chargement, notifications. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly loading = inject(LoadingService);
  protected readonly notifications = inject(NotificationService);
  protected readonly menu = [
    { lien: '/dashboard', libelle: 'Tableau de bord', icone: '▦' },
    { lien: '/vehicules', libelle: 'Véhicules', icone: '◈' },
    { lien: '/clients', libelle: 'Clients', icone: '◉' },
    { lien: '/locations', libelle: 'Locations', icone: '⇄' },
  ];
}
