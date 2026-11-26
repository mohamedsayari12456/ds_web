package tn.lim3.driveeasy.client.dto;

import jakarta.validation.constraints.NotNull;
import tn.lim3.driveeasy.client.model.CategorieClient;

/** PATCH /clients/{id}/categorie. */
public record CategorieRequest(@NotNull(message = "La categorie est obligatoire") CategorieClient categorie) {
}
