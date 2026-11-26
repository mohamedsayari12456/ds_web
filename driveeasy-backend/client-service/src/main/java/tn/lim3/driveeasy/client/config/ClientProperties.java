package tn.lim3.driveeasy.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Proprietes metier externalisees dans config-repo/client-service.yml (prefixe driveeasy.client). */
@ConfigurationProperties(prefix = "driveeasy.client")
public record ClientProperties(Integer ageMinimum, Boolean donneesDemo) {
    public ClientProperties {
        if (ageMinimum == null) ageMinimum = 21;
        if (donneesDemo == null) donneesDemo = true;
    }
}
