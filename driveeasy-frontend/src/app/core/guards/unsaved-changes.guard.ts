import { CanDeactivateFn } from '@angular/router';

/** Composant qui peut signaler des modifications non enregistrees. */
export interface AvecModificationsNonEnregistrees {
  aDesModificationsNonEnregistrees(): boolean;
}

/** Guard : demande confirmation avant de quitter un formulaire modifie et non enregistre. */
export const unsavedChangesGuard: CanDeactivateFn<AvecModificationsNonEnregistrees> = (component) =>
  !component.aDesModificationsNonEnregistrees()
  || confirm('Des modifications ne sont pas enregistrees. Quitter quand meme ?');
