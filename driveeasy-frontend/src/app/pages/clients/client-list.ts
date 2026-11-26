import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CATEGORIES_CLIENT, CategorieClient, Client } from '../../core/models/client';
import { ClientService } from '../../core/services/client.service';
import { NotificationService } from '../../core/services/notification.service';
import { Pagination } from '../../shared/pagination';
import { LibellePipe } from '../../shared/libelle.pipe';

/** /clients : tableau pagine, recherche (nom / CIN / email), badge de categorie, alerte permis expire. */
@Component({
  selector: 'app-client-list',
  imports: [FormsModule, RouterLink, DatePipe, Pagination, LibellePipe],
  templateUrl: './client-list.html',
})
export class ClientList implements OnInit {
  private readonly service = inject(ClientService);
  private readonly notifications = inject(NotificationService);

  protected readonly categories = CATEGORIES_CLIENT;
  protected readonly taillePage = 8;
  protected q = '';
  protected categorie: CategorieClient | '' = '';
  protected readonly clients = signal<Client[]>([]);
  protected readonly page = signal(0);
  protected readonly nbPages = computed(() => Math.ceil(this.clients().length / this.taillePage));
  protected readonly pageCourante = computed(() =>
    this.clients().slice(this.page() * this.taillePage, (this.page() + 1) * this.taillePage));
  protected readonly nbPermisExpires = computed(() => this.clients().filter((c) => c.permisExpire).length);

  ngOnInit(): void {
    this.rechercher();
  }

  rechercher(): void {
    this.service.rechercher(this.q, this.categorie).subscribe((c) => {
      this.clients.set(c);
      this.page.set(0);
    });
  }

  /** Permis qui expire dans moins de 30 jours. */
  expireBientot(c: Client): boolean {
    const jours = (new Date(c.dateExpirationPermis).getTime() - Date.now()) / 86_400_000;
    return !c.permisExpire && jours <= 30;
  }

  supprimer(c: Client): void {
    if (!confirm(`Supprimer le client ${c.prenom} ${c.nom} ?`)) return;
    this.service.supprimer(c.id).subscribe(() => {
      this.notifications.success('Client supprimé');
      this.rechercher();
    });
  }
}
