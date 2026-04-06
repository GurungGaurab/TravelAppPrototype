package com.example.TravelApp.repository;

import com.example.TravelApp.model.Bookings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingsRepository extends JpaRepository<Bookings, Long> {
}
