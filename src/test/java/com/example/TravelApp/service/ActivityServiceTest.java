package com.example.TravelApp.service;

import com.example.TravelApp.model.Activity;
import com.example.TravelApp.model.Itinerary;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.model.User;
import com.example.TravelApp.repository.TripRepository;
import com.example.TravelApp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ActivityServiceTest {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void addActivityToTripUsesOpenGapBeforeLaterActivities() {
        String ownerEmail = "traveler@example.com";
        LocalDate tripDate = LocalDate.of(2026, 4, 10);

        User user = new User();
        user.setEmail(ownerEmail);
        user.setHashedPassword("secret");
        user.setName("Traveler");
        userRepository.save(user);

        Trip trip = new Trip("Tokyo", "Tokyo", tripDate, tripDate.plusDays(2), 1200.0);
        trip.setUser(user);

        Itinerary itinerary = new Itinerary("Trip plan");
        trip.setItinerary(itinerary);
        itinerary.addActivity(activity("Lunch reservation", "Shibuya", tripDate, LocalTime.of(14, 0), 1));
        itinerary.addActivity(activity("Evening show", "Shinjuku", tripDate, LocalTime.of(18, 0), 2));

        trip = tripRepository.save(trip);

        boolean added = activityService.addActivityToTrip(
                trip.getId(),
                ownerEmail,
                "Senso-ji Temple",
                "Morning visit",
                tripDate,
                null,
                "Asakusa",
                null,
                null,
                null
        );

        assertThat(added).isTrue();

        Trip savedTrip = tripRepository.findByIdAndUserEmail(trip.getId(), ownerEmail).orElseThrow();
        Activity addedActivity = savedTrip.getItinerary().getActivities().stream()
                .filter(activity -> "Senso-ji Temple".equals(activity.getActivityName()))
                .findFirst()
                .orElseThrow();

        assertThat(addedActivity.getDate()).isEqualTo(tripDate);
        assertThat(addedActivity.getTime()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void addActivityToTripFailsWhenSelectedDateScheduleIsFull() {
        String ownerEmail = "packed@example.com";
        LocalDate tripDate = LocalDate.of(2026, 4, 10);

        User user = new User();
        user.setEmail(ownerEmail);
        user.setHashedPassword("secret");
        user.setName("Packed Day");
        userRepository.save(user);

        Trip trip = new Trip("Tokyo", "Tokyo", tripDate, tripDate.plusDays(2), 1200.0);
        trip.setUser(user);

        Itinerary itinerary = new Itinerary("Packed trip plan");
        trip.setItinerary(itinerary);
        itinerary.addActivity(activity("Morning tour", "Asakusa", tripDate, LocalTime.of(10, 0), 1));
        itinerary.addActivity(activity("Lunch stop", "Shibuya", tripDate, LocalTime.of(13, 30), 2));
        itinerary.addActivity(activity("Evening show", "Shinjuku", tripDate, LocalTime.of(17, 0), 3));

        trip = tripRepository.save(trip);
        Long tripId = trip.getId();

        assertThatThrownBy(() -> activityService.addActivityToTrip(
                tripId,
                ownerEmail,
                "Night market",
                "Extra stop",
                tripDate,
                null,
                "Ueno",
                null,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Schedule full for 2026-04-10. Please pick another date or time.");

        Trip savedTrip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail).orElseThrow();
        assertThat(savedTrip.getItinerary().getActivities()).hasSize(3);
        assertThat(savedTrip.getItinerary().getActivities().stream()
                .noneMatch(activity -> "Night market".equals(activity.getActivityName()))).isTrue();
    }

    @Test
    void updateActivityKeepsManualTimeOnSameDayBeforeLastDay() {
        String ownerEmail = "edit-late@example.com";
        LocalDate firstDay = LocalDate.of(2026, 4, 10);

        User user = new User();
        user.setEmail(ownerEmail);
        user.setHashedPassword("secret");
        user.setName("Late Edit");
        userRepository.save(user);

        Trip trip = new Trip("Tokyo", "Tokyo", firstDay, firstDay.plusDays(2), 1200.0);
        trip.setUser(user);

        Itinerary itinerary = new Itinerary("Trip plan");
        trip.setItinerary(itinerary);
        Activity activity = activity("Temple visit", "Asakusa", firstDay, LocalTime.of(10, 0), 1);
        itinerary.addActivity(activity);

        trip = tripRepository.save(trip);
        Long tripId = trip.getId();
        Long activityId = trip.getItinerary().getActivities().get(0).getId();

        activityService.updateActivity(
                tripId,
                activityId,
                ownerEmail,
                "Temple visit",
                "Late night update",
                firstDay,
                LocalTime.of(22, 0),
                "Asakusa",
                null
        );

        Trip savedTrip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail).orElseThrow();
        Activity updatedActivity = savedTrip.getItinerary().getActivities().stream()
                .filter(item -> item.getId().equals(activityId))
                .findFirst()
                .orElseThrow();

        assertThat(updatedActivity.getDate()).isEqualTo(firstDay);
        assertThat(updatedActivity.getTime()).isEqualTo(LocalTime.of(22, 0));
    }

    @Test
    void updateActivityFailsOnLastDayWhenManualTimeExceedsStay() {
        String ownerEmail = "last-day@example.com";
        LocalDate onlyDay = LocalDate.of(2026, 4, 10);

        User user = new User();
        user.setEmail(ownerEmail);
        user.setHashedPassword("secret");
        user.setName("Last Day");
        userRepository.save(user);

        Trip trip = new Trip("Tokyo", "Tokyo", onlyDay, onlyDay, 1200.0);
        trip.setUser(user);

        Itinerary itinerary = new Itinerary("Trip plan");
        trip.setItinerary(itinerary);
        Activity activity = activity("Temple visit", "Asakusa", onlyDay, LocalTime.of(10, 0), 1);
        itinerary.addActivity(activity);

        trip = tripRepository.save(trip);
        Long tripId = trip.getId();
        Long activityId = trip.getItinerary().getActivities().get(0).getId();

        assertThatThrownBy(() -> activityService.updateActivity(
                tripId,
                activityId,
                ownerEmail,
                "Temple visit",
                "Late night update",
                onlyDay,
                LocalTime.of(22, 0),
                "Asakusa",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You are going over your stay");
    }

    private Activity activity(String name, String location, LocalDate date, LocalTime time, int displayOrder) {
        Activity activity = new Activity(name, "", date, time, location, null);
        activity.setDisplayOrder(displayOrder);
        return activity;
    }
}
