package tn.lim3.driveeasy.vehicule.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Proprietes metier externalisees dans config-repo/vehicule-service.yml (prefixe driveeasy.vehicule). */
@ConfigurationProperties(prefix = "driveeasy.vehicule")
public record VehiculeProperties(Integer anneeMin, Boolean donneesDemo) {
    public VehiculeProperties {
        if (anneeMin == null) anneeMin = 1990;
        if (donneesDemo == null) donneesDemo = true;
    }
}
