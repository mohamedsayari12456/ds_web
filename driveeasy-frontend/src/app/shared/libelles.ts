/** Libelles lisibles des enums (affichage). */
export const LIBELLES: Record<string, string> = {
  DISPONIBLE: 'Disponible', LOUE: 'Loué', MAINTENANCE: 'Maintenance',
  RESERVEE: 'Réservée', EN_COURS: 'En cours', TERMINEE: 'Terminée', ANNULEE: 'Annulée',
  STANDARD: 'Standard', FIDELE: 'Fidèle', VIP: 'VIP',
  ESSENCE: 'Essence', DIESEL: 'Diesel', HYBRIDE: 'Hybride', ELECTRIQUE: 'Électrique', GPL: 'GPL',
  MANUELLE: 'Manuelle', AUTOMATIQUE: 'Automatique',
  ECONOMIQUE: 'Économique', COMPACTE: 'Compacte', BERLINE: 'Berline', SUV: 'SUV', UTILITAIRE: 'Utilitaire', LUXE: 'Luxe',
};

export function libelle(v: string | null | undefined): string {
  return v ? (LIBELLES[v] ?? v) : '';
}

/** Date du jour au format yyyy-MM-dd (fuseau local). */
export function aujourdhui(decalageJours = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + decalageJours);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}
