import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StatutVehicule, Vehicule, VehiculeFiltres, VehiculeRequest } from '../models/vehicule';
import { toParams } from './http-params';

/** Acces a vehicule-service via la gateway (/api/vehicules). */
@Injectable({ providedIn: 'root' })
export class VehiculeService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/vehicules`;

  lister(filtres: VehiculeFiltres = {}): Observable<Vehicule[]> {
    return this.http.get<Vehicule[]>(this.url, { params: toParams(filtres) });
  }

  disponibles(dateDebut: string, dateFin: string, categorie?: string): Observable<Vehicule[]> {
    return this.http.get<Vehicule[]>(`${this.url}/disponibles`, { params: toParams({ dateDebut, dateFin, categorie }) });
  }

  detail(id: number): Observable<Vehicule> {
    return this.http.get<Vehicule>(`${this.url}/${id}`);
  }

  creer(v: VehiculeRequest): Observable<Vehicule> {
    return this.http.post<Vehicule>(this.url, v);
  }

  modifier(id: number, v: VehiculeRequest): Observable<Vehicule> {
    return this.http.put<Vehicule>(`${this.url}/${id}`, v);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  changerStatut(id: number, statut: StatutVehicule): Observable<Vehicule> {
    return this.http.patch<Vehicule>(`${this.url}/${id}/statut`, { statut });
  }
}
