package org.golftripbooker.dtos;

import org.golftripbooker.models.Destination;
import org.golftripbooker.models.TripRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The New Request screen.
 *
 * Note what a client is not allowed to send: no userId, no status, no createdAt,
 * no declineReason. Those come from the JWT and the workflow, so there is no field
 * here to tamper with.
 */
public class TripRequestForm {

    private int destinationId;

    /**
     * Set instead of destinationId when the client wants somewhere the app does not
     * know about yet. Coordinates arrive when the phone managed to geocode the name;
     * without them the place still works, it just cannot be mapped until a host books
     * it and somebody fills them in.
     */
    private String requestedPlace;
    private BigDecimal requestedLatitude;
    private BigDecimal requestedLongitude;
    private BigDecimal budgetPerPlayer;
    private int playerCount;
    private int roundsRequested;
    private int nights;
    private LocalDate earliestStart;
    private LocalDate latestStart;
    private String notes;

    /**
     * Builds a model with only the destination's id filled in. TripRequestService
     * looks up the real row, which is what makes an unknown destination a validation
     * error instead of a foreign key crash.
     */
    public TripRequest toModel(int requestId) {
        TripRequest request = new TripRequest();
        request.setRequestId(requestId);
        // Only the id is set. TripRequestService resolves the real row, so the other
        // fields -- coordinates included -- are never taken from the request body.
        request.setDestination(
                destinationId > 0 ? new Destination(destinationId, null, null, null, null, null) : null);
        request.setRequestedPlace(requestedPlace);
        request.setRequestedLatitude(requestedLatitude);
        request.setRequestedLongitude(requestedLongitude);
        request.setBudgetPerPlayer(budgetPerPlayer);
        request.setPlayerCount(playerCount);
        request.setRoundsRequested(roundsRequested);
        request.setNights(nights);
        request.setEarliestStart(earliestStart);
        request.setLatestStart(latestStart);
        request.setNotes(notes);
        return request;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(int destinationId) {
        this.destinationId = destinationId;
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
}
