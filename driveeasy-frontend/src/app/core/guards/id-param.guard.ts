import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

/** Guard : l'identifiant de l'URL doit etre un entier positif, sinon redirection vers la liste parente. */
export const idParamGuard: CanActivateFn = (route) => {
  const id = Number(route.paramMap.get('id'));
  if (Number.isInteger(id) && id > 0) {
    return true;
  }
  const parent = route.pathFromRoot.map((r) => r.url.map((s) => s.path).join('/')).filter(Boolean)[0] ?? 'dashboard';
  return inject(Router).createUrlTree(['/' + parent]);
};
