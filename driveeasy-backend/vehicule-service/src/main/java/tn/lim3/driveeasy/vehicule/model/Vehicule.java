package tn.lim3.driveeasy.vehicule.model;

import java.math.BigDecimal;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/** Entite metier Vehicule (stockee en memoire). */
public class Vehicule {

    private Long id;
    private String immatriculation;
    private String marque;
    private String modele;
    private Integer annee;
    private String couleur;
    private Categorie categorie;
    private Carburant carburant;
    private Boite boite;
    private Integer nbPlaces;
    private Integer kilometrage;
    private BigDecimal prixJournalier;
    private StatutVehicule statut;
    private String imageUrl;
    /** Planning des locations qui bloquent ce vehicule. */
    private final List<PeriodeReservation> reservations = new CopyOnWriteArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getImmatriculation() { return immatriculation; }
    public void setImmatriculation(String immatriculation) { this.immatriculation = immatriculation; }
    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }
    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }
    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }
    public String getCouleur() { return couleur; }
    public void setCouleur(String couleur) { this.couleur = couleur; }
    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) { this.categorie = categorie; }
    public Carburant getCarburant() { return carburant; }
    public void setCarburant(Carburant carburant) { this.carburant = carburant; }
    public Boite getBoite() { return boite; }
    public void setBoite(Boite boite) { this.boite = boite; }
    public Integer getNbPlaces() { return nbPlaces; }
    public void setNbPlaces(Integer nbPlaces) { this.nbPlaces = nbPlaces; }
    public Integer getKilometrage() { return kilometrage; }
    public void setKilometrage(Integer kilometrage) { this.kilometrage = kilometrage; }
    public BigDecimal getPrixJournalier() { return prixJournalier; }
    public void setPrixJournalier(BigDecimal prixJournalier) { this.prixJournalier = prixJournalier; }
    public StatutVehicule getStatut() { return statut; }
    public void setStatut(StatutVehicule statut) { this.statut = statut; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<PeriodeReservation> getReservations() { return reservations; }
}
