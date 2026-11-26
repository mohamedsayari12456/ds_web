package tn.lim3.driveeasy.vehicule.exception;

/** Donnees valides syntaxiquement mais contraires a une regle de gestion -> HTTP 400. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
