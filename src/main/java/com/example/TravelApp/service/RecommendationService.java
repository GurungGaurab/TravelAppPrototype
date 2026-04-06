package com.example.TravelApp.service;

import com.example.TravelApp.model.Recommendation;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.model.User;
import com.example.TravelApp.repository.RecommendationRepository;
import com.example.TravelApp.repository.TripRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final TripRepository tripRepository;
    private final ActivityService activityService;
    private final BudgetService budgetService;

    public RecommendationService(RecommendationRepository recommendationRepository,
                                 TripRepository tripRepository,
                                 ActivityService activityService,
                                 BudgetService budgetService) {
        this.recommendationRepository = recommendationRepository;
        this.tripRepository = tripRepository;
        this.activityService = activityService;
        this.budgetService = budgetService;
    }

    @PostConstruct
    @Transactional
    public void seedRecommendations() {
        if (tripRepository.count() == 0) {
            return;
        }

        for (Trip trip : tripRepository.findAll()) {
            ensureRecommendationsForTrip(trip);
        }
    }

    @Transactional
    public void addRecommendation(Trip trip, Recommendation recommendation) {
        recommendation.setTrip(trip);
        recommendationRepository.save(recommendation);
    }

    @Transactional
    public void addRecommendationToItinerary(Long recommendationId, String ownerEmail) {
        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation not found"));

        Trip trip = recommendation.getTrip();
        if (trip.getUser() == null || !ownerEmail.equals(trip.getUser().getEmail())) {
            throw new IllegalArgumentException("Recommendation not found for this user");
        }
        LocalDate baseDate = trip.getStartDate() != null ? trip.getStartDate() : LocalDate.now();
        ActivityService.ActivitySlot suggestedSlot = activityService.getNextSuggestedRecommendationSlot(
                trip.getId(),
                ownerEmail,
                baseDate
        );
        boolean added = activityService.addActivityToTrip(
                trip.getId(),
                ownerEmail,
                recommendation.getName(),
                recommendation.getDescription(),
                suggestedSlot.date(),
                suggestedSlot.time(),
                recommendation.getLocation()
        );
        if (!added) {
            return;
        }
        if (recommendation.getEstimatedCost() != null && recommendation.getEstimatedCost() > 0) {
            budgetService.addAutomaticExpenseToTrip(
                    trip.getId(),
                    ownerEmail,
                    "Attractions",
                    recommendation.getEstimatedCost(),
                    suggestedSlot.date() != null ? suggestedSlot.date() : LocalDate.now(),
                    "Attraction booking: " + recommendation.getName()
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendationsForTrip(Trip trip) {
        return recommendationRepository.findByTripId(trip.getId());
    }

    @Transactional(readOnly = true)
    public List<Recommendation> getRecommendationsForTripId(Long tripId) {
        return recommendationRepository.findByTripId(tripId);
    }

    @Transactional
    public void ensureRecommendationsForTrip(Trip trip) {
        if (!recommendationRepository.findByTripId(trip.getId()).isEmpty()) {
            return;
        }

        for (Recommendation recommendation : buildRecommendationsForDestination(trip.getDestination())) {
            addRecommendation(trip, recommendation);
        }
    }

    @Transactional
    public void cleanupDuplicateRecommendationsForTrip(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        List<Recommendation> recommendations = recommendationRepository.findByTripId(tripId);
        if (recommendations.isEmpty()) {
            return;
        }

        Set<String> seenKeys = new HashSet<>();
        for (Recommendation recommendation : recommendations) {
            String key = recommendationKey(recommendation);
            if (!seenKeys.add(key)) {
                recommendationRepository.delete(recommendation);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Recommendation> getPersonalizedRecommendationsForTrip(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        List<Recommendation> recommendations = recommendationRepository.findByTripId(tripId);
        User user = trip.getUser();

        return recommendations.stream()
                .filter(recommendation -> !isAlreadyPlanned(trip, recommendation))
                .sorted(Comparator.comparingDouble((Recommendation recommendation) -> recommendationScore(recommendation, trip, user)).reversed())
                .toList();
    }

    private List<Recommendation> buildRecommendationsForDestination(String destination) {
        String normalizedDestination = destination == null ? "" : destination.toLowerCase(Locale.ROOT);
        List<Recommendation> recommendations = new ArrayList<>();

        if (normalizedDestination.contains("tokyo")) {
            recommendations.add(new Recommendation("Attraction", "Senso-ji Temple", "Asakusa", "Historic temple and market street.", 4.8, "culture", 20.0));
            recommendations.add(new Recommendation("Food", "Tsukiji Outer Market", "Tokyo", "Seafood and local food experience.", 4.6, "food", 35.0));
            recommendations.add(new Recommendation("Experience", "Shibuya Crossing", "Shibuya", "Famous city crossing and nightlife area.", 4.5, "city", 0.0));
            recommendations.add(new Recommendation("Experience", "teamLab Planets", "Toyosu", "Immersive digital art museum.", 4.7, "art", 45.0));
            recommendations.add(new Recommendation("Attraction", "Tokyo Skytree", "Sumida", "Observation tower with panoramic views of the city.", 4.7, "city", 30.0));
            recommendations.add(new Recommendation("Park", "Ueno Park", "Ueno", "Relaxed park area with museums and seasonal flowers.", 4.5, "nature", 0.0));
            recommendations.add(new Recommendation("Shopping", "Harajuku Takeshita Street", "Harajuku", "Youth fashion street with snacks and shopping.", 4.4, "shopping", 15.0));
            recommendations.add(new Recommendation("Food", "Omoide Yokocho", "Shinjuku", "Narrow alley packed with yakitori stalls and local dining.", 4.5, "food", 25.0));
            recommendations.add(new Recommendation("Experience", "Meiji Shrine", "Shibuya", "Peaceful shrine walk surrounded by forest in central Tokyo.", 4.7, "culture", 0.0));
            recommendations.add(new Recommendation("Attraction", "Tokyo DisneySea", "Urayasu", "Full-day themed park popular with couples and families.", 4.8, "family", 85.0));
            recommendations.add(new Recommendation("Market", "Ameya-Yokocho Market", "Ueno", "Bustling market street for snacks, fashion, and souvenirs.", 4.4, "shopping", 18.0));
            recommendations.add(new Recommendation("Experience", "Tokyo Metropolitan Government Building", "Shinjuku", "Free observation decks with sweeping skyline views.", 4.5, "city", 0.0));
            recommendations.add(new Recommendation("Attraction", "Imperial Palace East Gardens", "Chiyoda", "Historic grounds with wide paths and seasonal greenery.", 4.4, "culture", 0.0));
            recommendations.add(new Recommendation("Food", "Asakusa Street Food Crawl", "Asakusa", "Sweet treats, tempura bites, and traditional snacks near the temple.", 4.5, "food", 22.0));
            recommendations.add(new Recommendation("Experience", "Akihabara Arcade Run", "Akihabara", "Retro arcades, anime stores, and electric-town energy.", 4.4, "city", 20.0));
            recommendations.add(new Recommendation("Park", "Shinjuku Gyoen", "Shinjuku", "Large landscaped park ideal for a slower half-day reset.", 4.7, "nature", 8.0));
            recommendations.add(new Recommendation("Experience", "Tokyo Bay Sunset Cruise", "Tokyo Bay", "Evening harbour cruise with skyline lights and breezes.", 4.6, "city", 48.0));
            recommendations.add(new Recommendation("Attraction", "Mori Art Museum", "Roppongi", "Contemporary art museum paired with city-view access.", 4.5, "art", 25.0));
            recommendations.add(new Recommendation("Shopping", "Ginza Window Shopping", "Ginza", "Luxury storefronts, dessert cafes, and polished city streets.", 4.3, "shopping", 12.0));
            recommendations.add(new Recommendation("Experience", "Yanaka Old Town Walk", "Yanaka", "Traditional neighbourhood lanes with a quieter local feel.", 4.6, "culture", 10.0));
        } else if (normalizedDestination.contains("osaka")) {
            recommendations.add(new Recommendation("Food", "Dotonbori Street Food", "Osaka", "Popular district for takoyaki, okonomiyaki, and nightlife.", 4.8, "food", 25.0));
            recommendations.add(new Recommendation("Attraction", "Osaka Castle", "Osaka", "Historic castle grounds and museum experience.", 4.7, "culture", 18.0));
            recommendations.add(new Recommendation("Experience", "Umeda Sky Building", "Osaka", "Observation deck with skyline views.", 4.5, "city", 20.0));
            recommendations.add(new Recommendation("Experience", "Shinsekai Walk", "Osaka", "Retro entertainment district with street food and local color.", 4.4, "city", 15.0));
            recommendations.add(new Recommendation("Attraction", "Universal Studios Japan", "Osaka Bay", "Theme park with movie attractions and rides.", 4.8, "family", 90.0));
            recommendations.add(new Recommendation("Market", "Kuromon Ichiba Market", "Namba", "Food market known for fresh seafood and grilled snacks.", 4.6, "food", 20.0));
            recommendations.add(new Recommendation("Experience", "Osaka Aquarium Kaiyukan", "Tempozan", "Large aquarium and harbour-side complex.", 4.7, "family", 32.0));
            recommendations.add(new Recommendation("Shopping", "Shinsaibashi Shopping Arcade", "Shinsaibashi", "Long covered street with fashion, souvenirs, and cafes.", 4.4, "shopping", 20.0));
            recommendations.add(new Recommendation("Attraction", "Sumiyoshi Taisha", "Osaka", "Historic shrine with iconic bridge and peaceful grounds.", 4.5, "culture", 5.0));
            recommendations.add(new Recommendation("Experience", "Nakanoshima Riverside Walk", "Nakanoshima", "Easy evening walk with city lights and river views.", 4.3, "nature", 0.0));
            recommendations.add(new Recommendation("Experience", "Hozenji Yokocho Night Walk", "Namba", "Lantern-lit alleyways and compact local dining stops.", 4.4, "food", 18.0));
            recommendations.add(new Recommendation("Attraction", "Abeno Harukas Viewpoint", "Tennoji", "Tall tower observatory with panoramic city views.", 4.5, "city", 22.0));
            recommendations.add(new Recommendation("Park", "Osaka Castle Park Cycle", "Osaka", "Relaxed bike route around castle greenery and moats.", 4.3, "nature", 12.0));
            recommendations.add(new Recommendation("Food", "Shin-Osaka Ramen Stop", "Shin-Osaka", "Casual ramen-focused meal break near the station district.", 4.2, "food", 16.0));
            recommendations.add(new Recommendation("Experience", "Tenjinbashi-suji Shopping Stroll", "Kita", "Long local arcade packed with neighborhood shops and snacks.", 4.4, "shopping", 14.0));
        } else if (normalizedDestination.contains("hong kong") || normalizedDestination.contains("hongkong")) {
            recommendations.add(new Recommendation("Experience", "Star Ferry", "Victoria Harbour", "Classic harbour crossing with skyline views.", 4.8, "city", 5.0));
            recommendations.add(new Recommendation("Attraction", "Ocean Park", "Hong Kong Island", "Marine life, rides, and family-friendly attractions.", 4.6, "family", 60.0));
            recommendations.add(new Recommendation("Attraction", "Victoria Peak", "Hong Kong Island", "Famous city viewpoint and tram experience.", 4.7, "city", 20.0));
            recommendations.add(new Recommendation("Food", "Temple Street Night Market", "Kowloon", "Street food and night market shopping experience.", 4.5, "food", 20.0));
            recommendations.add(new Recommendation("Attraction", "Hong Kong Disneyland", "Lantau Island", "Theme park with rides, shows, and character experiences.", 4.7, "family", 80.0));
            recommendations.add(new Recommendation("Experience", "Ngong Ping 360 Cable Car", "Lantau Island", "Scenic cable car ride to the Big Buddha area.", 4.6, "nature", 35.0));
            recommendations.add(new Recommendation("Attraction", "Big Buddha", "Lantau Island", "Large outdoor Buddha statue with monastery grounds nearby.", 4.7, "culture", 10.0));
            recommendations.add(new Recommendation("Shopping", "Ladies Market", "Mong Kok", "Busy open-air market for fashion, gifts, and street snacks.", 4.3, "shopping", 15.0));
            recommendations.add(new Recommendation("Food", "Tsim Sha Tsui Harbourfront", "Tsim Sha Tsui", "Waterfront walk with food stops and skyline views.", 4.5, "food", 18.0));
            recommendations.add(new Recommendation("Experience", "Avenue of Stars", "Tsim Sha Tsui", "Harbour promenade ideal for an evening city walk.", 4.4, "city", 0.0));
            recommendations.add(new Recommendation("Experience", "Central Escalator Walk", "Central", "Urban hillside walk linking cafes, street art, and city scenes.", 4.3, "city", 8.0));
            recommendations.add(new Recommendation("Food", "Dim Sum Brunch in Sheung Wan", "Sheung Wan", "Classic dim sum stop with easy access to older neighborhoods.", 4.6, "food", 24.0));
            recommendations.add(new Recommendation("Attraction", "PMQ Creative Hub", "Central", "Design studios, galleries, and local maker shops.", 4.2, "art", 12.0));
            recommendations.add(new Recommendation("Nature", "Dragon's Back Hike", "Shek O", "Moderate coastal hike with sea views and breezy ridgelines.", 4.7, "nature", 0.0));
            recommendations.add(new Recommendation("Shopping", "Harbour City Browsing", "Tsim Sha Tsui", "Major mall complex with shopping and harbour access.", 4.2, "shopping", 20.0));
        } else if (normalizedDestination.contains("japan")) {
            recommendations.add(new Recommendation("Attraction", "Japan Highlights", destination, "Popular sightseeing and culture picks for a Japan trip.", 4.5, "culture", 28.0));
            recommendations.add(new Recommendation("Food", "Japan Food Picks", destination, "A mix of local dining and food street recommendations.", 4.4, "food", 30.0));
            recommendations.add(new Recommendation("Experience", "Japan City Walk", destination, "A general city exploration route for visitors.", 4.3, "city", 15.0));
        } else if (normalizedDestination.contains("paris")) {
            recommendations.add(new Recommendation("Attraction", "Eiffel Tower", "Paris", "Landmark city attraction and observation experience.", 4.7, "city", 40.0));
            recommendations.add(new Recommendation("Food", "Le Marais Food Walk", "Paris", "Classic pastries, cafes, and local street food.", 4.5, "food", 30.0));
            recommendations.add(new Recommendation("Experience", "Louvre Highlights Tour", "Paris", "Art and history tour for first-time visitors.", 4.8, "art", 55.0));
        } else if (normalizedDestination.contains("sydney")) {
            recommendations.add(new Recommendation("Attraction", "Sydney Opera House", "Sydney", "Iconic harbour landmark and guided tour.", 4.7, "culture", 42.0));
            recommendations.add(new Recommendation("Experience", "Bondi to Coogee Walk", "Sydney", "Scenic coastal walk with beach stops.", 4.8, "nature", 0.0));
            recommendations.add(new Recommendation("Food", "The Rocks Dining Spot", "Sydney", "Historic area with local restaurants.", 4.4, "food", 38.0));
        } else {
            recommendations.add(new Recommendation("Attraction", destination + " City Highlights", destination, "Popular landmarks and must-see places for first-time visitors.", 4.4, "city", 25.0));
            recommendations.add(new Recommendation("Food", destination + " Local Food Picks", destination, "Recommended restaurants and tasting spots for the area.", 4.3, "food", 30.0));
            recommendations.add(new Recommendation("Experience", destination + " Cultural Experience", destination, "An experience suited for visitors exploring the destination.", 4.2, "culture", 35.0));
        }

        appendExtendedRecommendations(destination, recommendations);
        return recommendations;
    }

    private void appendExtendedRecommendations(String destination, List<Recommendation> recommendations) {
        recommendations.add(new Recommendation("Experience", destination + " Sunrise Walk", destination, "A calm early outing for photos, coffee, and local streets.", 4.2, "nature", 0.0));
        recommendations.add(new Recommendation("Food", destination + " Signature Lunch Spot", destination, "A dependable midday food stop featuring local specialties.", 4.3, "food", 24.0));
        recommendations.add(new Recommendation("Attraction", destination + " History Museum", destination, "An easy museum visit to add context and culture to the trip.", 4.1, "culture", 18.0));
        recommendations.add(new Recommendation("Experience", destination + " Riverside Evening Walk", destination, "A slower-paced walk with scenic views and photo points.", 4.2, "city", 0.0));
        recommendations.add(new Recommendation("Shopping", destination + " Local Market Browse", destination, "A flexible shopping stop for snacks, crafts, and gifts.", 4.1, "shopping", 15.0));
        recommendations.add(new Recommendation("Nature", destination + " Garden Break", destination, "A quiet green-space stop for a lighter afternoon.", 4.1, "nature", 10.0));
    }

    private double recommendationScore(Recommendation recommendation, Trip trip, User user) {
        double score = recommendation.getRating() != null ? recommendation.getRating() : 0.0;
        String preferenceText = buildPreferenceText(user);
        String tag = safeLower(recommendation.getPreferenceTag());
        String type = safeLower(recommendation.getType());
        String location = safeLower(recommendation.getLocation());
        String destination = safeLower(trip.getDestination());

        if (!tag.isBlank() && preferenceText.contains(tag)) {
            score += 3.0;
        }
        if (!type.isBlank() && preferenceText.contains(type)) {
            score += 2.0;
        }
        if (!location.isBlank() && destination.contains(location)) {
            score += 1.0;
        }

        Double budgetRange = user != null ? user.getBudgetRange() : null;
        Double estimatedCost = recommendation.getEstimatedCost();
        if (budgetRange != null && estimatedCost != null) {
            if (budgetRange >= estimatedCost) {
                score += 1.5;
            } else {
                score -= 1.0;
            }
        }

        return score;
    }

    private boolean isAlreadyPlanned(Trip trip, Recommendation recommendation) {
        if (trip.getItinerary() == null || trip.getItinerary().getActivities() == null) {
            return false;
        }

        return trip.getItinerary().getActivities().stream()
                .anyMatch(activity ->
                        safeLower(activity.getActivityName()).equals(safeLower(recommendation.getName()))
                                && safeLower(activity.getLocation()).equals(safeLower(recommendation.getLocation())));
    }

    private String buildPreferenceText(User user) {
        if (user == null) {
            return "";
        }

        return (safeLower(user.getTravelPreferences()) + " "
                + safeLower(user.getInterests()) + " "
                + safeLower(user.getPersonalInfo()));
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String recommendationKey(Recommendation recommendation) {
        return safeLower(recommendation.getName()) + "|"
                + safeLower(recommendation.getLocation()) + "|"
                + safeLower(recommendation.getType());
    }
}
