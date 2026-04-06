package com.example.TravelApp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
    private String name;
    private String location;
    private String description;
    private Double rating;
    @Column(name = "preference_tag")
    private String preferenceTag;
    @Column(name = "estimated_cost")
    private Double estimatedCost;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    public Recommendation() {
    }

    public Recommendation(String type, String name, String location, String description, Double rating) {
        this.type = type;
        this.name = name;
        this.location = location;
        this.description = description;
        this.rating = rating;
    }

    public Recommendation(String type,
                          String name,
                          String location,
                          String description,
                          Double rating,
                          String preferenceTag,
                          Double estimatedCost) {
        this.type = type;
        this.name = name;
        this.location = location;
        this.description = description;
        this.rating = rating;
        this.preferenceTag = preferenceTag;
        this.estimatedCost = estimatedCost;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getPreferenceTag() {
        return preferenceTag;
    }

    public void setPreferenceTag(String preferenceTag) {
        this.preferenceTag = preferenceTag;
    }

    public Double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(Double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }
}
