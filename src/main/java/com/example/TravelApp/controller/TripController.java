package com.example.TravelApp.controller;

import com.example.TravelApp.model.Activity;
import com.example.TravelApp.model.FlightBooking;
import com.example.TravelApp.model.HotelBooking;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.service.RecommendationService;
import com.example.TravelApp.service.SharedItineraryService;
import com.example.TravelApp.service.TripService;
import com.example.TravelApp.service.ActivityService;
import com.example.TravelApp.service.BookingService;
import com.example.TravelApp.service.BudgetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class TripController {
    private static final DateTimeFormatter FLOW_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final TripService tripService;
    private final RecommendationService recommendationService;
    private final SharedItineraryService sharedItineraryService;
    private final ActivityService activityService;
    private final BookingService bookingService;
    private final BudgetService budgetService;

    public TripController(TripService tripService,
                          RecommendationService recommendationService,
                          SharedItineraryService sharedItineraryService,
                          ActivityService activityService,
                          BookingService bookingService,
                          BudgetService budgetService) {
        this.tripService = tripService;
        this.recommendationService = recommendationService;
        this.sharedItineraryService = sharedItineraryService;
        this.activityService = activityService;
        this.bookingService = bookingService;
        this.budgetService = budgetService;
    }

    @GetMapping("/trips")
    public String trips(Model model, Principal principal) {
        model.addAttribute("trips", tripService.getTripsForUser(principal.getName()));
        return "trips";
    }

    @GetMapping("/trips/{id}")
    public String tripDetails(@PathVariable Long id, Model model, Principal principal) {
        activityService.cleanupDuplicateActivitiesForTrip(id, principal.getName());
        budgetService.cleanupDuplicateExpensesForTrip(id, principal.getName());
        recommendationService.cleanupDuplicateRecommendationsForTrip(id, principal.getName());
        Trip trip = tripService.getTripForUser(id, principal.getName());
        recommendationService.ensureRecommendationsForTrip(trip);
        model.addAttribute("trip", trip);
        model.addAttribute("recommendations", recommendationService.getPersonalizedRecommendationsForTrip(id, principal.getName()));
        model.addAttribute("sharedItineraries", sharedItineraryService.getSharedItinerariesForTripId(id));
        model.addAttribute("orderedActivities", activityService.getOrderedActivities(trip));
        model.addAttribute("activityPlannedAmount", activityService.getPlannedActivityAmount(trip));
        model.addAttribute("flightOptions", bookingService.getFlightOptionsForTrip(id, principal.getName()));
        model.addAttribute("hotelOptions", bookingService.getHotelOptionsForTrip(id, principal.getName()));
        model.addAttribute("selectedFlightBooking", bookingService.getSelectedFlightBooking(id, principal.getName()));
        model.addAttribute("selectedHotelBooking", bookingService.getSelectedHotelBooking(id, principal.getName()));
        model.addAttribute("displayExpenses", budgetService.getUniqueExpenses(trip.getBudget()));
        model.addAttribute("displaySpentAmount", budgetService.getDisplaySpentAmount(trip.getBudget()));
        model.addAttribute("displayRemainingAmount", budgetService.getDisplayRemainingAmount(trip.getBudget()));
        model.addAttribute("budgetSpentPercentage", budgetService.getSpentPercentage(trip.getBudget()));
        model.addAttribute("overBudget", budgetService.isOverBudget(trip.getBudget()));
        model.addAttribute("categorySpend", budgetService.getCategorySpend(trip.getBudget()));
        return "trip-details";
    }

    @GetMapping("/trips/{id}/flow")
    public String tripFlow(@PathVariable Long id, Model model, Principal principal) {
        activityService.cleanupDuplicateActivitiesForTrip(id, principal.getName());
        budgetService.cleanupDuplicateExpensesForTrip(id, principal.getName());
        recommendationService.cleanupDuplicateRecommendationsForTrip(id, principal.getName());

        Trip trip = tripService.getTripForUser(id, principal.getName());
        FlightBooking selectedFlight = bookingService.getSelectedFlightBooking(id, principal.getName());
        HotelBooking selectedHotel = bookingService.getSelectedHotelBooking(id, principal.getName());

        model.addAttribute("trip", trip);
        model.addAttribute("selectedFlightBooking", selectedFlight);
        model.addAttribute("selectedHotelBooking", selectedHotel);
        List<Activity> sortedActivities = activityService.getOrderedActivities(trip);
        model.addAttribute("sortedActivities", sortedActivities);
        model.addAttribute("activityDayLabels", buildActivityDayLabels(trip, sortedActivities));
        model.addAttribute("hasActivities", !sortedActivities.isEmpty());
        model.addAttribute("flowItems", buildFlowItems(trip, selectedFlight, selectedHotel));
        return "trip-flow";
    }

    @PostMapping("/trips")
    public String createTrip(@RequestParam String tripName,
                             @RequestParam String destination,
                             @RequestParam LocalDate startDate,
                             @RequestParam LocalDate endDate,
                             @RequestParam Double totalBudget,
                             Principal principal) {
        Trip createdTrip = tripService.createTrip(tripName, destination, startDate, endDate, totalBudget, principal.getName());
        recommendationService.ensureRecommendationsForTrip(createdTrip);
        return "redirect:/trips/" + createdTrip.getId();
    }

    @PostMapping("/trips/{id}/delete")
    public String deleteTrip(@PathVariable Long id, Principal principal) {
        tripService.deleteTripForUser(id, principal.getName());
        return "redirect:/trips";
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
