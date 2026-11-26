package tn.lim3.driveeasy.client.config;

import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.web.client.RestClient;

/**
 * RestClient utilise pour la communication synchrone inter-services.
 *  - @LoadBalanced : les URL de type http://nom-du-service sont resolues via Eureka (load balancing) ;
 *  - RestClientBuilderConfigurer applique la configuration Spring Boot, dont l'instrumentation
 *    Micrometer : l'en-tete de tracage (traceId) est propage, les appels apparaissent dans Zipkin ;
 *  - delais d'attente courts pour ne pas bloquer en cas de panne d'un service.
 */
@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
        // Client HTTP du JDK : supporte PATCH (contrairement a HttpURLConnection)
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        return configurer.configure(RestClient.builder()).requestFactory(factory);
    }
}
