package tn.lim3.driveeasy.vehicule.repository;

import org.springframework.stereotype.Repository;
import tn.lim3.driveeasy.vehicule.model.Vehicule;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Depot en memoire (RAM) - aucune base de donnees, conformement au cahier des charges.
 * ConcurrentHashMap + AtomicLong garantissent un acces sur en cas de requetes concurrentes.
 */
@Repository
public class VehiculeRepository {

    private final Map<Long, Vehicule> stockage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public List<Vehicule> findAll() {
        return stockage.values().stream().sorted(Comparator.comparing(Vehicule::getId)).toList();
    }

    public Optional<Vehicule> findById(Long id) {
        return Optional.ofNullable(stockage.get(id));
    }

    public Optional<Vehicule> findByImmatriculation(String immatriculation) {
        return stockage.values().stream()
                .filter(v -> v.getImmatriculation().equalsIgnoreCase(immatriculation))
                .findFirst();
    }

    public Vehicule save(Vehicule vehicule) {
        if (vehicule.getId() == null) {
            vehicule.setId(sequence.incrementAndGet());
        }
        stockage.put(vehicule.getId(), vehicule);
        return vehicule;
    }

    public void deleteById(Long id) {
        stockage.remove(id);
    }

    public long count() {
        return stockage.size();
    }
}
