package tn.lim3.driveeasy.location.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Indicateurs du tableau de bord (GET /locations/statistiques). */
public record StatistiquesResponse(
        BigDecimal chiffreAffairesTotal,
        BigDecimal chiffreAffairesMois,
        List<CaMensuel> chiffreAffairesParMois,
        Map<String, Long> locationsParStatut,
        long locationsEnCours,
        long locationsEnRetard,
        Integer nbVehicules,
        Map<String, Long> vehiculesParStatut,
        BigDecimal tauxOccupation,
        BigDecimal tauxOccupation30Jours,
        List<TopVehicule> topVehicules,
        List<String> avertissements
) {
    public record CaMensuel(String mois, BigDecimal montant, long nbLocations) {
    }

    public record TopVehicule(Long vehiculeId, String libelle, String immatriculation, long nbLocations,
                              BigDecimal chiffreAffaires) {
    }
}
