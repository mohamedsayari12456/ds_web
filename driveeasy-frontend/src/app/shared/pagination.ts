import { Component, computed, input, output } from '@angular/core';

/** Composant de pagination reutilisable (pages numerotees a partir de 0). */
@Component({
  selector: 'app-pagination',
  template: `
    <div class="pagination">
      <span>{{ total() }} resultat(s) · page {{ page() + 1 }} / {{ nbPages() || 1 }}</span>
      <div class="actions">
        <button class="btn btn-petit" [disabled]="page() === 0" (click)="changer.emit(page() - 1)">‹ Précédent</button>
        @for (p of pages(); track p) {
          <button class="btn btn-petit" [class.btn-primaire]="p === page()" (click)="changer.emit(p)">{{ p + 1 }}</button>
        }
        <button class="btn btn-petit" [disabled]="page() >= nbPages() - 1" (click)="changer.emit(page() + 1)">Suivant ›</button>
      </div>
    </div>
  `,
})
export class Pagination {
  readonly page = input.required<number>();
  readonly nbPages = input.required<number>();
  readonly total = input.required<number>();
  readonly changer = output<number>();
  protected readonly pages = computed(() => Array.from({ length: this.nbPages() }, (_, i) => i).slice(0, 12));
}
