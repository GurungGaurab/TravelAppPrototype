package com.example.TravelApp.repository;

import com.example.TravelApp.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByUserEmailOrderByStartDateAsc(String email);

    Optional<Trip> findByIdAndUserEmail(Long id, String email);
}
