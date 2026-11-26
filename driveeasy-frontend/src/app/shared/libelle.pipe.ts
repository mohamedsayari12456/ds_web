import { Pipe, PipeTransform } from '@angular/core';
import { libelle } from './libelles';

/** {{ 'EN_COURS' | libelle }} -> "En cours" */
@Pipe({ name: 'libelle' })
export class LibellePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return libelle(value);
  }
}
