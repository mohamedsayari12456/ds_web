import { Component, inject, input, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { BOITES, CARBURANTS, CATEGORIES_VEHICULE, STATUTS_VEHICULE, VehiculeRequest } from '../../core/models/vehicule';
import { VehiculeService } from '../../core/services/vehicule.service';
import { NotificationService } from '../../core/services/notification.service';
import { AvecModificationsNonEnregistrees } from '../../core/guards/unsaved-changes.guard';
import { LibellePipe } from '../../shared/libelle.pipe';

/** Validateur : annee coherente (entre 1990 et l'annee courante). */
function anneeValide(c: AbstractControl): ValidationErrors | null {
  const a = Number(c.value);
  return !c.value || (a >= 1990 && a <= new Date().getFullYear()) ? null : { annee: true };
}

/** /vehicules/new et /vehicules/:id/edit : formulaire reactif avec validations et gestion du statut. */
@Component({
  selector: 'app-vehicule-form',
  imports: [ReactiveFormsModule, RouterLink, LibellePipe],
  templateUrl: './vehicule-form.html',
  styleUrl: './vehicules.css',
})
export class VehiculeForm implements OnInit, AvecModificationsNonEnregistrees {
  /** Parametre de route :id (liaison automatique withComponentInputBinding). */
  readonly id = input<string>();

  private readonly fb = inject(FormBuilder);
  private readonly service = inject(VehiculeService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly carburants = CARBURANTS;
  protected readonly boites = BOITES;
  protected readonly categories = CATEGORIES_VEHICULE;
  protected readonly statuts = STATUTS_VEHICULE;
  protected readonly anneeMax = new Date().getFullYear();
  protected readonly enregistrement = signal(false);
  protected readonly erreursServeur = signal<Record<string, string>>({});
  protected readonly statutActuel = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    immatriculation: ['', [Validators.required, Validators.maxLength(20)]],
    marque: ['', Validators.required],
    modele: ['', Validators.required],
    annee: [this.anneeMax, [Validators.required, anneeValide]],
    couleur: [''],
    categorie: ['ECONOMIQUE', Validators.required],
    carburant: ['ESSENCE', Validators.required],
    boite: ['MANUELLE', Validators.required],
    nbPlaces: [5, [Validators.required, Validators.min(1), Validators.max(9)]],
    kilometrage: [0, [Validators.required, Validators.min(0)]],
    prixJournalier: [100, [Validators.required, Validators.min(0.01)]],
    statut: ['DISPONIBLE', Validators.required],
    imageUrl: [''],
  });

  get modeEdition(): boolean {
    return !!this.id();
  }

  ngOnInit(): void {
    if (this.modeEdition) {
      this.service.detail(Number(this.id())).subscribe((v) => {
        this.form.patchValue({ ...v, couleur: v.couleur ?? '', imageUrl: v.imageUrl ?? '' });
        this.statutActuel.set(v.statut);
        if (v.statut === 'LOUE') {
          this.form.controls.statut.disable();
        }
        this.form.markAsPristine();
      });
    }
  }

  apercuImage(): string {
    const url = this.form.controls.imageUrl.value;
    return url || `assets/vehicules/${this.form.controls.categorie.value.toLowerCase()}.svg`;
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
    const valeur = this.form.getRawValue() as unknown as VehiculeRequest;
    const requete = this.modeEdition
      ? this.service.modifier(Number(this.id()), valeur)
      : this.service.creer(valeur);
    requete.subscribe({
      next: (v) => {
        this.form.markAsPristine();
        this.notifications.success(`Véhicule ${v.marque} ${v.modele} enregistré`);
        this.router.navigate(['/vehicules']);
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
