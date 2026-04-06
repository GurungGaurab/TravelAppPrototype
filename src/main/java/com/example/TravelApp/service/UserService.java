package com.example.TravelApp.service;

import com.example.TravelApp.model.User;
import com.example.TravelApp.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String email,
                             String password,
                             String name,
                             String personalInfo,
                             String travelPreferences,
                             Double budgetRange,
                             String interests) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("An account with that email already exists.");
        }

        User user = new User(
                email,
                passwordEncoder.encode(password),
                name,
                personalInfo,
                travelPreferences,
                budgetRange,
                interests
        );
        return userRepository.save(user);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User updateProfile(String email,
                              String name,
                              String personalInfo,
                              String travelPreferences,
                              Double budgetRange,
                              String interests) {
        User user = getUserByEmail(email);
        user.setName(name);
        user.setPersonalInfo(personalInfo);
        user.setTravelPreferences(travelPreferences);
        user.setBudgetRange(budgetRange);
        user.setInterests(interests);
        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getHashedPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
