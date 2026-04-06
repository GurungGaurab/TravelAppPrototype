package com.example.TravelApp.repository;

import com.example.TravelApp.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByTripId(Long tripId);
}
