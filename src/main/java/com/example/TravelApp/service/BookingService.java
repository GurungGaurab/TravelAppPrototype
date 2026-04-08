package com.example.TravelApp.service;

import com.example.TravelApp.model.Bookings;
import com.example.TravelApp.model.FlightBooking;
import com.example.TravelApp.model.HotelBooking;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.repository.BookingsRepository;
import com.example.TravelApp.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class BookingService {

    private final BookingsRepository bookingsRepository;
    private final TripRepository tripRepository;
    private final BudgetService budgetService;

    public BookingService(BookingsRepository bookingsRepository,
                          TripRepository tripRepository,
                          BudgetService budgetService) {
        this.bookingsRepository = bookingsRepository;
        this.tripRepository = tripRepository;
        this.budgetService = budgetService;
    }

    public List<FlightOption> getFlightOptionsForTrip(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        LocalDate startDate = trip.getStartDate() != null ? trip.getStartDate() : LocalDate.now();
        LocalDate endDate = trip.getEndDate() != null && !trip.getEndDate().isBefore(startDate)
                ? trip.getEndDate()
                : startDate.plusDays(3);
        String displayDestination = trip.getDestination() != null ? trip.getDestination() : "Destination";
        String origin = inferOrigin(displayDestination);

        List<FlightOption> options = new ArrayList<>();
        options.add(new FlightOption(
                "FLT-1-" + tripId,
                "TravelAir",
                "TA" + tripId + "1",
                origin,
                displayDestination,
                startDate.atTime(8, 15),
                startDate.atTime(12, 30),
                endDate.atTime(14, 10),
                endDate.atTime(18, 25),
                280.0
        ));
        options.add(new FlightOption(
                "FLT-2-" + tripId,
                "SkyBridge",
                "SB" + tripId + "2",
                origin,
                displayDestination,
                startDate.atTime(10, 40),
                startDate.atTime(14, 55),
                endDate.atTime(16, 20),
                endDate.atTime(20, 40),
                325.0
        ));
        options.add(new FlightOption(
                "FLT-3-" + tripId,
                "Pacific Jet",
                "PJ" + tripId + "3",
                origin,
                displayDestination,
                startDate.atTime(18, 5),
                startDate.atTime(22, 20),
                endDate.atTime(11, 30),
                endDate.atTime(15, 45),
                305.0
        ));
        return options;
    }

    public FlightBooking getSelectedFlightBooking(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        return trip.getBookings().stream()
                .filter(FlightBooking.class::isInstance)
                .map(FlightBooking.class::cast)
                .findFirst()
                .orElse(null);
    }

    public HotelBooking getSelectedHotelBooking(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        return trip.getBookings().stream()
                .filter(HotelBooking.class::isInstance)
                .map(HotelBooking.class::cast)
                .findFirst()
                .orElse(null);
    }

    public List<HotelOption> getHotelOptionsForTrip(Long tripId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        LocalDate checkInDate = trip.getStartDate() != null ? trip.getStartDate() : LocalDate.now();
        LocalDate checkOutDate = trip.getEndDate() != null ? trip.getEndDate() : checkInDate.plusDays(3);
        String displayDestination = trip.getDestination() != null ? trip.getDestination() : "Destination";

        List<HotelOption> options = new ArrayList<>();
        options.add(new HotelOption("HOT-1-" + tripId, displayDestination + " Central Hotel", displayDestination, checkInDate, checkOutDate, 135.0, "Standard Room"));
        options.add(new HotelOption("HOT-2-" + tripId, displayDestination + " Comfort Stay", displayDestination, checkInDate, checkOutDate, 115.0, "City Room"));
        options.add(new HotelOption("HOT-3-" + tripId, displayDestination + " Grand Suites", displayDestination, checkInDate, checkOutDate, 175.0, "Deluxe Suite"));
        return options;
    }

    @Transactional
    public void createBookingFromOption(Long tripId, String ownerEmail, String bookingType, String optionId) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        if ("HOTEL".equalsIgnoreCase(bookingType)) {
            HotelBooking selectedHotel = getSelectedHotelBooking(tripId, ownerEmail);
            if (selectedHotel != null) {
                return;
            }

            HotelOption option = getHotelOptionsForTrip(tripId, ownerEmail).stream()
                    .filter(candidate -> candidate.optionId().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Hotel option not found"));

            boolean exists = trip.getBookings().stream()
                    .filter(HotelBooking.class::isInstance)
                    .map(HotelBooking.class::cast)
                    .anyMatch(booking ->
                            booking.getHotelName().equalsIgnoreCase(option.hotelName())
                                    && booking.getLocation().equalsIgnoreCase(option.location()));
            if (exists) {
                return;
            }

            Bookings booking = new HotelBooking(
                    option.hotelName(),
                    option.location(),
                    option.checkInDate(),
                    option.checkOutDate(),
                    option.pricePerNight()
            );
            saveBooking(trip, booking, "HOTEL");
            budgetService.addAutomaticExpenseToTrip(
                    tripId,
                    ownerEmail,
                    "Accommodation",
                    option.pricePerNight() * Math.max(1, option.checkOutDate().toEpochDay() - option.checkInDate().toEpochDay()),
                    option.checkInDate(),
                    hotelExpenseDescription(option)
            );
        } else {
            FlightBooking selectedFlight = getSelectedFlightBooking(tripId, ownerEmail);
            if (selectedFlight != null) {
                return;
            }

            FlightOption option = getFlightOptionsForTrip(tripId, ownerEmail).stream()
                    .filter(candidate -> candidate.optionId().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Flight option not found"));

            boolean exists = trip.getBookings().stream()
                    .filter(FlightBooking.class::isInstance)
                    .map(FlightBooking.class::cast)
                    .anyMatch(booking ->
                            booking.getFlightNumber().equalsIgnoreCase(option.flightNumber())
                                    && booking.getAirline().equalsIgnoreCase(option.airline()));
            if (exists) {
                return;
            }

            Bookings booking = new FlightBooking(
                    option.origin(),
                    option.destination(),
                    option.departureDate(),
                    option.arrivalDate(),
                    option.returnDepartureDate(),
                    option.returnArrivalDate(),
                    option.price(),
                    option.airline(),
                    option.flightNumber()
            );
            saveBooking(trip, booking, "FLIGHT");
            budgetService.addAutomaticExpenseToTrip(
                    tripId,
                    ownerEmail,
                    "Flight",
                    option.price(),
                    option.departureDate().toLocalDate(),
                    flightExpenseDescription(option)
            );
        }
    }

    @Transactional
    public void cancelBooking(Long tripId, Long bookingId, String ownerEmail) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Bookings booking = trip.getBookings().stream()
                .filter(candidate -> Objects.equals(candidate.getId(), bookingId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Booking not found for this trip"));

        if (booking instanceof FlightBooking) {
            List<Bookings> flightBookings = trip.getBookings().stream()
                    .filter(FlightBooking.class::isInstance)
                    .toList();
            for (Bookings flightBooking : flightBookings) {
                FlightBooking savedFlight = (FlightBooking) flightBooking;
                budgetService.removeAutomaticExpenseFromTrip(
                        tripId,
                        ownerEmail,
                        "Flight",
                        flightExpenseDescription(savedFlight)
                );
                flightBooking.setTrip(null);
                trip.getBookings().remove(flightBooking);
                bookingsRepository.delete(flightBooking);
            }
        } else {
            if (booking instanceof HotelBooking) {
                List<Bookings> hotelBookings = trip.getBookings().stream()
                        .filter(HotelBooking.class::isInstance)
                        .toList();
                for (Bookings hotelBooking : hotelBookings) {
                    HotelBooking savedHotel = (HotelBooking) hotelBooking;
                    budgetService.removeAutomaticExpenseFromTrip(
                            tripId,
                            ownerEmail,
                            "Accommodation",
                            hotelExpenseDescription(savedHotel)
                    );
                    hotelBooking.setTrip(null);
                    trip.getBookings().remove(hotelBooking);
                    bookingsRepository.delete(hotelBooking);
                }
            } else {
                booking.setTrip(null);
                trip.getBookings().remove(booking);
                bookingsRepository.delete(booking);
            }
        }

        tripRepository.save(trip);
    }

    public void createMockBooking(Long tripId,
                                  String ownerEmail,
                                  String bookingType,
                                  String primaryName,
                                  String secondaryValue,
                                  Double price,
                                  LocalDate date) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Bookings booking;
        if ("HOTEL".equalsIgnoreCase(bookingType)) {
            booking = new HotelBooking(
                    primaryName,
                    secondaryValue,
                    date,
                    date.plusDays(2),
                    price
            );
        } else {
            booking = new FlightBooking(
                    "Sydney",
                    trip.getDestination(),
                    LocalDateTime.of(date, java.time.LocalTime.of(9, 0)),
                    LocalDateTime.of(date, java.time.LocalTime.of(17, 30)),
                    LocalDateTime.of(date.plusDays(3), LocalTime.of(12, 0)),
                    LocalDateTime.of(date.plusDays(3), LocalTime.of(20, 0)),
                    price,
                    secondaryValue,
                    "TA" + tripId
            );
        }

        saveBooking(trip, booking, bookingType);
    }

    private void saveBooking(Trip trip, Bookings booking, String bookingType) {
        booking.setTrip(trip);
        trip.getBookings().add(booking);
        tripRepository.save(trip);
    }

    public record FlightOption(
            String optionId,
            String airline,
            String flightNumber,
            String origin,
            String destination,
            LocalDateTime departureDate,
            LocalDateTime arrivalDate,
            LocalDateTime returnDepartureDate,
            LocalDateTime returnArrivalDate,
            Double price
    ) {
    }

    public record HotelOption(
            String optionId,
            String hotelName,
            String location,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Double pricePerNight,
            String roomType
    ) {
    }

    private String inferOrigin(String destination) {
        String normalized = destination.toLowerCase();
        if (normalized.contains("hong kong")) {
            return "Tokyo";
        }
        return "Hong Kong";
    }

    private String flightExpenseDescription(FlightOption option) {
        return "Flight booking: " + option.airline() + " " + option.flightNumber();
    }

    private String flightExpenseDescription(FlightBooking booking) {
        return "Flight booking: " + booking.getAirline() + " " + booking.getFlightNumber();
    }

    private String hotelExpenseDescription(HotelOption option) {
        return "Stay booking: " + option.hotelName();
    }

    private String hotelExpenseDescription(HotelBooking booking) {
        return "Stay booking: " + booking.getHotelName();
    }
}
