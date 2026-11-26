package tn.lim3.driveeasy.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway DriveEasy - port 8080.
 * Point d'entree unique : /api/vehicules/**, /api/clients/**, /api/locations/**
 * Les routes, le CORS et le load balancing (lb://) sont definis dans config-repo/api-gateway.yml.
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
