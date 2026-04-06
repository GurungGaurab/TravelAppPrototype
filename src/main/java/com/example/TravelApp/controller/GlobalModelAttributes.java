package com.example.TravelApp.controller;

import com.example.TravelApp.model.User;
import com.example.TravelApp.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserService userService;

    public GlobalModelAttributes(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("authenticated")
    public boolean authenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication authentication) {
        if (!authenticated(authentication)) {
            return null;
        }

        try {
            return userService.getUserByEmail(authentication.getName());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
