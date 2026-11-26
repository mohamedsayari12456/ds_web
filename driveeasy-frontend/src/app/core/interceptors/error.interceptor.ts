import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

/**
 * Intercepteur d'erreurs : transforme les reponses d'erreur de l'API (format ApiError) en message lisible,
 * affiche un toast, puis relaie l'erreur au composant (qui peut afficher les erreurs de validation champ par champ).
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifications = inject(NotificationService);
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      notifications.error(messageErreur(err));
      return throwError(() => err);
    }),
  );
};

export function messageErreur(err: HttpErrorResponse): string {
  if (err.status === 0) {
    return "Serveur injoignable : verifiez que l'API Gateway (port 8080) est demarree.";
  }
  const body = err.error;
  if (body?.details && typeof body.details === 'object') {
    return `${body.message} : ${Object.values(body.details).join(', ')}`;
  }
  if (body?.message) {
    return body.message;
  }
  if (err.status === 503) {
    return 'Service momentanement indisponible (503).';
  }
  return `Erreur ${err.status} : ${err.statusText}`;
}
