package com.example.TravelApp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "flight_bookings")
public class FlightBooking extends Bookings {

    private String origin;
    private String destination;
    private LocalDateTime departureDate;
    private LocalDateTime arrivalDate;
    private LocalDateTime returnDepartureDate;
    private LocalDateTime returnArrivalDate;
    private Double price;
    private String airline;
    private String flightNumber;

    public FlightBooking() {
    }

    public FlightBooking(String origin,
                         String destination,
                         LocalDateTime departureDate,
                         LocalDateTime arrivalDate,
                         LocalDateTime returnDepartureDate,
                         LocalDateTime returnArrivalDate,
                         Double price,
                         String airline,
                         String flightNumber) {
        this.origin = origin;
        this.destination = destination;
        this.departureDate = departureDate;
        this.arrivalDate = arrivalDate;
        this.returnDepartureDate = returnDepartureDate;
        this.returnArrivalDate = returnArrivalDate;
        this.price = price;
        this.airline = airline;
        this.flightNumber = flightNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalDateTime getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDateTime departureDate) {
        this.departureDate = departureDate;
    }

    public LocalDateTime getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(LocalDateTime arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public LocalDateTime getReturnDepartureDate() {
        return returnDepartureDate;
    }

    public void setReturnDepartureDate(LocalDateTime returnDepartureDate) {
        this.returnDepartureDate = returnDepartureDate;
    }

    public LocalDateTime getReturnArrivalDate() {
        return returnArrivalDate;
    }

    public void setReturnArrivalDate(LocalDateTime returnArrivalDate) {
        this.returnArrivalDate = returnArrivalDate;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    @Override
    public String getBookingSummary() {
        String outbound = departureDate != null && arrivalDate != null
                ? "Outbound: " + departureDate + " to " + arrivalDate
                : "Outbound time TBD";
        String inbound = returnDepartureDate != null && returnArrivalDate != null
                ? " Return: " + returnDepartureDate + " to " + returnArrivalDate
                : "";
        return "Flight " + flightNumber + " with " + airline + " from " + origin + " to " + destination + " (" + outbound + "." + inbound + ") $" + price;
    }
}
