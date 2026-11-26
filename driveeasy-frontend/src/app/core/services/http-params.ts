import { HttpParams } from '@angular/common/http';

/** Construit des HttpParams en ignorant les valeurs vides (null, undefined, ''). */
export function toParams(obj: object): HttpParams {
  let params = new HttpParams();
  Object.entries(obj).forEach(([k, v]) => {
    if (v !== null && v !== undefined && v !== '') {
      params = params.set(k, String(v));
    }
  });
  return params;
}
