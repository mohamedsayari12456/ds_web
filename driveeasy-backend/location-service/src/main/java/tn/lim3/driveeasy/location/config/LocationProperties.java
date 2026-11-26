package tn.lim3.driveeasy.location.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Regles tarifaires et de fidelite externalisees dans config-repo/location-service.yml
 * (prefixe driveeasy.location) : modifiables sans recompiler.
 */
@ConfigurationProperties(prefix = "driveeasy.location")
public record LocationProperties(
        Boolean donneesDemo,
        Integer taillePageDefaut,
        BigDecimal penaliteCoefficient,
        Integer seuilFidele,
        Integer seuilVip,
        Map<String, BigDecimal> remises,
        Map<String, BigDecimal> coefficientsCategorie
) {
    public LocationProperties {
        if (donneesDemo == null) donneesDemo = true;
        if (taillePageDefaut == null) taillePageDefaut = 10;
        if (penaliteCoefficient == null) penaliteCoefficient = new BigDecimal("1.5");
        if (seuilFidele == null) seuilFidele = 5;
        if (seuilVip == null) seuilVip = 15;
        if (remises == null) remises = Map.of("STANDARD", BigDecimal.ZERO, "FIDELE", BigDecimal.valueOf(5), "VIP", BigDecimal.TEN);
        if (coefficientsCategorie == null) coefficientsCategorie = Map.of();
    }

    public BigDecimal remisePour(String categorieClient) {
        return chercher(remises, categorieClient, BigDecimal.ZERO);
    }

    public BigDecimal coefficientPour(String categorieVehicule) {
        return chercher(coefficientsCategorie, categorieVehicule, BigDecimal.ONE);
    }

    /** Recherche insensible a la casse (les cles YAML peuvent etre normalisees lors du binding). */
    private static BigDecimal chercher(Map<String, BigDecimal> map, String cle, BigDecimal defaut) {
        if (cle == null) return defaut;
        return map.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(cle))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(defaut);
    }
}
