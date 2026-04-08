package com.example.TravelApp.service;

import com.example.TravelApp.model.Activity;
import com.example.TravelApp.model.FlightBooking;
import com.example.TravelApp.model.Itinerary;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.repository.ActivityRepository;
import com.example.TravelApp.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ActivityService {
    private static final LocalTime DEFAULT_ACTIVITY_START = LocalTime.of(10, 0);
    private static final LocalTime AUTO_SCHEDULE_LATEST_START = LocalTime.of(20, 0);
    private static final LocalTime MANUAL_LATEST_START = LocalTime.of(20, 0);
    private static final int ACTIVITY_GAP_MINUTES = 30;
    private static final LocalTime LATE_ARRIVAL_CUTOFF = LocalTime.of(18, 0);
    private static final int ARRIVAL_BUFFER_HOURS = 1;
    private static final int RETURN_FLIGHT_BUFFER_HOURS = 2;
    private static final int ACTIVITY_DURATION_HOURS = 3;

    private final ActivityRepository activityRepository;
    private final TripRepository tripRepository;
    private final BudgetService budgetService;

    public ActivityService(ActivityRepository activityRepository,
                           TripRepository tripRepository,
                           BudgetService budgetService) {
        this.activityRepository = activityRepository;
        this.tripRepository = tripRepository;
        this.budgetService = budgetService;
    }

    @Transactional
    public boolean addActivityToTrip(Long tripId,
                                     String ownerEmail,
                                     String activityName,
                                     String description,
                                     LocalDate date,
                                     LocalTime time,
                                     String location) {
        return addActivityToTrip(tripId, ownerEmail, activityName, description, date, time, location, null, null, null);
    }

    @Transactional
    public boolean addActivityToTrip(Long tripId,
                                     String ownerEmail,
                                     String activityName,
                                     String description,
                                     LocalDate date,
                                     LocalTime time,
                                     String location,
                                     Double amount,
                                     String expenseCategory,
                                     String expenseDescription) {
        return addActivityToTrip(
                tripId,
                ownerEmail,
                activityName,
                description,
                date,
                time,
                location,
                amount,
                expenseCategory,
                expenseDescription,
                true
        );
    }

    @Transactional
    public boolean addActivityToTrip(Long tripId,
                                     String ownerEmail,
                                     String activityName,
                                     String description,
                                     LocalDate date,
                                     LocalTime time,
                                     String location,
                                     Double amount,
                                     String expenseCategory,
                                     String expenseDescription,
                                     boolean keepRequestedDate) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Itinerary itinerary = trip.getItinerary();
        if (itinerary == null) {
            itinerary = new Itinerary("Auto-created itinerary for " + trip.getTripName());
            trip.setItinerary(itinerary);
        }

        String normalizedActivityName = normalizeText(activityName);
        String normalizedLocation = normalizeText(location);
        String normalizedDescription = normalizeOptionalText(description);

        boolean alreadyExists = itinerary.getActivities().stream()
                .anyMatch(activity ->
                        safeEquals(activity.getActivityName(), normalizedActivityName)
                                && safeEquals(activity.getLocation(), normalizedLocation));

        if (alreadyExists) {
            return false;
        }

        Double normalizedAmount = normalizeAmount(amount);
        ActivitySlot resolvedSlot = resolveActivitySlot(trip, itinerary, date, time, null, !keepRequestedDate);
        validateActivityWithinTrip(trip, resolvedSlot.date());
        Activity activity = new Activity(
                normalizedActivityName,
                normalizedDescription,
                resolvedSlot.date(),
                resolvedSlot.time(),
                normalizedLocation,
                normalizedAmount
        );
        activity.setDisplayOrder(nextDisplayOrder(itinerary, resolvedSlot.date()));
        itinerary.addActivity(activity);

        tripRepository.save(trip);
        if (expenseCategory != null && expenseDescription != null && normalizedAmount != null) {
            budgetService.addAutomaticExpenseToTrip(
                    tripId,
                    ownerEmail,
                    expenseCategory,
                    normalizedAmount,
                    resolvedSlot.date() != null ? resolvedSlot.date() : LocalDate.now(),
                    expenseDescription
            );
        }
        return true;
    }

    @Transactional
    public void cleanupDuplicateActivitiesForTrip(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Itinerary itinerary = trip.getItinerary();
        if (itinerary == null || itinerary.getActivities() == null || itinerary.getActivities().isEmpty()) {
            return;
        }

        Set<String> seenKeys = new HashSet<>();
        itinerary.getActivities().sort(Comparator
                .comparing(Activity::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Activity::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Activity::getTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(activity -> safeLower(activity.getActivityName()))
                .thenComparing(activity -> safeLower(activity.getLocation())));

        var iterator = itinerary.getActivities().iterator();
        while (iterator.hasNext()) {
            Activity activity = iterator.next();
            String key = activityKey(activity.getActivityName(), activity.getLocation());
            if (!seenKeys.add(key)) {
                iterator.remove();
                activity.setItinerary(null);
                activityRepository.delete(activity);
            }
        }

        normalizeAllActivityOrders(itinerary);
        tripRepository.save(trip);
    }

    public LocalTime getNextSuggestedRecommendationTime(Long tripId, String ownerEmail, LocalDate targetDate) {
        return getNextSuggestedRecommendationSlot(tripId, ownerEmail, targetDate).time();
    }

    public ActivitySlot getNextSuggestedRecommendationSlot(Long tripId, String ownerEmail, LocalDate targetDate) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Itinerary itinerary = trip.getItinerary();
        return resolveActivitySlot(trip, itinerary, targetDate, null, null, true);
    }

    @Transactional
    public void removeActivityFromTrip(Long tripId, Long activityId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Itinerary itinerary = trip.getItinerary();
        if (itinerary == null || itinerary.getActivities() == null) {
            throw new IllegalArgumentException("Activity not found for this trip");
        }

        Activity activity = itinerary.getActivities().stream()
                .filter(candidate -> candidate.getId() != null && candidate.getId().equals(activityId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Activity not found for this trip"));

        String activityName = activity.getActivityName();
        LocalDate activityDate = activity.getDate();
        itinerary.removeActivity(activity);
        normalizeActivityOrder(itinerary, activityDate);
        tripRepository.save(trip);
        budgetService.removeAutomaticExpenseFromTrip(
                tripId,
                ownerEmail,
                "Activities",
                plannedActivityExpenseDescription(activityName)
        );
        budgetService.removeAutomaticExpenseFromTrip(
                tripId,
                ownerEmail,
                "Attractions",
                "Attraction booking: " + activityName
        );
    }

    @Transactional
    public void moveActivity(Long tripId, Long activityId, String ownerEmail, boolean moveUp) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Itinerary itinerary = trip.getItinerary();
        if (itinerary == null || itinerary.getActivities() == null || itinerary.getActivities().isEmpty()) {
            throw new IllegalArgumentException("Activity not found for this trip");
        }

        Activity activity = itinerary.getActivities().stream()
                .filter(candidate -> candidate.getId() != null && candidate.getId().equals(activityId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Activity not found for this trip"));

        LocalDate targetDate = activity.getDate();
        normalizeActivityOrder(itinerary, targetDate);

        List<Activity> sameDayActivities = itinerary.getActivities().stream()
                .filter(candidate -> sameDate(candidate.getDate(), targetDate))
                .sorted(activityComparator())
                .toList();

        int currentIndex = -1;
        for (int index = 0; index < sameDayActivities.size(); index++) {
            if (sameDayActivities.get(index).getId() != null && sameDayActivities.get(index).getId().equals(activityId)) {
                currentIndex = index;
                break;
            }
        }

        if (currentIndex < 0) {
            throw new IllegalArgumentException("Activity not found for this trip");
        }

        int swapIndex = moveUp ? currentIndex - 1 : currentIndex + 1;
        if (swapIndex < 0 || swapIndex >= sameDayActivities.size()) {
            return;
        }

        Activity other = sameDayActivities.get(swapIndex);
        Integer currentOrder = activity.getDisplayOrder();
        activity.setDisplayOrder(other.getDisplayOrder());
        other.setDisplayOrder(currentOrder);
        LocalTime currentTime = activity.getTime();
        activity.setTime(other.getTime());
        other.setTime(currentTime);
        normalizeActivityOrder(itinerary, targetDate);
        tripRepository.save(trip);
    }

    public void updateActivity(Long tripId,
                               Long activityId,
                               String ownerEmail,
                               String activityName,
                               String description,
                               LocalDate date,
                               LocalTime time,
                               String location,
                               Double amount) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Itinerary itinerary = trip.getItinerary();
        if (itinerary == null || itinerary.getActivities() == null) {
            throw new IllegalArgumentException("Activity not found for this trip");
        }

        Activity activity = itinerary.getActivities().stream()
                .filter(candidate -> candidate.getId() != null && candidate.getId().equals(activityId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Activity not found for this trip"));

        String previousActivityName = activity.getActivityName();
        LocalDate previousDate = activity.getDate();
        boolean allowNextDayOverflow = time != null;
        ActivitySlot resolvedSlot = resolveActivitySlot(trip, itinerary, date, time, activityId, allowNextDayOverflow);
        validateActivityWithinTrip(trip, resolvedSlot.date());
        Integer nextOrderForNewDate = sameDate(previousDate, resolvedSlot.date())
                ? activity.getDisplayOrder()
                : nextDisplayOrderExcludingActivity(itinerary, resolvedSlot.date(), activityId);
        Double normalizedAmount = normalizeAmount(amount);
        String normalizedActivityName = normalizeText(activityName);
        String normalizedLocation = normalizeText(location);
        String normalizedDescription = normalizeOptionalText(description);

        activity.setActivityName(normalizedActivityName);
        activity.setDescription(normalizedDescription);
        activity.setDate(resolvedSlot.date());
        activity.setTime(resolvedSlot.time());
        activity.setLocation(normalizedLocation);
        activity.setAmount(normalizedAmount);

        if (!sameDate(previousDate, resolvedSlot.date())) {
            activity.setDisplayOrder(nextOrderForNewDate);
        }

        normalizeActivityOrder(itinerary, previousDate);
        normalizeActivityOrder(itinerary, resolvedSlot.date());
        tripRepository.save(trip);

        budgetService.removeAutomaticExpenseFromTrip(
                tripId,
                ownerEmail,
                "Activities",
                plannedActivityExpenseDescription(previousActivityName)
        );

        if (normalizedAmount != null) {
            budgetService.addAutomaticExpenseToTrip(
                    tripId,
                    ownerEmail,
                    "Activities",
                    normalizedAmount,
                    resolvedSlot.date() != null ? resolvedSlot.date() : LocalDate.now(),
                    plannedActivityExpenseDescription(normalizedActivityName)
            );
        }
    }

    public List<Activity> getOrderedActivitiesForTrip(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));
        return getOrderedActivities(trip);
    }

    public List<Activity> getOrderedActivities(Trip trip) {
        if (trip.getItinerary() == null || trip.getItinerary().getActivities() == null) {
            return List.of();
        }

        List<Activity> ordered = new ArrayList<>(trip.getItinerary().getActivities());
        ordered.sort(activityComparator());
        return ordered;
    }

    public double getPlannedActivityAmount(Trip trip) {
        return getOrderedActivities(trip).stream()
                .mapToDouble(activity -> activity.getAmount() != null ? activity.getAmount() : 0.0)
                .sum();
    }

    private void normalizeAllActivityOrders(Itinerary itinerary) {
        if (itinerary.getActivities() == null || itinerary.getActivities().isEmpty()) {
            return;
        }

        itinerary.getActivities().stream()
                .map(Activity::getDate)
                .distinct()
                .forEach(date -> normalizeActivityOrder(itinerary, date));
    }

    private void normalizeActivityOrder(Itinerary itinerary, LocalDate targetDate) {
        if (itinerary == null || itinerary.getActivities() == null) {
            return;
        }

        List<Activity> sameDay = itinerary.getActivities().stream()
                .filter(activity -> sameDate(activity.getDate(), targetDate))
                .sorted(activityComparator())
                .toList();

        for (int index = 0; index < sameDay.size(); index++) {
            sameDay.get(index).setDisplayOrder(index + 1);
        }
    }

    private int nextDisplayOrder(Itinerary itinerary, LocalDate targetDate) {
        return (int) itinerary.getActivities().stream()
                .filter(activity -> sameDate(activity.getDate(), targetDate))
                .count() + 1;
    }

    private int nextDisplayOrderExcludingActivity(Itinerary itinerary, LocalDate targetDate, Long activityId) {
        return (int) itinerary.getActivities().stream()
                .filter(activity -> sameDate(activity.getDate(), targetDate))
                .filter(activity -> activity.getId() == null || !activity.getId().equals(activityId))
                .count() + 1;
    }

    private ActivitySlot resolveActivitySlot(Trip trip,
                                             Itinerary itinerary,
                                             LocalDate targetDate,
                                             LocalTime requestedTime,
                                             Long excludeActivityId,
                                             boolean allowNextDayOverflow) {
        LocalDate safeDate = targetDate != null ? targetDate : LocalDate.now();
        if (trip != null && trip.getEndDate() != null && safeDate.isAfter(trip.getEndDate())) {
            throw new IllegalArgumentException("You are going over your stay");
        }
        LocalTime safeRequestedTime = normalizeRequestedTime(requestedTime);
        FlightBooking flightBooking = getSelectedFlightBooking(trip);
        LocalTime earliestAllowedStart = getEarliestAllowedStart(flightBooking, safeDate);
        LocalTime latestAllowedStart = getLatestAllowedStart(flightBooking, safeDate);

        if (safeRequestedTime != null) {
            boolean isLastTripDay = trip != null && trip.getEndDate() != null && safeDate.equals(trip.getEndDate());
            if (isLastTripDay && safeRequestedTime.isAfter(latestAllowedStart)) {
                throw new IllegalArgumentException("You are going over your stay");
            }
            return new ActivitySlot(safeDate, safeRequestedTime);
        }

        if (itinerary == null || itinerary.getActivities() == null) {
            if (earliestAllowedStart.isAfter(latestAllowedStart)) {
                if (!allowNextDayOverflow) {
                    throw scheduleFullException(safeDate);
                }
                return resolveActivitySlot(trip, itinerary, safeDate.plusDays(1), null, excludeActivityId, true);
            }
            return new ActivitySlot(safeDate, maxTime(DEFAULT_ACTIVITY_START, earliestAllowedStart));
        }

        LocalTime nextTime = findNextAvailableStart(itinerary, safeDate, earliestAllowedStart, latestAllowedStart, excludeActivityId);
        if (nextTime == null) {
            if (!allowNextDayOverflow) {
                throw scheduleFullException(safeDate);
            }
            return resolveActivitySlot(trip, itinerary, safeDate.plusDays(1), null, excludeActivityId, true);
        }

        return new ActivitySlot(safeDate, nextTime);
    }

    private IllegalArgumentException scheduleFullException(LocalDate date) {
        return new IllegalArgumentException("Schedule full for " + date + ". Please pick another date or time.");
    }

    private LocalTime findNextAvailableStart(Itinerary itinerary,
                                             LocalDate targetDate,
                                             LocalTime earliestAllowedStart,
                                             LocalTime latestAllowedStart,
                                             Long excludeActivityId) {
        if (earliestAllowedStart.isAfter(latestAllowedStart)) {
            return null;
        }

        LocalTime candidate = maxTime(DEFAULT_ACTIVITY_START, earliestAllowedStart);
        List<Activity> sameDayActivities = itinerary.getActivities().stream()
                .filter(activity -> sameDate(activity.getDate(), targetDate))
                .filter(activity -> excludeActivityId == null || activity.getId() == null || !activity.getId().equals(excludeActivityId))
                .filter(activity -> activity.getTime() != null && activity.getEndTime() != null)
                .sorted(Comparator.comparing(Activity::getTime))
                .toList();

        for (Activity existingActivity : sameDayActivities) {
            LocalTime existingStart = existingActivity.getTime();
            if (existingStart == null) {
                continue;
            }
            if (fitsBefore(candidate, existingStart) && !candidate.isAfter(latestAllowedStart)) {
                return candidate;
            }

            LocalTime nextCandidate = existingActivity.getEndTime();
            if (nextCandidate == null) {
                continue;
            }
            candidate = maxTime(candidate, nextCandidate.plusMinutes(ACTIVITY_GAP_MINUTES));
        }

        if (candidate.isAfter(latestAllowedStart)) {
            return null;
        }
        return candidate;
    }

    private boolean fitsBefore(LocalTime candidateStart, LocalTime nextActivityStart) {
        return !candidateStart.plusHours(ACTIVITY_DURATION_HOURS)
                .plusMinutes(ACTIVITY_GAP_MINUTES)
                .isAfter(nextActivityStart);
    }

    private Comparator<Activity> activityComparator() {
        return Comparator
                .comparing(Activity::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Activity::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Activity::getTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Activity::getActivityName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private boolean sameDate(LocalDate left, LocalDate right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    private boolean safeEquals(String left, String right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private String activityKey(String name, String location) {
        return safeLower(name) + "|" + safeLower(location);
    }

    public String plannedActivityExpenseDescription(String activityName) {
        return "Planned activity: " + activityName;
    }

    private Double normalizeAmount(Double amount) {
        if (amount == null || amount <= 0) {
            return null;
        }
        return amount;
    }

    private LocalTime normalizeRequestedTime(LocalTime time) {
        if (time == null || LocalTime.MIDNIGHT.equals(time)) {
            return null;
        }
        return time;
    }

    private void validateActivityWithinTrip(Trip trip, LocalDate scheduledDate) {
        if (trip == null || scheduledDate == null || trip.getEndDate() == null) {
            return;
        }
        if (scheduledDate.isAfter(trip.getEndDate())) {
            throw new IllegalArgumentException("You are going over your stay");
        }
    }

    private FlightBooking getSelectedFlightBooking(Trip trip) {
        if (trip == null || trip.getBookings() == null) {
            return null;
        }
        return trip.getBookings().stream()
                .filter(FlightBooking.class::isInstance)
                .map(FlightBooking.class::cast)
                .findFirst()
                .orElse(null);
    }

    private LocalTime getEarliestAllowedStart(FlightBooking flightBooking, LocalDate date) {
        if (flightBooking == null || date == null || flightBooking.getArrivalDate() == null) {
            return DEFAULT_ACTIVITY_START;
        }
        LocalDateTime arrival = flightBooking.getArrivalDate();
        if (!date.equals(arrival.toLocalDate())) {
            return DEFAULT_ACTIVITY_START;
        }
        LocalTime arrivalTime = arrival.toLocalTime();
        if (arrivalTime.isAfter(LATE_ARRIVAL_CUTOFF)) {
            return AUTO_SCHEDULE_LATEST_START.plusMinutes(1);
        }

        LocalDateTime earliestAfterArrival = arrival.plusHours(ARRIVAL_BUFFER_HOURS);
        if (!earliestAfterArrival.toLocalDate().equals(date)) {
            return AUTO_SCHEDULE_LATEST_START.plusMinutes(1);
        }
        return maxTime(DEFAULT_ACTIVITY_START, earliestAfterArrival.toLocalTime());
    }

    private LocalTime getLatestAllowedStart(FlightBooking flightBooking, LocalDate date) {
        LocalTime latest = AUTO_SCHEDULE_LATEST_START;
        if (flightBooking == null || date == null || flightBooking.getReturnDepartureDate() == null) {
            return latest;
        }
        LocalDateTime returnDeparture = flightBooking.getReturnDepartureDate();
        if (!date.equals(returnDeparture.toLocalDate())) {
            return latest;
        }
        LocalTime flightLimitedStart = returnDeparture.toLocalTime()
                .minusHours(RETURN_FLIGHT_BUFFER_HOURS)
                .minusHours(ACTIVITY_DURATION_HOURS);
        return flightLimitedStart.isBefore(DEFAULT_ACTIVITY_START) ? DEFAULT_ACTIVITY_START.minusMinutes(1) : minTime(latest, flightLimitedStart);
    }

    private LocalTime maxTime(LocalTime first, LocalTime second) {
        return first.isAfter(second) ? first : second;
    }

    private LocalTime minTime(LocalTime first, LocalTime second) {
        return first.isBefore(second) ? first : second;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Activity name and location are required");
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    public record ActivitySlot(LocalDate date, LocalTime time) {
    }
}
