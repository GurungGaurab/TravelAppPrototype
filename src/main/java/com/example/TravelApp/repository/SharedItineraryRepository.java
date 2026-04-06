package com.example.TravelApp.repository;

import com.example.TravelApp.model.SharedItinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SharedItineraryRepository extends JpaRepository<SharedItinerary, Long> {

    Optional<SharedItinerary> findByShareToken(String shareToken);

    List<SharedItinerary> findAllByOrderByCreatedAtDesc();

    List<SharedItinerary> findByTripIdOrderByCreatedAtDesc(Long tripId);
}
