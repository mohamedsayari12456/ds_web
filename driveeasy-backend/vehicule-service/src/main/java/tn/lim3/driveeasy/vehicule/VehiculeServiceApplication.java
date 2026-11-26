package tn.lim3.driveeasy.vehicule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Microservice de gestion du parc automobile - port 8081 (defini dans le Config Server). */
@SpringBootApplication
@ConfigurationPropertiesScan
public class VehiculeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VehiculeServiceApplication.class, args);
    }
}
