package tn.lim3.driveeasy.location.client;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tn.lim3.driveeasy.location.exception.ApiError;
import tn.lim3.driveeasy.location.exception.BusinessException;
import tn.lim3.driveeasy.location.exception.ConflictException;
import tn.lim3.driveeasy.location.exception.ResourceNotFoundException;
import tn.lim3.driveeasy.location.exception.ServiceUnavailableException;

/**
 * Traduit les erreurs des appels inter-services en exceptions locales :
 * 404 -> 404, 409 -> 409, 400 -> 400 (message du service distant conserve), autre / panne -> 503.
 */
final class RemoteErrorHelper {

    private RemoteErrorHelper() {
    }

    static RuntimeException traduire(Exception e, String service) {
        if (e instanceof RestClientResponseException rex) {
            String message = messageDistant(rex);
            int code = rex.getStatusCode().value();
            if (code == HttpStatus.NOT_FOUND.value()) return new ResourceNotFoundException(message);
            if (code == HttpStatus.CONFLICT.value()) return new ConflictException(message);
            if (code == HttpStatus.BAD_REQUEST.value()) return new BusinessException(message);
        }
        if (e instanceof RestClientException || e instanceof IllegalStateException) {
            return new ServiceUnavailableException(service + " indisponible : " + e.getMessage(), e);
        }
        return new ServiceUnavailableException("Erreur lors de l'appel a " + service, e);
    }

    private static String messageDistant(RestClientResponseException rex) {
        try {
            ApiError err = rex.getResponseBodyAs(ApiError.class);
            if (err != null && err.message() != null) return err.message();
        } catch (Exception ignore) {
            // corps non JSON : on garde le message brut
        }
        return rex.getStatusText();
    }
}
