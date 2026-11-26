import { HttpErrorResponse } from '@angular/common/http';
import { messageErreur } from './error.interceptor';

describe('messageErreur', () => {
  it('signale un serveur injoignable (statut 0)', () => {
    expect(messageErreur(new HttpErrorResponse({ status: 0 }))).toContain('injoignable');
  });

  it('reprend le message ApiError du backend', () => {
    const err = new HttpErrorResponse({ status: 409, error: { message: "L'immatriculation existe deja" } });
    expect(messageErreur(err)).toBe("L'immatriculation existe deja");
  });

  it('detaille les erreurs de validation', () => {
    const err = new HttpErrorResponse({ status: 400, error: { message: 'Donnees invalides', details: { prix: 'Prix > 0' } } });
    expect(messageErreur(err)).toBe('Donnees invalides : Prix > 0');
  });
});
