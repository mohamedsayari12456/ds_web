package tn.lim3.driveeasy.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Serveur de configuration centralisee - port 8888.
 * Profil "native" (defaut) : lit le dossier config-repo du projet.
 * Profil "git" : lit le depot GitHub indique par CONFIG_GIT_URI.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
