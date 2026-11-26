package tn.lim3.driveeasy.location.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Entite metier Location (stockee en memoire). */
public class Location {

    private Long id;
    private Long clientId;
    private Long vehiculeId;
    private LocalDate dateDebut;
    private LocalDate dateFinPrevue;
    private LocalDate dateRetourEffectif;
    private Integer kmDepart;
    private Integer kmRetour;
    private BigDecimal prixJournalierApplique;
    /** Coefficient de la categorie du vehicule au moment de la reservation. */
    private BigDecimal coefficientCategorie;
    /** Pourcentage de remise fidelite applique (0, 5 ou 10). */
    private BigDecimal tauxRemise;
    private BigDecimal montantRemise;
    /** Montant prevu a la reservation (jours x prix x coefficient - remise). */
    private BigDecimal montantBase;
    private BigDecimal penalite;
    /** Montant total = montantBase + penalite. */
    private BigDecimal montantTotal;
    private StatutLocation statut;
    private LocalDateTime dateCreation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public Long getVehiculeId() { return vehiculeId; }
    public void setVehiculeId(Long vehiculeId) { this.vehiculeId = vehiculeId; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFinPrevue() { return dateFinPrevue; }
    public void setDateFinPrevue(LocalDate dateFinPrevue) { this.dateFinPrevue = dateFinPrevue; }
    public LocalDate getDateRetourEffectif() { return dateRetourEffectif; }
    public void setDateRetourEffectif(LocalDate dateRetourEffectif) { this.dateRetourEffectif = dateRetourEffectif; }
    public Integer getKmDepart() { return kmDepart; }
    public void setKmDepart(Integer kmDepart) { this.kmDepart = kmDepart; }
    public Integer getKmRetour() { return kmRetour; }
    public void setKmRetour(Integer kmRetour) { this.kmRetour = kmRetour; }
    public BigDecimal getPrixJournalierApplique() { return prixJournalierApplique; }
    public void setPrixJournalierApplique(BigDecimal p) { this.prixJournalierApplique = p; }
    public BigDecimal getCoefficientCategorie() { return coefficientCategorie; }
    public void setCoefficientCategorie(BigDecimal c) { this.coefficientCategorie = c; }
    public BigDecimal getTauxRemise() { return tauxRemise; }
    public void setTauxRemise(BigDecimal tauxRemise) { this.tauxRemise = tauxRemise; }
    public BigDecimal getMontantRemise() { return montantRemise; }
    public void setMontantRemise(BigDecimal montantRemise) { this.montantRemise = montantRemise; }
    public BigDecimal getMontantBase() { return montantBase; }
    public void setMontantBase(BigDecimal montantBase) { this.montantBase = montantBase; }
    public BigDecimal getPenalite() { return penalite; }
    public void setPenalite(BigDecimal penalite) { this.penalite = penalite; }
    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }
    public StatutLocation getStatut() { return statut; }
    public void setStatut(StatutLocation statut) { this.statut = statut; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    /** Une location "active" bloque le client et le vehicule. */
    public boolean isActive() {
        return statut == StatutLocation.RESERVEE || statut == StatutLocation.EN_COURS;
    }
}
