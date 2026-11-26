import { Pipe, PipeTransform } from '@angular/core';
import { formatNumber } from '@angular/common';

/** Montant en dinars tunisiens avec 2 decimales : {{ 1234.5 | tnd }} -> "1 234,50 TND". */
@Pipe({ name: 'tnd' })
export class TndPipe implements PipeTransform {
  transform(value: number | null | undefined, digits = '1.2-2'): string {
    return value === null || value === undefined ? '–' : `${formatNumber(value, 'fr-FR', digits)} TND`;
  }
}
