package tn.lim3.driveeasy.location.repository;

import org.springframework.stereotype.Repository;
import tn.lim3.driveeasy.location.model.Location;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Depot en memoire (RAM) des locations - aucune base de donnees. */
@Repository
public class LocationRepository {

    private final Map<Long, Location> stockage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    /** Reserve un identifiant AVANT l'appel a vehicule-service (le planning du vehicule reference cet id). */
    public Long nextId() {
        return sequence.incrementAndGet();
    }

    public List<Location> findAll() {
        return stockage.values().stream().sorted(Comparator.comparing(Location::getId).reversed()).toList();
    }

    public Optional<Location> findById(Long id) {
        return Optional.ofNullable(stockage.get(id));
    }

    public Location save(Location location) {
        if (location.getId() == null) {
            location.setId(nextId());
        }
        stockage.put(location.getId(), location);
        return location;
    }

    public long count() {
        return stockage.size();
    }
}
