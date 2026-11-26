import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CategorieClient, Client, ClientRequest, Eligibilite } from '../models/client';
import { toParams } from './http-params';

/** Acces a client-service via la gateway (/api/clients). */
@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/clients`;

  rechercher(q = '', categorie: CategorieClient | '' = ''): Observable<Client[]> {
    return this.http.get<Client[]>(this.url, { params: toParams({ q, categorie }) });
  }

  detail(id: number): Observable<Client> {
    return this.http.get<Client>(`${this.url}/${id}`);
  }

  eligibilite(id: number): Observable<Eligibilite> {
    return this.http.get<Eligibilite>(`${this.url}/${id}/eligibilite`);
  }

  creer(c: ClientRequest): Observable<Client> {
    return this.http.post<Client>(this.url, c);
  }

  modifier(id: number, c: ClientRequest): Observable<Client> {
    return this.http.put<Client>(`${this.url}/${id}`, c);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
