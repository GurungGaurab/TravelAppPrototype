package com.example.TravelApp.controller;

import com.example.TravelApp.service.SharedItineraryService;
import com.example.TravelApp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;
    private final SharedItineraryService sharedItineraryService;

    public AuthController(UserService userService, SharedItineraryService sharedItineraryService) {
        this.userService = userService;
        this.sharedItineraryService = sharedItineraryService;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("sharedItineraries", sharedItineraryService.getAllSharedItineraries());
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String name,
                           @RequestParam(defaultValue = "") String personalInfo,
                           @RequestParam(defaultValue = "") String travelPreferences,
                           @RequestParam(defaultValue = "0") Double budgetRange,
                           @RequestParam(defaultValue = "") String interests,
                           Model model) {
        try {
            userService.registerUser(email, password, name, personalInfo, travelPreferences, budgetRange, interests);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            return "register";
        }
    }
}
