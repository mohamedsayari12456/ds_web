package tn.lim3.driveeasy.location.model;

/** Cycle de vie : RESERVEE -> EN_COURS (depart) -> TERMINEE (retour) ; RESERVEE -> ANNULEE. */
public enum StatutLocation {
    RESERVEE, EN_COURS, TERMINEE, ANNULEE
}
