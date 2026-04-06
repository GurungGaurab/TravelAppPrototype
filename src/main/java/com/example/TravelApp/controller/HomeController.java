package com.example.TravelApp.controller;

import com.example.TravelApp.service.SharedItineraryService;
import com.example.TravelApp.service.TripService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class HomeController {

    private final TripService tripService;
    private final SharedItineraryService sharedItineraryService;

    public HomeController(TripService tripService,
                          SharedItineraryService sharedItineraryService) {
        this.tripService = tripService;
        this.sharedItineraryService = sharedItineraryService;
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        model.addAttribute("sharedItineraries", sharedItineraryService.getAllSharedItineraries());
        if (principal != null) {
            model.addAttribute("trips", tripService.getTripsForUser(principal.getName()));
        } else {
            model.addAttribute("trips", java.util.List.of());
        }
        return "index";
    }

    @GetMapping("/homepage-preview")
    public String homepagePreview(Model model, Principal principal) {
        model.addAttribute("sharedItineraries", sharedItineraryService.getAllSharedItineraries());
        if (principal != null) {
            model.addAttribute("trips", tripService.getTripsForUser(principal.getName()));
        } else {
            model.addAttribute("trips", java.util.List.of());
        }
        return "index";
    }
}
