package com.example.TravelApp.controller;

import com.example.TravelApp.service.ActivityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Controller
public class ActivityController {
    private static final List<DateTimeFormatter> SUPPORTED_TIME_FORMATS = List.of(
            DateTimeFormatter.ofPattern("H"),
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("h a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("ha", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)
    );

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/activities")
    public String createActivity(@RequestParam Long tripId,
                                 @RequestParam String activityName,
                                 @RequestParam String description,
                                 @RequestParam LocalDate date,
                                 @RequestParam String location,
                                 @RequestParam(required = false) Double amount,
                                 Principal principal) {
        activityService.addActivityToTrip(
                tripId,
                principal.getName(),
                activityName,
                description,
                date,
                null,
                location,
                amount,
                "Activities",
                activityService.plannedActivityExpenseDescription(activityName)
        );
        return "redirect:/trips/" + tripId + "#activities-section";
    }

    @PostMapping("/activities/{activityId}/delete")
    public String deleteActivity(@PathVariable Long activityId,
                                 @RequestParam Long tripId,
                                 Principal principal) {
        activityService.removeActivityFromTrip(tripId, activityId, principal.getName());
        return "redirect:/trips/" + tripId + "#activities-section";
    }

    @PostMapping("/activities/{activityId}/edit")
    public String editActivity(@PathVariable Long activityId,
                               @RequestParam Long tripId,
                               @RequestParam String activityName,
                               @RequestParam String description,
                               @RequestParam LocalDate date,
                               @RequestParam(required = false) String time,
                               @RequestParam String location,
                               @RequestParam(required = false) Double amount,
                               @RequestParam(defaultValue = "details") String view,
                               Principal principal) {
        activityService.updateActivity(
                tripId,
                activityId,
                principal.getName(),
                activityName,
                description,
                date,
                parseTime(time),
                location,
                amount
        );
        if ("summary".equalsIgnoreCase(view) || "flow".equalsIgnoreCase(view)) {
            return "redirect:" + activityRedirectPath(tripId, view);
        }
        return "redirect:/trips/" + tripId + "#activities-section";
    }

    @PostMapping("/activities/{activityId}/move-up")
    public String moveActivityUp(@PathVariable Long activityId,
                                 @RequestParam Long tripId,
                                 @RequestParam(defaultValue = "details") String view,
                                 Principal principal) {
        activityService.moveActivity(tripId, activityId, principal.getName(), true);
        if ("summary".equalsIgnoreCase(view) || "flow".equalsIgnoreCase(view)) {
            return "redirect:" + activityRedirectPath(tripId, view);
        }
        return "redirect:/trips/" + tripId + "#activities-section";
    }

    @PostMapping("/activities/{activityId}/move-down")
    public String moveActivityDown(@PathVariable Long activityId,
                                   @RequestParam Long tripId,
                                   @RequestParam(defaultValue = "details") String view,
                                   Principal principal) {
        activityService.moveActivity(tripId, activityId, principal.getName(), false);
        if ("summary".equalsIgnoreCase(view) || "flow".equalsIgnoreCase(view)) {
            return "redirect:" + activityRedirectPath(tripId, view);
        }
        return "redirect:/trips/" + tripId + "#activities-section";
    }

    private String activityRedirectPath(Long tripId, String view) {
        if ("summary".equalsIgnoreCase(view) || "flow".equalsIgnoreCase(view)) {
            return "/trips/" + tripId + "/flow";
        }
        return "/trips/" + tripId;
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalizedValue = normalizeTimeInput(value);
        for (DateTimeFormatter formatter : SUPPORTED_TIME_FORMATS) {
            try {
                return LocalTime.parse(normalizedValue, formatter);
            } catch (DateTimeParseException exception) {
                // Try the next supported format.
            }
        }
        throw new IllegalArgumentException("Time must use formats like 2:00 PM or 14:00");
    }

    private String normalizeTimeInput(String value) {
        String normalized = value.trim()
                .toUpperCase(Locale.ENGLISH)
                .replace('.', ':')
                .replaceAll("\\s+", " ");
        normalized = normalized.replaceAll("(?<=\\d)(AM|PM)$", " $1");

        if (normalized.matches("^\\d{1,2} [AP]M$")) {
            normalized = normalized.replace(" AM", ":00 AM").replace(" PM", ":00 PM");
        }

        if ((normalized.endsWith(" AM") || normalized.endsWith(" PM"))
                && normalized.matches("^\\d{1,2}:\\d{2} [AP]M$")) {
            int hour = Integer.parseInt(normalized.substring(0, normalized.indexOf(':')));
            if (hour > 12) {
                return normalized.substring(0, normalized.indexOf(' '));
            }
        }
        return normalized;
    }
}
