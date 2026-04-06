package com.example.TravelApp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.TravelApp.controller.HomeController;
import com.example.TravelApp.controller.SharedItineraryController;
import com.example.TravelApp.controller.TripController;
import com.example.TravelApp.service.TripService;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.repository.TripRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TravelAppApplicationTests {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripService tripService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @Test
    void keyControllersAreRegistered() {
        assertNotNull(applicationContext.getBean(HomeController.class));
        assertNotNull(applicationContext.getBean(TripController.class));
        assertNotNull(applicationContext.getBean(SharedItineraryController.class));
    }

    @Test
    void servicesAreRegistered() {
        assertNotNull(tripService);
    }

    @Test
    void seededTripExists() {
        Trip trip = tripRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Expected a seeded trip"));
        assertNotNull(trip.getId());
        assertTrue(trip.getTripName() != null && !trip.getTripName().isBlank());
    }

    @Test
    void tripServiceReturnsTrips() {
        assertTrue(!tripService.getAllTrips().isEmpty());
    }
}
