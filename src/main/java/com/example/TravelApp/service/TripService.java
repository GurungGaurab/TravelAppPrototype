package com.example.TravelApp.service;

import com.example.TravelApp.model.Budget;
import com.example.TravelApp.model.Itinerary;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.model.User;
import com.example.TravelApp.repository.TripRepository;
import com.example.TravelApp.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TripService(TripRepository tripRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void seedSampleTrip() {
        if (tripRepository.count() > 0) {
            return;
        }

        User demoUser = userRepository.findByEmail("demo@travelapp.com")
                .orElseGet(() -> userRepository.save(new User(
                        "demo@travelapp.com",
                        passwordEncoder.encode("password"),
                        "Demo User",
                        "Demo account for TravelApp.",
                        "City trips",
                        2500.0,
                        "Food, culture, sightseeing"
                )));

        Trip trip = new Trip(
                "Japan Spring Trip",
                "Tokyo",
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 17),
                2500.0
        );
        trip.setUser(demoUser);

        Itinerary itinerary = new Itinerary("7-day Tokyo trip with food, culture, and city exploration.");
        Budget budget = new Budget(2500.0, 0.0, 2500.0);

        trip.setItinerary(itinerary);
        trip.setBudget(budget);

        tripRepository.save(trip);
    }

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public List<Trip> getTripsForUser(String email) {
        return tripRepository.findByUserEmailOrderByStartDateAsc(email);
    }

    public Trip getTripById(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));
    }

    public Trip getTripForUser(Long id, String email) {
        return tripRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));
    }

    public Trip createTrip(String tripName,
                           String destination,
                           LocalDate startDate,
                           LocalDate endDate,
                           Double totalBudget,
                           String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Trip trip = new Trip(tripName, destination, startDate, endDate, totalBudget);
        trip.setUser(owner);

        Itinerary itinerary = new Itinerary("New itinerary for " + tripName);
        Budget budget = new Budget(totalBudget, 0.0, totalBudget);

        trip.setItinerary(itinerary);
        trip.setBudget(budget);

        return tripRepository.save(trip);
    }

    public void deleteTripForUser(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));
        tripRepository.delete(trip);
    }
}
