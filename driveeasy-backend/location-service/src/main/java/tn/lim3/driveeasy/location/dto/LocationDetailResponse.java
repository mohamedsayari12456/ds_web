package tn.lim3.driveeasy.location.dto;

import tn.lim3.driveeasy.location.client.ClientDto;
import tn.lim3.driveeasy.location.client.VehiculeDto;

import java.util.List;

/** Detail enrichi : location + client + vehicule agreges (GET /locations/{id}). */
public record LocationDetailResponse(
        LocationResponse location,
        ClientDto client,
        VehiculeDto vehicule,
        List<String> avertissements
) {
}
