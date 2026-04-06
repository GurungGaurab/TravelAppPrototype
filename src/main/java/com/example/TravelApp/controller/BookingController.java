package com.example.TravelApp.controller;

import com.example.TravelApp.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;

@Controller
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/bookings")
    public String createBooking(@RequestParam Long tripId,
                                @RequestParam String bookingType,
                                @RequestParam String primaryName,
                                @RequestParam String secondaryValue,
                                @RequestParam Double price,
                                @RequestParam LocalDate date,
                                Principal principal) {
        bookingService.createMockBooking(tripId, principal.getName(), bookingType, primaryName, secondaryValue, price, date);
        return "redirect:/trips/" + tripId;
    }

    @PostMapping("/bookings/demo")
    public String createBookingFromDemoOption(@RequestParam Long tripId,
                                              @RequestParam String bookingType,
                                              @RequestParam String optionId,
                                              Principal principal) {
        bookingService.createBookingFromOption(tripId, principal.getName(), bookingType, optionId);
        return "redirect:/trips/" + tripId;
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancelBooking(@org.springframework.web.bind.annotation.PathVariable Long bookingId,
                                @RequestParam Long tripId,
                                Principal principal) {
        bookingService.cancelBooking(tripId, bookingId, principal.getName());
        return "redirect:/trips/" + tripId;
    }
}
