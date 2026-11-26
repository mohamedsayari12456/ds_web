package tn.lim3.driveeasy.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Annuaire des services DriveEasy (Eureka Server) - port 8761.
 * Tous les autres composants s'y enregistrent ; le registre lui-meme ne s'enregistre pas.
 */
@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}
