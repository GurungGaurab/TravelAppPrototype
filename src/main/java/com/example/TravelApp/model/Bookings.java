package com.example.TravelApp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bookings")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Bookings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public abstract String getBookingSummary();
}
