package tn.lim3.driveeasy.vehicule.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.lim3.driveeasy.vehicule.config.VehiculeProperties;
import tn.lim3.driveeasy.vehicule.dto.ReservationRequest;
import tn.lim3.driveeasy.vehicule.dto.StatutRequest;
import tn.lim3.driveeasy.vehicule.dto.VehiculeRequest;
import tn.lim3.driveeasy.vehicule.exception.BusinessException;
import tn.lim3.driveeasy.vehicule.exception.ConflictException;
import tn.lim3.driveeasy.vehicule.model.*;
import tn.lim3.driveeasy.vehicule.repository.VehiculeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests unitaires des regles de gestion de vehicule-service. */
class VehiculeServiceTest {

    private VehiculeService service;

    @BeforeEach
    void init() {
        service = new VehiculeService(new VehiculeRepository(), new VehiculeProperties(1990, false));
    }

    private VehiculeRequest requete(String immat, int annee, String prix) {
        return new VehiculeRequest(immat, "Peugeot", "208", annee, "Blanc", Categorie.ECONOMIQUE, Carburant.ESSENCE,
                Boite.MANUELLE, 5, 1000, new BigDecimal(prix), null, null);
    }

    @Test
    void immatriculationUnique_409() {
        service.creer(requete("100 TU 1", 2022, "80"));
        assertThatThrownBy(() -> service.creer(requete("100 tu 1", 2023, "90")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void anneeFuture_refusee() {
        assertThatThrownBy(() -> service.creer(requete("100 TU 2", Year.now().getValue() + 1, "80")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reservationsQuiSeChevauchent_409() {
        Vehicule v = service.creer(requete("100 TU 3", 2022, "80"));
        LocalDate d = LocalDate.now().plusDays(1);
        service.reserver(v.getId(), new ReservationRequest(1L, d, d.plusDays(3)));
        assertThatThrownBy(() -> service.reserver(v.getId(), new ReservationRequest(2L, d.plusDays(2), d.plusDays(5))))
                .isInstanceOf(ConflictException.class);
        assertThat(service.disponibles(d.plusDays(4), d.plusDays(6), null)).hasSize(1);
        assertThat(service.disponibles(d, d.plusDays(1), null)).isEmpty();
    }

    @Test
    void vehiculeEnMaintenance_nonReservable() {
        Vehicule v = service.creer(requete("100 TU 4", 2022, "80"));
        service.changerStatut(v.getId(), new StatutRequest(StatutVehicule.MAINTENANCE, null));
        LocalDate d = LocalDate.now().plusDays(1);
        assertThatThrownBy(() -> service.reserver(v.getId(), new ReservationRequest(1L, d, d.plusDays(1))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void suppressionRefuseeSiLoue() {
        Vehicule v = service.creer(requete("100 TU 5", 2022, "80"));
        service.changerStatut(v.getId(), new StatutRequest(StatutVehicule.LOUE, null));
        assertThatThrownBy(() -> service.supprimer(v.getId())).isInstanceOf(ConflictException.class);
    }
}
