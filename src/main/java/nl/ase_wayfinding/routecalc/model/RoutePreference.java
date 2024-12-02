package nl.ase_wayfinding.routecalc.model;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "route_preferences")
public class RoutePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long preferenceId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "avoid_highways")
    private Boolean avoidHighways = false;

    @Column(name = "avoid_tolls")
    private Boolean avoidTolls = false;

    @Column(name = "preferred_mode")
    private String preferredMode;

    @Column(name = "eco_friendly")
    private Boolean ecoFriendly = true;

    @Column(name = "minimize_co2")
    private Boolean minimizeCo2 = false;

    @Column(name = "avoid_dangerous_streets")
    private Boolean avoidDangerousStreets = true;

    @Column(name = "last_updated")
    private Timestamp lastUpdated;

    // Constructors
    public RoutePreference() {}

    // Getters and Setters

    public Long getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(Long preferenceId) {
        this.preferenceId = preferenceId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
       this.user = user;
    }

    public Boolean getAvoidHighways() {
        return avoidHighways;
    }

    public void setAvoidHighways(Boolean avoidHighways) {
        this.avoidHighways = avoidHighways;
    }

    public Boolean getAvoidTolls() {
        return avoidTolls;
    }

    public void setAvoidTolls(Boolean avoidTolls) {
        this.avoidTolls = avoidTolls;
    }

    public String getPreferredMode() {
        return preferredMode;
    }

    public void setPreferredMode(String preferredMode) {
        this.preferredMode = preferredMode;
    }

    public Boolean getEcoFriendly() {
        return ecoFriendly;
    }

    public void setEcoFriendly(Boolean ecoFriendly) {
        this.ecoFriendly = ecoFriendly;
    }

    public Boolean getMinimizeCo2() {
        return minimizeCo2;
    }

    public void setMinimizeCo2(Boolean minimizeCo2) {
        this.minimizeCo2 = minimizeCo2;
    }

    public Boolean getAvoidDangerousStreets() {
        return avoidDangerousStreets;
    }

    public void setAvoidDangerousStreets(Boolean avoidDangerousStreets) {
        this.avoidDangerousStreets = avoidDangerousStreets;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Timestamp lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
