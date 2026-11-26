import { Client } from './client';
import { Vehicule } from './vehicule';

export type StatutLocation = 'RESERVEE' | 'EN_COURS' | 'TERMINEE' | 'ANNULEE';
export const STATUTS_LOCATION: StatutLocation[] = ['RESERVEE', 'EN_COURS', 'TERMINEE', 'ANNULEE'];

export interface Location {
  id: number;
  clientId: number;
  vehiculeId: number;
  dateDebut: string;
  dateFinPrevue: string;
  dateRetourEffectif?: string;
  nbJours: number;
  kmDepart?: number;
  kmRetour?: number;
  kmParcourus?: number;
  prixJournalierApplique: number;
  coefficientCategorie: number;
  tauxRemise: number;
  montantRemise: number;
  montantBase: number;
  penalite: number;
  montantTotal: number;
  statut: StatutLocation;
  enRetard: boolean;
  dateCreation: string;
}

export interface LocationDetail {
  location: Location;
  client?: Client;
  vehicule?: Vehicule;
  avertissements: string[];
}

export interface LocationRequest {
  clientId: number;
  vehiculeId: number;
  dateDebut: string;
  dateFin: string;
}

export interface Simulation {
  clientId: number;
  vehiculeId: number;
  vehicule: string;
  categorieVehicule: string;
  categorieClient: string;
  dateDebut: string;
  dateFin: string;
  nbJours: number;
  prixJournalier: number;
  coefficientCategorie: number;
  sousTotal: number;
  tauxRemise: number;
  montantRemise: number;
  montantTotal: number;
  clientEligible: boolean;
  vehiculeDisponible: boolean;
  avertissements: string[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface LocationFiltres {
  statut?: StatutLocation | '';
  clientId?: number | null;
  vehiculeId?: number | null;
  dateDebut?: string;
  dateFin?: string;
  page?: number;
  size?: number;
}

export interface Statistiques {
  chiffreAffairesTotal: number;
  chiffreAffairesMois: number;
  chiffreAffairesParMois: { mois: string; montant: number; nbLocations: number }[];
  locationsParStatut: Record<StatutLocation, number>;
  locationsEnCours: number;
  locationsEnRetard: number;
  nbVehicules?: number;
  vehiculesParStatut: Record<string, number>;
  tauxOccupation?: number;
  tauxOccupation30Jours?: number;
  topVehicules: { vehiculeId: number; libelle: string; immatriculation?: string; nbLocations: number; chiffreAffaires: number }[];
  avertissements: string[];
}
