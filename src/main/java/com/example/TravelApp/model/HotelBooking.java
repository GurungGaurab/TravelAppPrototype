package com.example.TravelApp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "hotel_bookings")
public class HotelBooking extends Bookings {

    private String hotelName;
    private String location;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Double pricePerNight;

    public HotelBooking() {
    }

    public HotelBooking(String hotelName,
                        String location,
                        LocalDate checkInDate,
                        LocalDate checkOutDate,
                        Double pricePerNight) {
        this.hotelName = hotelName;
        this.location = location;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.pricePerNight = pricePerNight;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(Double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    @Override
    public String getBookingSummary() {
        return "Hotel " + hotelName + " in " + location + " ($" + pricePerNight + " per night)";
    }
}
