package tn.lim3.driveeasy.vehicule.exception;

/** Conflit avec l'etat actuel (doublon, statut incompatible...) -> HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
