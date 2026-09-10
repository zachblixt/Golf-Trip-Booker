package org.golftripbooker.dtos;

import org.golftripbooker.models.Booking;
import org.golftripbooker.models.TripRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * A past trip as it appears on Explore: what a group of this size actually got for
 * this much money. Deliberately anonymous -- no client, no notes, no request id --
 * because browsing other people's trips should not mean browsing other people.
 */
public class BookedTripResponse {

    private final DestinationResponse destination;
    private final int playerCount;
    private final int roundsPlayed;
    private final int nights;
    private final BigDecimal totalCost;
    private final BigDecimal costPerPlayer;
    private final List<String> courses;
    private final String lodging;
    private final LocalDate startDate;

    public static BookedTripResponse from(TripRequest request, Booking booking) {
        return new BookedTripResponse(request, booking);
    }

    /** Skips any request whose booking is missing rather than emitting a half-filled card. */
    public static List<BookedTripResponse> fromAll(List<TripRequest> requests,
                                                   Map<Integer, Booking> bookings) {
        return requests.stream()
                .filter(r -> bookings.get(r.getRequestId()) != null)
                .map(r -> from(r, bookings.get(r.getRequestId())))
                .toList();
    }

    private BookedTripResponse(TripRequest request, Booking booking) {
        this.destination = DestinationResponse.from(request.getDestination());
        this.playerCount = request.getPlayerCount();
        this.roundsPlayed = booking.courseList().size();
        this.nights = request.getNights();
        this.totalCost = booking.getTotalCost();
        this.costPerPlayer = request.getPlayerCount() > 0
                ? booking.getTotalCost().divide(
                        BigDecimal.valueOf(request.getPlayerCount()), 2, RoundingMode.HALF_UP)
                : booking.getTotalCost();
        this.courses = booking.courseList();
        this.lodging = booking.getLodging();
        this.startDate = booking.getStartDate();
    }

    public DestinationResponse getDestination() {
        return destination;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getRoundsPlayed() {
        return roundsPlayed;
    }

    public int getNights() {
        return nights;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public BigDecimal getCostPerPlayer() {
        return costPerPlayer;
    }

    public List<String> getCourses() {
        return courses;
    }

    public String getLodging() {
        return lodging;
    }

    public LocalDate getStartDate() {
        return startDate;
    }
}
