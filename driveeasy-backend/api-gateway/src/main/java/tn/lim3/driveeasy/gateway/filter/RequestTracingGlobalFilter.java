package tn.lim3.driveeasy.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filtre global personnalise de la gateway :
 *  - ajoute un identifiant de requete X-Request-Id (genere s'il est absent) transmis aux services ;
 *  - mesure le temps de traitement et le renvoie dans l'en-tete X-Response-Time ;
 *  - journalise chaque requete (methode, chemin, route cible, statut, duree) et signale les requetes lentes.
 */
@Component
public class RequestTracingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestTracingGlobalFilter.class);
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String RESPONSE_TIME_HEADER = "X-Response-Time";

    private final long seuilLenteMs;

    public RequestTracingGlobalFilter(@Value("${driveeasy.gateway.seuil-requete-lente-ms:1000}") long seuilLenteMs) {
        this.seuilLenteMs = seuilLenteMs;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long debut = System.currentTimeMillis();
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        final String id = requestId;

        ServerHttpRequest requete = exchange.getRequest().mutate()
                .headers(h -> h.set(REQUEST_ID_HEADER, id))
                .build();
        ServerWebExchange echange = exchange.mutate().request(requete).build();

        // Les en-tetes de reponse doivent etre poses avant l'ecriture du corps
        echange.getResponse().beforeCommit(() -> {
            long duree = System.currentTimeMillis() - debut;
            echange.getResponse().getHeaders().set(REQUEST_ID_HEADER, id);
            echange.getResponse().getHeaders().set(RESPONSE_TIME_HEADER, duree + "ms");
            return Mono.empty();
        });

        return chain.filter(echange).doFinally(signal -> {
            long duree = System.currentTimeMillis() - debut;
            Route route = echange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String routeId = route != null ? route.getId() : "-";
            var statut = echange.getResponse().getStatusCode();
            String message = "[{}] {} {} -> route={} statut={} duree={}ms";
            Object[] args = {id, requete.getMethod(), requete.getURI().getPath(), routeId, statut, duree};
            if (duree > seuilLenteMs) {
                log.warn("REQUETE LENTE " + message, args);
            } else {
                log.info(message, args);
            }
        });
    }

    @Override
    public int getOrder() {
        // Execute tres tot dans la chaine de filtres
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
