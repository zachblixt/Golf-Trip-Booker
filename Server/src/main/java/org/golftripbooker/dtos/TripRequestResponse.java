package org.golftripbooker.dtos;

import org.golftripbooker.models.Booking;
import org.golftripbooker.models.RequestStatus;
import org.golftripbooker.models.TripRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * One request, shaped for whoever is looking at it.
 *
 * `client` is populated only in the host's queue -- a client looking at their own
 * list already knows whose it is, and leaving the field out is one less place for
 * another user's email to leak.
 */
public class TripRequestResponse {

    private final int requestId;
    private final DestinationResponse destination;

    /**
     * What to call this trip. The destination's name once there is one, otherwise the
     * place the client typed -- so every screen renders one field instead of deciding
     * which of two to show.
     */
    private final String placeLabel;
    private final String requestedPlace;
    private final BigDecimal budgetPerPlayer;
    private final int playerCount;
    private final int roundsRequested;
    private final int nights;
    private final LocalDate earliestStart;
    private final LocalDate latestStart;
    private final String notes;
    private final RequestStatus status;
    private final String declineReason;
    private final LocalDateTime createdAt;

    /** budgetPerPlayer x playerCount. The ceiling the host's booking form displays. */
    private final BigDecimal budgetCeiling;

    private final UserResponse client;
    private final BookingResponse booking;

    /**
     * How many proposals this request has collected, live and dead. Zero on the list
     * screens, which do not pay for the count -- only the detail screens show it.
     */
    private final int proposalCount;

    /** The client's own view: no client block. */
    public static TripRequestResponse forClient(TripRequest request, Booking booking) {
        return new TripRequestResponse(request, booking, false, 0);
    }

    /** The detail view, which also carries the round number. */
    public static TripRequestResponse forClient(TripRequest request, Booking booking,
                                                int proposalCount) {
        return new TripRequestResponse(request, booking, false, proposalCount);
    }

    /** The host's view: who asked, so the queue can say "from zach". */
    public static TripRequestResponse forHost(TripRequest request, Booking booking) {
        return new TripRequestResponse(request, booking, true, 0);
    }

    public static TripRequestResponse forHost(TripRequest request, Booking booking,
                                              int proposalCount) {
        return new TripRequestResponse(request, booking, true, proposalCount);
    }

    public static List<TripRequestResponse> forClient(List<TripRequest> requests,
                                                      Map<Integer, Booking> bookings) {
        return requests.stream()
                .map(r -> forClient(r, bookings.get(r.getRequestId())))
                .toList();
    }

    public static List<TripRequestResponse> forHost(List<TripRequest> requests,
                                                    Map<Integer, Booking> bookings) {
        return requests.stream()
                .map(r -> forHost(r, bookings.get(r.getRequestId())))
                .toList();
    }

    private TripRequestResponse(TripRequest request, Booking booking, boolean includeClient,
                                int proposalCount) {
        this.requestId = request.getRequestId();
        this.destination = DestinationResponse.from(request.getDestination());
        this.placeLabel = request.placeLabel();
        this.requestedPlace = request.getRequestedPlace();
        this.budgetPerPlayer = request.getBudgetPerPlayer();
        this.playerCount = request.getPlayerCount();
        this.roundsRequested = request.getRoundsRequested();
        this.nights = request.getNights();
        this.earliestStart = request.getEarliestStart();
        this.latestStart = request.getLatestStart();
        this.notes = request.getNotes();
        this.status = request.getStatus();
        this.declineReason = request.getDeclineReason();
        this.createdAt = request.getCreatedAt();
        this.budgetCeiling = request.budgetCeiling();
        this.client = includeClient ? UserResponse.from(request.getClient()) : null;
        this.booking = BookingResponse.from(booking);
        this.proposalCount = proposalCount;
    }

    public int getProposalCount() {
        return proposalCount;
    }

    public int getRequestId() {
        return requestId;
    }

    public DestinationResponse getDestination() {
        return destination;
    }

    public String getPlaceLabel() {
        return placeLabel;
    }

    public String getRequestedPlace() {
        return requestedPlace;
    }

    public BigDecimal getBudgetPerPlayer() {
        return budgetPerPlayer;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getRoundsRequested() {
        return roundsRequested;
    }

    public int getNights() {
        return nights;
    }

    public LocalDate getEarliestStart() {
        return earliestStart;
    }

    public LocalDate getLatestStart() {
        return latestStart;
    }

    public String getNotes() {
        return notes;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getBudgetCeiling() {
        return budgetCeiling;
    }

    public UserResponse getClient() {
        return client;
    }

    public BookingResponse getBooking() {
        return booking;
    }
}
