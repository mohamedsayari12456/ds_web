package tn.lim3.driveeasy.location;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Microservice des locations - port 8083 (defini dans le Config Server).
 * Orchestre les appels vers client-service (eligibilite, fidelite) et vehicule-service (disponibilite, statut).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LocationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LocationServiceApplication.class, args);
    }
}
