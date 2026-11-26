package tn.lim3.driveeasy.client.exception;

/** Un service distant (appel inter-services) est indisponible ou a echoue -> HTTP 503. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
