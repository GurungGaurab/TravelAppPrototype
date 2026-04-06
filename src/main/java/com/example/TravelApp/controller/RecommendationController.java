package com.example.TravelApp.controller;

import com.example.TravelApp.service.RecommendationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/recommendations/add-to-itinerary")
    public String addToItinerary(@RequestParam Long recommendationId,
                                 @RequestParam Long tripId,
                                 Principal principal) {
        recommendationService.addRecommendationToItinerary(recommendationId, principal.getName());
        return "redirect:/trips/" + tripId + "#activities-section";
    }
}
