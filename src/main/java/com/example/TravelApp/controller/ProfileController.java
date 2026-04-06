package com.example.TravelApp.controller;

import com.example.TravelApp.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        model.addAttribute("profileUser", userService.getUserByEmail(principal.getName()));
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String name,
                                @RequestParam(defaultValue = "") String personalInfo,
                                @RequestParam(defaultValue = "") String travelPreferences,
                                @RequestParam(defaultValue = "0") Double budgetRange,
                                @RequestParam(defaultValue = "") String interests,
                                Principal principal) {
        userService.updateProfile(
                principal.getName(),
                name,
                personalInfo,
                travelPreferences,
                budgetRange,
                interests
        );
        return "redirect:/profile";
    }
}
