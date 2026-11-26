package tn.lim3.driveeasy.client.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Format uniforme des erreurs renvoyees par l'API. */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> details
) {
}
