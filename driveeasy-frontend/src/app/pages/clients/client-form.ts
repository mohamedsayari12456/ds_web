import { Component, inject, input, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CATEGORIES_CLIENT, ClientRequest } from '../../core/models/client';
import { ClientService } from '../../core/services/client.service';
import { NotificationService } from '../../core/services/notification.service';
import { AvecModificationsNonEnregistrees } from '../../core/guards/unsaved-changes.guard';
import { LibellePipe } from '../../shared/libelle.pipe';

function datePassee(c: AbstractControl): ValidationErrors | null {
  return !c.value || new Date(c.value) < new Date() ? null : { passee: true };
}

/** /clients/new et /clients/:id/edit : formulaire reactif client. */
@Component({
  selector: 'app-client-form',
  imports: [ReactiveFormsModule, RouterLink, LibellePipe],
  templateUrl: './client-form.html',
})
export class ClientForm implements OnInit, AvecModificationsNonEnregistrees {
  readonly id = input<string>();

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(ClientService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly categories = CATEGORIES_CLIENT;
  protected readonly enregistrement = signal(false);
  protected readonly erreursServeur = signal<Record<string, string>>({});

  protected readonly form = this.fb.nonNullable.group({
    cin: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
    dateNaissance: ['', [Validators.required, datePassee]],
    email: ['', [Validators.required, Validators.email]],
    telephone: ['', [Validators.required, Validators.pattern(/^\+?[0-9 ]{8,15}$/)]],
    adresse: [''],
    numeroPermis: ['', Validators.required],
    dateExpirationPermis: ['', Validators.required],
    categorieClient: ['STANDARD'],
  });

  get modeEdition(): boolean {
    return !!this.id();
  }

  ngOnInit(): void {
    if (this.modeEdition) {
      this.service.detail(Number(this.id())).subscribe((c) => {
        this.form.patchValue({ ...c, adresse: c.adresse ?? '' });
        this.form.markAsPristine();
      });
    }
  }

  invalide(champ: string): boolean {
    const c = this.form.get(champ);
    return !!c && c.invalid && (c.touched || c.dirty);
  }

  enregistrer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enregistrement.set(true);
    this.erreursServeur.set({});
    const valeur = this.form.getRawValue() as ClientRequest;
    const requete = this.modeEdition ? this.service.modifier(Number(this.id()), valeur) : this.service.creer(valeur);
    requete.subscribe({
      next: (c) => {
        this.form.markAsPristine();
        this.notifications.success(`Client ${c.prenom} ${c.nom} enregistré`);
        this.router.navigate(['/clients', c.id]);
      },
      error: (err: HttpErrorResponse) => {
        this.enregistrement.set(false);
        this.erreursServeur.set(err.error?.details ?? {});
      },
    });
  }

  aDesModificationsNonEnregistrees(): boolean {
    return this.form.dirty && !this.enregistrement();
  }
}
