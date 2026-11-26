package tn.lim3.driveeasy.location.exception;

/** Ressource introuvable -> HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
