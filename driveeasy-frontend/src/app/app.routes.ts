import { Routes } from '@angular/router';
import { idParamGuard } from './core/guards/id-param.guard';
import { unsavedChangesGuard } from './core/guards/unsaved-changes.guard';

/** Routage : chaque ecran est charge a la demande (lazy loading de composants standalone). */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard', title: 'Tableau de bord - DriveEasy',
    loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'vehicules', title: 'Catalogue - DriveEasy',
    loadComponent: () => import('./pages/vehicules/vehicule-list').then((m) => m.VehiculeList),
  },
  {
    path: 'vehicules/new', title: 'Nouveau vehicule - DriveEasy',
    loadComponent: () => import('./pages/vehicules/vehicule-form').then((m) => m.VehiculeForm),
    canDeactivate: [unsavedChangesGuard],
  },
  {
    path: 'vehicules/:id/edit', title: 'Modifier un vehicule - DriveEasy',
    loadComponent: () => import('./pages/vehicules/vehicule-form').then((m) => m.VehiculeForm),
    canActivate: [idParamGuard], canDeactivate: [unsavedChangesGuard],
  },
  {
    path: 'clients', title: 'Clients - DriveEasy',
    loadComponent: () => import('./pages/clients/client-list').then((m) => m.ClientList),
  },
  {
    path: 'clients/new', title: 'Nouveau client - DriveEasy',
    loadComponent: () => import('./pages/clients/client-form').then((m) => m.ClientForm),
    canDeactivate: [unsavedChangesGuard],
  },
  {
    path: 'clients/:id/edit', title: 'Modifier un client - DriveEasy',
    loadComponent: () => import('./pages/clients/client-form').then((m) => m.ClientForm),
    canActivate: [idParamGuard], canDeactivate: [unsavedChangesGuard],
  },
  {
    path: 'clients/:id', title: 'Fiche client - DriveEasy',
    loadComponent: () => import('./pages/clients/client-detail').then((m) => m.ClientDetail),
    canActivate: [idParamGuard],
  },
  {
    path: 'locations', title: 'Locations - DriveEasy',
    loadComponent: () => import('./pages/locations/location-list').then((m) => m.LocationList),
  },
  {
    path: 'locations/new', title: 'Nouvelle location - DriveEasy',
    loadComponent: () => import('./pages/locations/location-new').then((m) => m.LocationNew),
  },
  {
    path: 'locations/:id/retour', title: 'Retour de vehicule - DriveEasy',
    loadComponent: () => import('./pages/locations/location-retour').then((m) => m.LocationRetour),
    canActivate: [idParamGuard],
  },
  { path: '**', redirectTo: 'dashboard' },
];
