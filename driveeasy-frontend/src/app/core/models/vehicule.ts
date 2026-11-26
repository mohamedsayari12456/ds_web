export type Carburant = 'ESSENCE' | 'DIESEL' | 'HYBRIDE' | 'ELECTRIQUE' | 'GPL';
export type Boite = 'MANUELLE' | 'AUTOMATIQUE';
export type StatutVehicule = 'DISPONIBLE' | 'LOUE' | 'MAINTENANCE';
export type CategorieVehicule = 'ECONOMIQUE' | 'COMPACTE' | 'BERLINE' | 'SUV' | 'UTILITAIRE' | 'LUXE';

export const CARBURANTS: Carburant[] = ['ESSENCE', 'DIESEL', 'HYBRIDE', 'ELECTRIQUE', 'GPL'];
export const BOITES: Boite[] = ['MANUELLE', 'AUTOMATIQUE'];
export const STATUTS_VEHICULE: StatutVehicule[] = ['DISPONIBLE', 'LOUE', 'MAINTENANCE'];
export const CATEGORIES_VEHICULE: CategorieVehicule[] = ['ECONOMIQUE', 'COMPACTE', 'BERLINE', 'SUV', 'UTILITAIRE', 'LUXE'];

export interface PeriodeReservation {
  locationId: number;
  dateDebut: string;
  dateFin: string;
}

export interface Vehicule {
  id: number;
  immatriculation: string;
  marque: string;
  modele: string;
  annee: number;
  couleur?: string;
  categorie: CategorieVehicule;
  carburant: Carburant;
  boite: Boite;
  nbPlaces: number;
  kilometrage: number;
  prixJournalier: number;
  statut: StatutVehicule;
  imageUrl?: string;
  reservations?: PeriodeReservation[];
}

export type VehiculeRequest = Omit<Vehicule, 'id' | 'reservations'>;

export interface VehiculeFiltres {
  categorie?: CategorieVehicule | '';
  carburant?: Carburant | '';
  statut?: StatutVehicule | '';
  prixMax?: number | null;
  q?: string;
}
