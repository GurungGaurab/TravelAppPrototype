package com.example.TravelApp.controller;

import com.example.TravelApp.model.Activity;
import com.example.TravelApp.model.FlightBooking;
import com.example.TravelApp.model.HotelBooking;
import com.example.TravelApp.model.SharedItinerary;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.service.ActivityService;
import com.example.TravelApp.service.SharedItineraryService;
import com.example.TravelApp.service.TripService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SharedItineraryController {
    private static final DateTimeFormatter FLOW_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final SharedItineraryService sharedItineraryService;
    private final TripService tripService;
    private final ActivityService activityService;

    public SharedItineraryController(SharedItineraryService sharedItineraryService,
                                     TripService tripService,
                                     ActivityService activityService) {
        this.sharedItineraryService = sharedItineraryService;
        this.tripService = tripService;
        this.activityService = activityService;
    }

    @GetMapping("/shared-itinerary")
    public String sharedItineraryHub(Model model, Principal principal) {
        model.addAttribute("trips", principal != null ? tripService.getTripsForUser(principal.getName()) : java.util.List.of());
        model.addAttribute("sharedItineraries", sharedItineraryService.getAllSharedItineraries());
        return "shared-itinerary";
    }

    @GetMapping("/shared-itinerary/open")
    public String openSharedItineraryByToken(@RequestParam String shareToken) {
        return "redirect:/shared-itinerary/" + shareToken.trim();
    }

    @PostMapping("/shared-itinerary")
    public String createSharedItinerary(@RequestParam Long tripId, Principal principal) {
        SharedItinerary sharedItinerary = sharedItineraryService.createSharedItinerary(tripId, principal.getName());
        return "redirect:/trips/" + tripId + "?shared=" + sharedItinerary.getShareToken();
    }

    @GetMapping("/shared-itinerary/{shareToken}")
    public String viewSharedItinerary(@PathVariable String shareToken) {
        return "redirect:/shared-itinerary/" + shareToken + "/flow";
    }

    @Transactional(readOnly = true)
    @GetMapping("/shared-itinerary/{shareToken}/flow")
    public String viewSharedTripFlow(@PathVariable String shareToken, Model model) {
        try {
            SharedItinerary sharedItinerary = sharedItineraryService.getByShareToken(shareToken);
            Trip trip = tripService.getTripById(sharedItinerary.getTrip().getId());

            FlightBooking selectedFlight = trip.getBookings() != null ? trip.getBookings().stream()
                    .filter(FlightBooking.class::isInstance)
                    .map(FlightBooking.class::cast)
                    .findFirst()
                    .orElse(null) : null;
            HotelBooking selectedHotel = trip.getBookings() != null ? trip.getBookings().stream()
                    .filter(HotelBooking.class::isInstance)
                    .map(HotelBooking.class::cast)
                    .findFirst()
                    .orElse(null) : null;

            model.addAttribute("sharedItinerary", sharedItinerary);
            model.addAttribute("trip", trip);
            model.addAttribute("selectedFlightBooking", selectedFlight);
            model.addAttribute("selectedHotelBooking", selectedHotel);
            List<Activity> sortedActivities = activityService.getOrderedActivities(trip);
            model.addAttribute("sortedActivities", sortedActivities);
            model.addAttribute("activityDayLabels", buildActivityDayLabels(trip, sortedActivities));
            model.addAttribute("hasActivities", !sortedActivities.isEmpty());
            model.addAttribute("flowItems", buildFlowItems(trip, selectedFlight, selectedHotel));
            return "shared-trip-flow";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", e.getClass().getSimpleName() + ": " + e.getMessage());
            return "error";
        }
    }

    private List<FlowItem> buildFlowItems(Trip trip, FlightBooking flightBooking, HotelBooking hotelBooking) {
        List<FlowItem> items = new ArrayList<>();

        if (flightBooking != null) {
            items.add(new FlowItem(
                    flightBooking.getDepartureDate(),
                    "Flight",
                    "Depart " + flightBooking.getOrigin() + " to " + flightBooking.getDestination(),
                    flightBooking.getAirline() + " " + flightBooking.getFlightNumber() + " · " + formatDateTimeRange(flightBooking.getDepartureDate(), flightBooking.getArrivalDate())
            ));
        }

        if (hotelBooking != null && hotelBooking.getCheckInDate() != null) {
            items.add(new FlowItem(
                    hotelBooking.getCheckInDate().atTime(LocalTime.of(15, 0)),
                    "Stay",
                    "Check in to " + hotelBooking.getHotelName(),
                    hotelBooking.getLocation() + " · " + hotelBooking.getCheckInDate() + " to " + hotelBooking.getCheckOutDate()
            ));
        }

        if (trip.getItinerary() != null && trip.getItinerary().getActivities() != null) {
            for (Activity activity : trip.getItinerary().getActivities()) {
                LocalDate activityDate = activity.getDate() != null ? activity.getDate() : trip.getStartDate();
                LocalTime activityTime = activity.getTime() != null ? activity.getTime() : LocalTime.of(10, 0);
                String loc = activity.getLocation() != null ? activity.getLocation() : "";
                String desc = activity.getDescription() != null && !activity.getDescription().isBlank() ? " · " + activity.getDescription() : "";
                items.add(new FlowItem(
                        activityDate.atTime(activityTime),
                        "Activity",
                        activity.getActivityName() != null ? activity.getActivityName() : "Activity",
                        loc + desc
                ));
            }
        }

        if (hotelBooking != null && hotelBooking.getCheckOutDate() != null) {
            items.add(new FlowItem(
                    hotelBooking.getCheckOutDate().atTime(LocalTime.of(11, 0)),
                    "Stay",
                    "Check out from " + hotelBooking.getHotelName(),
                    hotelBooking.getLocation() + " · Check-out day"
            ));
        }

        if (flightBooking != null && flightBooking.getReturnDepartureDate() != null) {
            items.add(new FlowItem(
                    flightBooking.getReturnDepartureDate(),
                    "Flight",
                    "Return from " + flightBooking.getDestination() + " to " + flightBooking.getOrigin(),
                    flightBooking.getAirline() + " " + flightBooking.getFlightNumber() + " · " + formatDateTimeRange(flightBooking.getReturnDepartureDate(), flightBooking.getReturnArrivalDate())
            ));
        }

        items.sort(Comparator.comparing(FlowItem::dateTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return items;
    }

    private String formatDateTimeRange(LocalDateTime start, LocalDateTime end) {
        String startText = start != null ? start.toLocalDate() + " " + start.toLocalTime().format(FLOW_TIME_FORMAT) : "TBD";
        String endText = end != null ? end.toLocalDate() + " " + end.toLocalTime().format(FLOW_TIME_FORMAT) : "TBD";
        return startText + " to " + endText;
    }

    private Map<Long, String> buildActivityDayLabels(Trip trip, List<Activity> activities) {
        Map<Long, String> labels = new LinkedHashMap<>();
        if (trip == null || trip.getStartDate() == null) {
            return labels;
        }

        for (Activity activity : activities) {
            if (activity.getId() == null || activity.getDate() == null) {
                continue;
            }
            long dayNumber = ChronoUnit.DAYS.between(trip.getStartDate(), activity.getDate()) + 1;
            labels.put(activity.getId(), "Day " + dayNumber);
        }
        return labels;
    }

    public record FlowItem(
            LocalDateTime dateTime,
            String type,
            String title,
            String details
    ) {
    }
}
