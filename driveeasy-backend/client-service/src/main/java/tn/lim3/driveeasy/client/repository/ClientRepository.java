package tn.lim3.driveeasy.client.repository;

import org.springframework.stereotype.Repository;
import tn.lim3.driveeasy.client.model.Client;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/** Depot en memoire (RAM) des clients - aucune base de donnees. */
@Repository
public class ClientRepository {

    private final Map<Long, Client> stockage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public List<Client> findAll() {
        return stockage.values().stream().sorted(Comparator.comparing(Client::getId)).toList();
    }

    public Optional<Client> findById(Long id) {
        return Optional.ofNullable(stockage.get(id));
    }

    /** Recherche insensible a la casse sur un champ unique (cin, email, numeroPermis). */
    public Optional<Client> findBy(Function<Client, String> champ, String valeur) {
        return stockage.values().stream()
                .filter(c -> champ.apply(c) != null && champ.apply(c).equalsIgnoreCase(valeur))
                .findFirst();
    }

    public Client save(Client client) {
        if (client.getId() == null) {
            client.setId(sequence.incrementAndGet());
        }
        stockage.put(client.getId(), client);
        return client;
    }

    public void deleteById(Long id) {
        stockage.remove(id);
    }

    public long count() {
        return stockage.size();
    }
}
