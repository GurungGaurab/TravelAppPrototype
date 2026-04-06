package com.example.TravelApp.controller;

import com.example.TravelApp.service.ActivityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Controller
public class ActivityController {

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
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Time must use HH:mm format");
        }
    }
}
