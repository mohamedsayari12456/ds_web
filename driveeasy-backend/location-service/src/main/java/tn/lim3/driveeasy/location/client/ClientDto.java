package tn.lim3.driveeasy.location.client;

import java.time.LocalDate;

/** Vue d'un client telle que renvoyee par client-service. */
public record ClientDto(Long id, String cin, String nom, String prenom, Integer age, String email, String telephone,
                        String numeroPermis, LocalDate dateExpirationPermis, boolean permisExpire,
                        String categorieClient) {
}
