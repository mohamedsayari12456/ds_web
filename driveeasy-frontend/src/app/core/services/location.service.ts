import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Location, LocationDetail, LocationFiltres, LocationRequest, Page, Simulation, Statistiques,
} from '../models/location';
import { toParams } from './http-params';

/** Acces a location-service via la gateway (/api/locations). */
@Injectable({ providedIn: 'root' })
export class LocationService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/locations`;

  lister(filtres: LocationFiltres = {}): Observable<Page<Location>> {
    return this.http.get<Page<Location>>(this.url, { params: toParams(filtres) });
  }

  detail(id: number): Observable<LocationDetail> {
    return this.http.get<LocationDetail>(`${this.url}/${id}`);
  }

  simuler(req: LocationRequest): Observable<Simulation> {
    return this.http.post<Simulation>(`${this.url}/simulation`, req);
  }

  creer(req: LocationRequest): Observable<Location> {
    return this.http.post<Location>(this.url, req);
  }

  depart(id: number): Observable<Location> {
    return this.http.patch<Location>(`${this.url}/${id}/depart`, {});
  }

  retour(id: number, kmRetour: number, dateRetourEffectif?: string): Observable<Location> {
    return this.http.patch<Location>(`${this.url}/${id}/retour`, { kmRetour, dateRetourEffectif });
  }

  annuler(id: number): Observable<Location> {
    return this.http.patch<Location>(`${this.url}/${id}/annulation`, {});
  }

  statistiques(): Observable<Statistiques> {
    return this.http.get<Statistiques>(`${this.url}/statistiques`);
  }
}
