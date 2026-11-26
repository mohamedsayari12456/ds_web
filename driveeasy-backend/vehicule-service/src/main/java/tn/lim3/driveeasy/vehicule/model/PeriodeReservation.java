package tn.lim3.driveeasy.vehicule.model;

import java.time.LocalDate;

/**
 * Periode pendant laquelle un vehicule est bloque par une location (reservee ou en cours).
 * Permet de repondre a GET /vehicules/disponibles?dateDebut&dateFin sans appeler location-service.
 */
public record PeriodeReservation(Long locationId, LocalDate dateDebut, LocalDate dateFin) {

    /** Deux periodes [d1,f1] et [d2,f2] se chevauchent si d1 <= f2 et d2 <= f1. */
    public boolean chevauche(LocalDate debut, LocalDate fin) {
        return !dateDebut.isAfter(fin) && !debut.isAfter(dateFin);
    }
}
