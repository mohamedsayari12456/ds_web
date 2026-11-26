package tn.lim3.driveeasy.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Microservice de gestion des clients - port 8082 (defini dans le Config Server). */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ClientServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClientServiceApplication.class, args);
    }
}
