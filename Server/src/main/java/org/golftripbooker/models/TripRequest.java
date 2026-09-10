package org.golftripbooker.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * What a client asked for. The client and the status are never taken from the
 * request body -- the client comes from the JWT, the status from the workflow.
 */
public class TripRequest {

    private int requestId;
    private User client;

    /**
     * Null when the client asked for somewhere not seeded yet. Exactly one of this
     * and requestedPlace is set, and a booking is what turns the second into the
     * first -- see BookingService.book.
     */
    private Destination destination;

    private String requestedPlace;
    private BigDecimal requestedLatitude;
    private BigDecimal requestedLongitude;
    private BigDecimal budgetPerPlayer;
    private int playerCount;
    private int roundsRequested;
    private int nights;

    /**
     * The window the group can travel in. Both or neither, and latest is never before
     * earliest -- TripRequestService enforces both. A fixed date is a window of one day.
     */
    private LocalDate earliestStart;
    private LocalDate latestStart;

    private String notes;
    private RequestStatus status;
    private String declineReason;
    private LocalDateTime createdAt;

    public TripRequest() {
    }

    public TripRequest(int requestId, User client, Destination destination, BigDecimal budgetPerPlayer,
                       int playerCount, int roundsRequested, int nights, LocalDate earliestStart,
                       LocalDate latestStart, String notes, RequestStatus status, String declineReason,
                       LocalDateTime createdAt) {
        this(requestId, client, destination, null, null, null, budgetPerPlayer, playerCount,
                roundsRequested, nights, earliestStart, latestStart, notes, status, declineReason,
                createdAt);
    }

    public TripRequest(int requestId, User client, Destination destination, String requestedPlace,
                       BigDecimal requestedLatitude, BigDecimal requestedLongitude,
                       BigDecimal budgetPerPlayer, int playerCount, int roundsRequested, int nights,
                       LocalDate earliestStart, LocalDate latestStart, String notes,
                       RequestStatus status, String declineReason, LocalDateTime createdAt) {
        this.requestedPlace = requestedPlace;
        this.requestedLatitude = requestedLatitude;
        this.requestedLongitude = requestedLongitude;
        this.requestId = requestId;
        this.client = client;
        this.destination = destination;
        this.budgetPerPlayer = budgetPerPlayer;
        this.playerCount = playerCount;
        this.roundsRequested = roundsRequested;
        this.nights = nights;
        this.earliestStart = earliestStart;
        this.latestStart = latestStart;
        this.notes = notes;
        this.status = status;
        this.declineReason = declineReason;
        this.createdAt = createdAt;
    }

    /** What to call this trip: the seeded destination, or the place the client named. */
    public String placeLabel() {
        return destination != null ? destination.getName() : requestedPlace;
    }

    /** True while this request names a place that is not a destination yet. */
    public boolean isUnseededPlace() {
        return destination == null && requestedPlace != null && !requestedPlace.isBlank();
    }

    public String getRequestedPlace() {
        return requestedPlace;
    }

    public void setRequestedPlace(String requestedPlace) {
        this.requestedPlace = requestedPlace;
    }

    public BigDecimal getRequestedLatitude() {
        return requestedLatitude;
    }

    public void setRequestedLatitude(BigDecimal requestedLatitude) {
        this.requestedLatitude = requestedLatitude;
    }

    public BigDecimal getRequestedLongitude() {
        return requestedLongitude;
    }

    public void setRequestedLongitude(BigDecimal requestedLongitude) {
        this.requestedLongitude = requestedLongitude;
    }

    /**
     * The most the trip may cost in total. The host's booking form shows this as
     * the ceiling, and BookingService refuses a total above it.
     */
    public BigDecimal budgetCeiling() {
        if (budgetPerPlayer == null) {
            return BigDecimal.ZERO;
        }
        return budgetPerPlayer.multiply(BigDecimal.valueOf(playerCount));
    }

    /** True only when both halves are present -- half a window is no window. */
    public boolean hasWindow() {
        return earliestStart != null && latestStart != null;
    }

    /**
     * Whether a start date the host picked is one this group said they could travel on.
     * A request with no window allows anything, exactly as a null preferred date did.
     */
    public boolean windowAllows(LocalDate start) {
        if (!hasWindow() || start == null) {
            return true;
        }
        return !start.isBefore(earliestStart) && !start.isAfter(latestStart);
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public User getClient() {
        return client;
    }

    public void setClient(User client) {
        this.client = client;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public BigDecimal getBudgetPerPlayer() {
        return budgetPerPlayer;
    }

    public void setBudgetPerPlayer(BigDecimal budgetPerPlayer) {
        this.budgetPerPlayer = budgetPerPlayer;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public int getRoundsRequested() {
        return roundsRequested;
    }

    public void setRoundsRequested(int roundsRequested) {
        this.roundsRequested = roundsRequested;
    }

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
    }

    public LocalDate getEarliestStart() {
        return earliestStart;
    }

    public void setEarliestStart(LocalDate earliestStart) {
        this.earliestStart = earliestStart;
    }

    public LocalDate getLatestStart() {
        return latestStart;
    }

    public void setLatestStart(LocalDate latestStart) {
        this.latestStart = latestStart;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TripRequest that = (TripRequest) o;
        return requestId == that.requestId
                && playerCount == that.playerCount
                && roundsRequested == that.roundsRequested
                && nights == that.nights
                && Objects.equals(client, that.client)
                && Objects.equals(destination, that.destination)
                && Objects.equals(requestedPlace, that.requestedPlace)
                && Objects.equals(requestedLatitude, that.requestedLatitude)
                && Objects.equals(requestedLongitude, that.requestedLongitude)
                && Objects.equals(budgetPerPlayer, that.budgetPerPlayer)
                && Objects.equals(earliestStart, that.earliestStart)
                && Objects.equals(latestStart, that.latestStart)
                && Objects.equals(notes, that.notes)
                && status == that.status
                && Objects.equals(declineReason, that.declineReason)
                && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, client, destination, requestedPlace, requestedLatitude,
                requestedLongitude, budgetPerPlayer, playerCount,
                roundsRequested, nights, earliestStart, latestStart, notes, status, declineReason,
                createdAt);
    }
}
