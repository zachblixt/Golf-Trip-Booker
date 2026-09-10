package org.golftripbooker.domain;

import org.golftripbooker.data.BookingRepository;
import org.golftripbooker.data.DestinationRepository;
import org.golftripbooker.data.TripRequestRepository;
import org.golftripbooker.models.Booking;
import org.golftripbooker.models.CounterReason;
import org.golftripbooker.models.Destination;
import org.golftripbooker.models.RequestStatus;
import org.golftripbooker.models.TripRequest;
import org.golftripbooker.models.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything a client does with their own requests. The host side lives in
 * BookingService.
 */
@Service
public class TripRequestService {

    private static final int MAX_PLAYERS = 24;
    private static final int MAX_ROUNDS = 14;
    private static final int MAX_NIGHTS = 30;
    private static final int MAX_NOTES_LENGTH = 500;
    /*
     * A counter has to say something. "No" alone puts the host back exactly where they
     * were and invites them to propose the same trip again, which is the failure the
     * whole counter flow exists to prevent -- so the note is required, with a low bar.
     */
    private static final int MIN_COUNTER_NOTE_LENGTH = 10;
    private static final int MAX_COUNTER_NOTE_LENGTH = 500;
    private static final int MAX_PLACE_LENGTH = 150;
    private static final BigDecimal MAX_BUDGET_PER_PLAYER = new BigDecimal("100000.00");

    // Coordinates arrive from the phone's geocoder, which means they arrive from the
    // request body, which means they are not to be trusted. decimal(9,6) holds three
    // integer digits, so an out-of-range value is a column overflow, not a bad pin.
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final TripRequestRepository repository;
    private final DestinationRepository destinationRepository;
    private final BookingRepository bookingRepository;

    public TripRequestService(TripRequestRepository repository,
                              DestinationRepository destinationRepository,
                              BookingRepository bookingRepository) {
        this.repository = repository;
        this.destinationRepository = destinationRepository;
        this.bookingRepository = bookingRepository;
    }

    // ---------- reads ----------

    /**
     * The caller's own requests. The id comes from the authenticated user, so there
     * is no parameter a client could tamper with to see someone else's list.
     */
    public List<TripRequest> findForClient(User actingUser) {
        if (actingUser == null) {
            return List.of();
        }
        return repository.findByClientId(actingUser.getUserId());
    }

    /**
     * A client may read their own request; a host may read any, because the host has
     * to see it in order to book it. Anyone else gets FORBIDDEN, not NOT_FOUND --
     * the request does exist, they simply may not have it.
     */
    public Result<TripRequest> findById(int requestId, User actingUser) {
        Result<TripRequest> result = new Result<>();

        if (isLoggedOut(result, actingUser)) {
            return result;
        }

        TripRequest existing = repository.findById(requestId);
        if (isMissing(result, existing, requestId)) {
            return result;
        }

        if (!actingUser.isHost() && existing.getClient().getUserId() != actingUser.getUserId()) {
            result.addErrorMessage("That request belongs to someone else.", ResultType.FORBIDDEN);
            return result;
        }

        result.setPayload(existing);
        return result;
    }

    /** Post-MVP Explore: what other groups actually got for their money. */
    public List<TripRequest> findBooked(BigDecimal maxCostPerPlayer, Integer destinationId) {
        return repository.findBooked(maxCostPerPlayer, destinationId);
    }

    /**
     * What a trip's detail screen shows: the confirmed booking, or the proposal the client
     * is being asked to answer. The status decides, so no caller has to.
     */
    public Booking findBooking(TripRequest request) {
        if (request == null) {
            return null;
        }
        if (request.getStatus() == RequestStatus.PROPOSED) {
            return bookingRepository.findLiveProposal(request.getRequestId());
        }

        /*
         * A pending request that once carried a proposal the host pulled back. Returning
         * it is what lets the detail screen explain why a trip the client was about to
         * decide on quietly went back to waiting. Null for a request nobody has proposed
         * on, which is the ordinary case and renders exactly as it did before.
         */
        if (request.getStatus() == RequestStatus.PENDING) {
            return bookingRepository.findLatestWithdrawnProposal(request.getRequestId());
        }

        return bookingRepository.findByRequestId(request.getRequestId());
    }

    /**
     * Which round this is. Shown on both detail screens so a negotiation that is dragging
     * is visible to the people having it, rather than only to whoever reads the table.
     */
    public int countProposals(int requestId) {
        return bookingRepository.countProposals(requestId);
    }

    /** One query for the whole page instead of one per row. */
    public Map<Integer, Booking> findBookingsFor(List<TripRequest> requests) {
        List<Integer> ids = requests.stream().map(TripRequest::getRequestId).toList();
        Map<Integer, Booking> byRequestId = new HashMap<>();
        for (Booking booking : bookingRepository.findByRequestIds(ids)) {
            byRequestId.put(booking.getRequestId(), booking);
        }
        return byRequestId;
    }

    // ---------- writes ----------

    public Result<TripRequest> create(TripRequest request, User actingUser) {
        Result<TripRequest> result = new Result<>();

        if (isLoggedOut(result, actingUser) || isNotClient(result, actingUser)) {
            return result;
        }

        if (request == null) {
            result.addErrorMessage("Request cannot be null.", ResultType.INVALID);
            return result;
        }

        Destination destination = resolveDestination(result, request);
        validate(result, request);
        if (!result.isSuccess()) {
            return result;
        }

        // The three fields a client does not get to choose.
        request.setClient(actingUser);
        request.setDestination(destination);
        request.setStatus(RequestStatus.PENDING);
        request.setDeclineReason(null);
        if (request.getCreatedAt() == null) {
            request.setCreatedAt(LocalDateTime.now());
        }

        result.setPayload(repository.create(request));
        return result;
    }

    /**
     * Post-MVP: edit a request that has not been acted on yet.
     *
     * Check order is deliberate -- identity, then existence, then ownership, then
     * state, then the shape of the data. A 404 for a request that does not exist is
     * more useful than a 400 about its player count.
     */
    public Result<TripRequest> update(TripRequest request, User actingUser) {
        Result<TripRequest> result = new Result<>();

        if (isLoggedOut(result, actingUser)) {
            return result;
        }

        if (request == null) {
            result.addErrorMessage("Request cannot be null.", ResultType.INVALID);
            return result;
        }

        if (request.getRequestId() <= 0) {
            result.addErrorMessage("Request id is required.", ResultType.INVALID);
            return result;
        }

        TripRequest existing = repository.findById(request.getRequestId());
        if (isMissing(result, existing, request.getRequestId())
                || isNotOwner(result, existing, actingUser, "edit")
                || isNotPending(result, existing, "edited")) {
            return result;
        }

        Destination destination = resolveDestination(result, request);
        validate(result, request);
        if (!result.isSuccess()) {
            return result;
        }

        // Owner, status, and creation time survive the edit untouched.
        request.setDestination(destination);
        request.setClient(existing.getClient());
        request.setStatus(existing.getStatus());
        request.setCreatedAt(existing.getCreatedAt());

        if (!repository.update(request)) {
            result.addErrorMessage("Request %s was not found.", ResultType.NOT_FOUND, request.getRequestId());
            return result;
        }

        result.setPayload(request);
        return result;
    }

    /**
     * The client agreeing to what the host arranged. This is the ONLY route to BOOKED --
     * nothing on the host side can reach it. Confirming the proposal and moving the
     * request are one change, so they are one transaction.
     *
     * The budget ceiling is not re-checked. The client has the total, the courses and the
     * dates in front of them and has just said yes to all of it, which outranks a number
     * they typed when the request was only an idea.
     */
    @Transactional
    public Result<Void> acceptProposal(int requestId, User actingUser) {
        Result<Void> result = new Result<>();

        if (isLoggedOut(result, actingUser)) {
            return result;
        }

        TripRequest existing = repository.findById(requestId);
        if (isMissing(result, existing, requestId)
                || isNotOwner(result, existing, actingUser, "accept a proposal on")) {
            return result;
        }

        Booking proposal = bookingRepository.findLiveProposal(requestId);
        if (proposal == null) {
            result.addErrorMessage("There is no live proposal on request %s to accept.",
                    ResultType.INVALID, requestId);
            return result;
        }

        // Claim the request first, as everywhere else.
        if (!repository.updateStatus(requestId,
                RequestStatus.PROPOSED, RequestStatus.BOOKED, null)) {
            result.addErrorMessage("That proposal was already answered. Reload the trip.",
                    ResultType.INVALID);
            return result;
        }

        if (!bookingRepository.confirm(proposal.getBookingId(), LocalDateTime.now())) {
            /*
             * The request says BOOKED but the proposal was answered by somebody else
             * first, so this transaction would commit a request pointing at a booking it
             * did not make. Undo all of it.
             */
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            result.addErrorMessage("That proposal was already answered. Reload the trip.",
                    ResultType.INVALID);
        }
        return result;
    }

    /**
     * The client turning a proposal down and saying why.
     *
     * The request goes back into the host's queue rather than ending, and the proposal
     * row survives carrying the reason -- which is the point. Sending it back with no
     * explanation leaves the host free to propose the same trip again, and the reason a
     * trip does not work is very often not its price.
     *
     * A client who is finished with the trip entirely cancels instead, which is a
     * different verb and a different button.
     */
    @Transactional
    public Result<Void> counterProposal(int requestId, CounterReason reason, String note,
                                        User actingUser) {
        Result<Void> result = new Result<>();

        if (isLoggedOut(result, actingUser)) {
            return result;
        }

        TripRequest existing = repository.findById(requestId);
        if (isMissing(result, existing, requestId)
                || isNotOwner(result, existing, actingUser, "answer a proposal on")) {
            return result;
        }

        if (reason == null) {
            result.addErrorMessage("Pick a reason so your host knows what to change.",
                    ResultType.INVALID);
        }

        String trimmed = note == null ? "" : note.trim();
        if (trimmed.length() < MIN_COUNTER_NOTE_LENGTH) {
            result.addErrorMessage("Tell your host what did not work, in at least %s characters.",
                    ResultType.INVALID, MIN_COUNTER_NOTE_LENGTH);
        } else if (trimmed.length() > MAX_COUNTER_NOTE_LENGTH) {
            result.addErrorMessage("Keep your note to %s characters or fewer.",
                    ResultType.INVALID, MAX_COUNTER_NOTE_LENGTH);
        }

        if (!result.isSuccess()) {
            return result;
        }

        Booking proposal = bookingRepository.findLiveProposal(requestId);
        if (proposal == null) {
            result.addErrorMessage("There is no live proposal on request %s to answer.",
                    ResultType.INVALID, requestId);
            return result;
        }

        // Claim the request first, as everywhere else.
        if (!repository.updateStatus(requestId,
                RequestStatus.PROPOSED, RequestStatus.PENDING, null)) {
            result.addErrorMessage("That proposal was already answered. Reload the trip.",
                    ResultType.INVALID);
            return result;
        }

        if (!bookingRepository.counter(proposal.getBookingId(), LocalDateTime.now(),
                reason, trimmed)) {
            /*
             * The request is back to PENDING but the proposal was answered by somebody
             * else first, so committing would leave a request in the queue whose history
             * does not explain why it is there. Undo all of it.
             */
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            result.addErrorMessage("That proposal was already answered. Reload the trip.",
                    ResultType.INVALID);
        }
        return result;
    }

    /** A client withdrawing a request nobody has booked yet. */
    public Result<Void> cancel(int requestId, User actingUser) {
        Result<Void> result = new Result<>();

        if (isLoggedOut(result, actingUser)) {
            return result;
        }

        TripRequest existing = repository.findById(requestId);
        if (isMissing(result, existing, requestId)
                || isNotOwner(result, existing, actingUser, "cancel")
                || isNotOpen(result, existing)) {
            return result;
        }

        /*
         * Conditional like every other status move: a client tapping Cancel at the same
         * moment their host books the trip must not silently undo the booking.
         */
        if (!repository.updateStatus(requestId,
                existing.getStatus(), RequestStatus.CANCELLED, null)) {
            result.addErrorMessage("Request %s was just handled by your host. Reload it to see.",
                    ResultType.INVALID, requestId);
        }
        return result;
    }

    /** Null is in range -- absence is allowed, nonsense is not. */
    private boolean outOfRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value != null && (value.compareTo(min) < 0 || value.compareTo(max) > 0);
    }

    // ---------- guards ----------
    // These answer "may this happen?" and are kept apart from validate(), which
    // only answers "is this data well-formed?".

    private boolean isLoggedOut(Result<?> result, User actingUser) {
        if (actingUser == null) {
            result.addErrorMessage("You must be logged in.", ResultType.FORBIDDEN);
            return true;
        }
        return false;
    }

    private boolean isNotClient(Result<?> result, User actingUser) {
        if (actingUser.isHost()) {
            result.addErrorMessage("Hosts book trips, they do not request them.", ResultType.FORBIDDEN);
            return true;
        }
        return false;
    }

    private boolean isMissing(Result<?> result, TripRequest existing, int requestId) {
        if (existing == null) {
            result.addErrorMessage("Request %s was not found.", ResultType.NOT_FOUND, requestId);
            return true;
        }
        return false;
    }

    private boolean isNotOwner(Result<?> result, TripRequest existing, User actingUser, String verb) {
        if (existing.getClient().getUserId() != actingUser.getUserId()) {
            result.addErrorMessage("You can only " + verb + " your own requests.", ResultType.FORBIDDEN);
            return true;
        }
        return false;
    }

    /**
     * Cancelling is allowed from PENDING or PROPOSED. Editing is not, which is why this
     * is separate from isNotPending: changing the numbers underneath a proposal the host
     * has already made would silently invalidate what they arranged.
     */
    private boolean isNotOpen(Result<?> result, TripRequest existing) {
        RequestStatus status = existing.getStatus();
        if (status != RequestStatus.PENDING && status != RequestStatus.PROPOSED) {
            result.addErrorMessage("Only an open request can be cancelled. This one is %s.",
                    ResultType.INVALID, status.name().toLowerCase());
            return true;
        }
        return false;
    }

    private boolean isNotPending(Result<?> result, TripRequest existing, String verb) {
        if (existing.getStatus() != RequestStatus.PENDING) {
            result.addErrorMessage("Only a pending request can be %s. This one is %s.",
                    ResultType.INVALID, verb, existing.getStatus().name().toLowerCase());
            return true;
        }
        return false;
    }

    // ---------- validation ----------

    /**
     * A request names either a seeded destination or a place we have never heard of --
     * exactly one, never both and never neither.
     *
     * The free-text branch is what stops the seeded list from capping who can use the
     * app: somebody in Montana can ask for Missoula today, and a host booking it turns
     * Missoula into a real destination that everybody after them can pick from a list.
     * The requests themselves are the demand signal for which places are worth adding.
     */
    private Destination resolveDestination(Result<?> result, TripRequest request) {
        boolean hasDestination = request.getDestination() != null
                && request.getDestination().getDestinationId() > 0;
        String place = request.getRequestedPlace() == null ? null : request.getRequestedPlace().trim();
        boolean hasPlace = place != null && !place.isEmpty();

        if (hasDestination && hasPlace) {
            result.addErrorMessage(
                    "Pick a destination from the list or name somewhere new, not both.",
                    ResultType.INVALID);
            return null;
        }

        if (!hasDestination && !hasPlace) {
            result.addErrorMessage("Pick a destination, or tell us where you want to go.",
                    ResultType.INVALID);
            return null;
        }

        if (hasPlace) {
            if (place.length() > MAX_PLACE_LENGTH) {
                result.addErrorMessage("That place name must be %s characters or fewer.",
                        ResultType.INVALID, MAX_PLACE_LENGTH);
            }
            request.setRequestedPlace(place);

            /*
             * A pin is optional, so a bad one is dropped rather than rejected. The
             * client cannot fix it -- it came from the OS geocoder, not from anything
             * they typed -- and failing the whole request over a coordinate would cost
             * them the trip to save the map.
             */
            if (outOfRange(request.getRequestedLatitude(), MIN_LATITUDE, MAX_LATITUDE)
                    || outOfRange(request.getRequestedLongitude(), MIN_LONGITUDE, MAX_LONGITUDE)) {
                request.setRequestedLatitude(null);
                request.setRequestedLongitude(null);
            }

            // Half a pin is no pin. Both or neither, so nothing downstream has to ask.
            if (request.getRequestedLatitude() == null || request.getRequestedLongitude() == null) {
                request.setRequestedLatitude(null);
                request.setRequestedLongitude(null);
            }

            return null;
        }

        Destination destination = destinationRepository.findById(request.getDestination().getDestinationId());
        if (destination == null) {
            result.addErrorMessage("Destination %s does not exist.",
                    ResultType.INVALID, request.getDestination().getDestinationId());
        }
        // A destination from the list wins; any place text alongside it is discarded.
        request.setRequestedPlace(null);
        request.setRequestedLatitude(null);
        request.setRequestedLongitude(null);
        return destination;
    }

    private void validate(Result<?> result, TripRequest request) {

        BigDecimal budget = request.getBudgetPerPlayer();
        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            result.addErrorMessage("Budget per player must be more than $0.", ResultType.INVALID);
        } else if (budget.compareTo(MAX_BUDGET_PER_PLAYER) > 0) {
            result.addErrorMessage("Budget per player must be $%s or less.",
                    ResultType.INVALID, MAX_BUDGET_PER_PLAYER.toPlainString());
        }

        if (request.getPlayerCount() < 1 || request.getPlayerCount() > MAX_PLAYERS) {
            result.addErrorMessage("Party size must be between 1 and %s players.",
                    ResultType.INVALID, MAX_PLAYERS);
        }

        if (request.getRoundsRequested() < 1 || request.getRoundsRequested() > MAX_ROUNDS) {
            result.addErrorMessage("Rounds must be between 1 and %s.", ResultType.INVALID, MAX_ROUNDS);
        }

        if (request.getNights() < 1 || request.getNights() > MAX_NIGHTS) {
            result.addErrorMessage("Nights must be between 1 and %s.", ResultType.INVALID, MAX_NIGHTS);
        }

        validateWindow(result, request);

        if (request.getNotes() != null && request.getNotes().length() > MAX_NOTES_LENGTH) {
            result.addErrorMessage("Notes must be %s characters or fewer.",
                    ResultType.INVALID, MAX_NOTES_LENGTH);
        }
    }

    /**
     * Both halves or neither, the same rule the coordinate pair follows: half a window
     * is no window, so nothing downstream has to ask which end is missing.
     *
     * Unlike a bad pin, a bad window is rejected rather than quietly dropped. The client
     * typed these dates, so they can fix them, and discarding them silently would let a
     * host book the trip for a week the group cannot travel.
     */
    private void validateWindow(Result<?> result, TripRequest request) {
        LocalDate earliest = request.getEarliestStart();
        LocalDate latest = request.getLatestStart();

        if (earliest == null && latest == null) {
            return;
        }

        if (earliest == null || latest == null) {
            result.addErrorMessage("Give both ends of the date range, or leave both empty.",
                    ResultType.INVALID);
            return;
        }

        if (earliest.isBefore(LocalDate.now())) {
            result.addErrorMessage("The earliest start date cannot be in the past.",
                    ResultType.INVALID);
        }

        if (latest.isBefore(earliest)) {
            result.addErrorMessage("The latest start date cannot come before the earliest.",
                    ResultType.INVALID);
        }
    }
}
