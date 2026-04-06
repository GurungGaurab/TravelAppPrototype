package com.example.TravelApp.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Column(name = "hashed_password")
    private String hashedPassword;

    private String name;

    @Column(name = "personal_info")
    private String personalInfo;

    @Column(name = "travel_preferences")
    private String travelPreferences;

    @Column(name = "budget_range")
    private Double budgetRange;

    private String interests;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trip> trips = new ArrayList<>();

    public User() {
    }

    public User(String email, String hashedPassword, String name, String personalInfo,
                String travelPreferences, Double budgetRange, String interests) {
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.name = name;
        this.personalInfo = personalInfo;
        this.travelPreferences = travelPreferences;
        this.budgetRange = budgetRange;
        this.interests = interests;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPersonalInfo() {
        return personalInfo;
    }

    public void setPersonalInfo(String personalInfo) {
        this.personalInfo = personalInfo;
    }

    public String getTravelPreferences() {
        return travelPreferences;
    }

    public void setTravelPreferences(String travelPreferences) {
        this.travelPreferences = travelPreferences;
    }

    public Double getBudgetRange() {
        return budgetRange;
    }

    public void setBudgetRange(Double budgetRange) {
        this.budgetRange = budgetRange;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public List<Trip> getTrips() {
        return trips;
    }

    public void addTrip(Trip trip) {
        trips.add(trip);
        trip.setUser(this);
    }

    public void removeTrip(Trip trip) {
        trips.remove(trip);
        trip.setUser(null);
    }
}
