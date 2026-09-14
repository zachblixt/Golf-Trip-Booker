package org.golftripbooker.domain;

import org.golftripbooker.data.BookingRepository;
import org.golftripbooker.data.DestinationRepository;
import org.golftripbooker.data.TripRequestRepository;
import org.golftripbooker.models.Booking;
import org.golftripbooker.models.Destination;
import org.golftripbooker.models.RequestStatus;
import org.golftripbooker.models.TripRequest;
import org.golftripbooker.models.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The host side. Every method here starts by proving the caller is a HOST, so the
 * role check does not live only in the filter chain.
 */
@Service
public class BookingService {

    private static final int MAX_LODGING_LENGTH = 150;
    private static final int MAX_DECLINE_REASON_LENGTH = 255;
    /*
     * The same floor TripRequestService puts on a client's counter note, and for the same
     * reason. Requiring an explanation from one side only would have been the original
     * problem wearing different clothes.
     */
    private static final int MIN_WITHDRAW_NOTE_LENGTH = 10;
    private static final int MAX_WITHDRAW_NOTE_LENGTH = 500;

    private final BookingRepository repository;
    private final TripRequestRepository requestRepository;
    private final DestinationRepository destinationRepository;

    public BookingService(BookingRepository repository,
                          TripRequestRepository requestRepository,
                          DestinationRepository destinationRepository) {
        this.repository = repository;
        this.requestRepository = requestRepository;
        this.destinationRepository = destinationRepository;
    }

    // ---------- reads ----------

    /** The queue, oldest first. */
    public List<TripRequest> findPending() {
        return findByStatus(RequestStatus.PENDING);
    }

    /**
     * Any status, for a host. Booked trips are how a host reaches a booking in order
     * to correct it -- the anonymised Explore list deliberately carries no booking id,
     * so it cannot be the way in.
     */
    public List<TripRequest> findByStatus(RequestStatus status) {
        return requestRepository.findByStatus(status);
    }

    public Booking findByRequestId(int requestId) {
        return repository.findByRequestId(requestId);
    }

    /**
     * Every proposal ever made on a request, newest first. Host-only, like everything
     * else here -- the client's own view of a request already carries the live proposal,
     * and the ones that came before it are the host's working notes, not theirs.
     */
    public Result<List<Booking>> findProposalHistory(int requestId, User actingHost) {
        Result<List<Booking>> result = new Result<>();

        if (isNotHost(result, actingHost)) {
            return result;
        }

        TripRequest request = requestRepository.findById(requestId);
        if (isMissing(result, request, requestId)) {
            return result;
        }

        result.setPayload(repository.findProposalHistory(requestId));
        return result;
    }

    // ---------- writes ----------

    /**
     * The host arranging a trip. It is always a PROPOSAL: the client is the only thing
     * that can turn it into a booking.
     *
     * The budget ceiling is deliberately not enforced here. An earlier version booked
     * outright whenever the total came in under it and only asked the client when it did
     * not, which quietly made price the single axis of acceptability -- but the host also
     * chooses the courses, the lodging and the exact dates inside the window, and a group
     * can hate any of those at any price. The ceiling is now shown to both sides and
     * judged by the person whose money it is.
     *
     * Writing the proposal and moving the request are one change, so they are one
     * transaction. The request is claimed before anything else is written, so losing a
     * race costs nothing rather than leaving a row behind on a request somebody else took.
     */
    @Transactional
    public Result<Booking> propose(Booking proposal, User actingHost) {
        Result<Booking> result = new Result<>();

        if (isNotHost(result, actingHost)) {
            return result;
        }

        if (proposal == null) {
            result.addErrorMessage("Proposal cannot be null.", ResultType.INVALID);
            return result;
        }

        TripRequest request = requestRepository.findById(proposal.getRequestId());
        if (isMissing(result, request, proposal.getRequestId())) {
            return result;
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            result.addErrorMessage("Request %s is already %s.", ResultType.INVALID,
                    request.getRequestId(), request.getStatus().name().toLowerCase());
            return result;
        }

        validate(result, proposal, request);
        if (!result.isSuccess()) {
            return result;
        }

        /*
         * Claim the request before writing anything else. The status check above ran
         * against a row read moments ago; this is the same check made atomically by the
         * database. Doing it first means a lost race writes nothing at all, rather than
         * leaving a proposal attached to a request somebody else already cancelled.
         */
        if (!requestRepository.updateStatus(request.getRequestId(),
                RequestStatus.PENDING, RequestStatus.PROPOSED, null)) {
            result.addErrorMessage("Request %s was just handled by someone else. Reload the queue.",
                    ResultType.INVALID, request.getRequestId());
            return result;
        }

        /*
         * A request for somewhere we did not know about becomes an ordinary request
         * here. Everything downstream -- Explore, the map, the filters -- then works
         * with no knowledge that this trip started as free text.
         */
        if (request.isUnseededPlace()) {
            promotePlace(request);
        }

        // Taken from the JWT, not from the body: a host cannot propose "as" someone else.
        proposal.setBookedBy(actingHost.getUserId());
        proposal.setBookedAt(LocalDateTime.now());
        // Always unconfirmed. A booking exists only once the client has said yes.
        proposal.setConfirmedAt(null);

        Booking created = repository.create(proposal);
        if (created == null) {
            /*
             * The request already says PROPOSED by now, so returning an error without
             * undoing that would commit half the change. Spring rolls back only on an
             * unchecked exception and this branch returns normally, so say it explicitly.
             */
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            result.addErrorMessage("The proposal could not be saved.", ResultType.INVALID);
            return result;
        }

        result.setPayload(created);
        return result;
    }

    /**
     * Turns the place a client typed into a real destination, reusing an existing one
     * when the name already matches -- otherwise booking "Missoula" twice would leave
     * two Missoulas in the list and split its trips across both.
     *
     * Coordinates come from the client, which geocodes the name on the device when it
     * can. Without them the destination is still perfectly usable; it just does not
     * appear on the map until somebody fills them in.
     */
    private void promotePlace(TripRequest request) {
        String place = request.getRequestedPlace().trim();

        Destination destination = destinationRepository.findByName(place);
        if (destination == null) {
            destination = destinationRepository.create(new Destination(
                    0, place, null, "Added when a host booked the first trip here.",
                    request.getRequestedLatitude(), request.getRequestedLongitude()));
        }

        requestRepository.attachDestination(request.getRequestId(), destination.getDestinationId());
        request.setDestination(destination);
    }

    /**
     * The host pulling a proposal back, putting the request in front of them again.
     *
     * The note is required, exactly as the client's is when they counter. The client is
     * watching a trip they were asked to decide on revert to pending without having done
     * anything, which is the case that most needs a sentence attached to it.
     *
     * Withdrawing is not declining. It says the arrangement fell through -- the lodge
     * released the block, the course lost the tee times -- not that the trip cannot be
     * put together at all. A host who means the latter declines the request instead.
     *
     * The proposal row is stamped as well as the request. Without that it would stay
     * neither confirmed nor countered, which is exactly what findLiveProposal looks for,
     * and a withdrawn proposal would sit in the history indistinguishable from a live one.
     */
    @Transactional
    public Result<Void> withdrawProposal(int requestId, String note, User actingHost) {
        Result<Void> result = new Result<>();

        if (isNotHost(result, actingHost)) {
            return result;
        }

        TripRequest request = requestRepository.findById(requestId);
        if (isMissing(result, request, requestId)) {
            return result;
        }

        String trimmed = note == null ? "" : note.trim();
        if (trimmed.length() < MIN_WITHDRAW_NOTE_LENGTH) {
            result.addErrorMessage(
                    "Tell the client why you are pulling this back, in at least %s characters.",
                    ResultType.INVALID, MIN_WITHDRAW_NOTE_LENGTH);
            return result;
        }
        if (trimmed.length() > MAX_WITHDRAW_NOTE_LENGTH) {
            result.addErrorMessage("Keep your note to %s characters or fewer.",
                    ResultType.INVALID, MAX_WITHDRAW_NOTE_LENGTH);
            return result;
        }

        Booking proposal = repository.findLiveProposal(requestId);

        /*
         * The note has nowhere to live without a row to put it on, so unlike every other
         * conditional move in this class the row is checked before the status. A PROPOSED
         * request with no live proposal is a state that should not exist; saying so is
         * better than silently reverting the status and dropping what the host wrote.
         */
        if (proposal == null) {
            result.addErrorMessage("Request %s has no proposal outstanding.",
                    ResultType.INVALID, requestId);
            return result;
        }

        if (!requestRepository.updateStatus(requestId,
                RequestStatus.PROPOSED, RequestStatus.PENDING, null)) {
            result.addErrorMessage("Request %s has no proposal outstanding.",
                    ResultType.INVALID, requestId);
            return result;
        }

        if (!repository.withdraw(proposal.getBookingId(), LocalDateTime.now(), trimmed)) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            result.addErrorMessage("The client answered that proposal first. Reload the queue.",
                    ResultType.INVALID);
        }
        return result;
    }

    /** Post-MVP: correcting a booking after the fact. The request stays BOOKED. */
    public Result<Booking> update(Booking booking, User actingHost) {
        Result<Booking> result = new Result<>();

        if (isNotHost(result, actingHost)) {
            return result;
        }

        if (booking == null || booking.getBookingId() <= 0) {
            result.addErrorMessage("Booking id is required.", ResultType.INVALID);
            return result;
        }

        Booking existing = repository.findById(booking.getBookingId());
        if (existing == null) {
            result.addErrorMessage("Booking %s was not found.", ResultType.NOT_FOUND, booking.getBookingId());
            return result;
        }

        // The booking cannot be moved to a different request.
        booking.setRequestId(existing.getRequestId());

        TripRequest request = requestRepository.findById(existing.getRequestId());
        if (isMissing(result, request, existing.getRequestId())) {
            return result;
        }

        validate(result, booking, request);
        if (!result.isSuccess()) {
            return result;
        }

        /*
         * A correction may not raise the price on a trip the client already agreed to.
         *
         * This is a guard rather than a validation rule: the booking is perfectly
         * well-formed, the host simply is not allowed to make this particular change after
         * the fact. Charging more than what was accepted is a new proposal, which is a
         * different verb and needs a new yes.
         */
        if (existing.isConfirmed() && booking.getTotalCost() != null
                && booking.getTotalCost().compareTo(existing.getTotalCost()) > 0) {
            result.addErrorMessage(
                    "The client agreed to $%s. Charging more than that needs a new proposal.",
                    ResultType.INVALID, existing.getTotalCost().toPlainString());
            return result;
        }

        booking.setBookedBy(existing.getBookedBy());
        booking.setBookedAt(existing.getBookedAt());

        if (!repository.update(booking)) {
            result.addErrorMessage("Booking %s was not found.", ResultType.NOT_FOUND, booking.getBookingId());
            return result;
        }

        result.setPayload(booking);
        return result;
    }

    public Result<Void> decline(int requestId, String reason, User actingHost) {
        Result<Void> result = new Result<>();

        if (isNotHost(result, actingHost)) {
            return result;
        }

        TripRequest request = requestRepository.findById(requestId);
        if (isMissing(result, request, requestId)) {
            return result;
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            result.addErrorMessage("Request %s is already %s.", ResultType.INVALID,
                    requestId, request.getStatus().name().toLowerCase());
            return result;
        }

        if (reason == null || reason.isBlank()) {
            result.addErrorMessage("Give the client a reason.", ResultType.INVALID);
            return result;
        }

        if (reason.length() > MAX_DECLINE_REASON_LENGTH) {
            result.addErrorMessage("The reason must be %s characters or fewer.",
                    ResultType.INVALID, MAX_DECLINE_REASON_LENGTH);
            return result;
        }

        if (!requestRepository.updateStatus(requestId,
                RequestStatus.PENDING, RequestStatus.DECLINED, reason.trim())) {
            result.addErrorMessage("Request %s was just handled by someone else. Reload the queue.",
                    ResultType.INVALID, requestId);
        }
        return result;
    }

    // ---------- guards ----------

    private boolean isNotHost(Result<?> result, User actingUser) {
        if (actingUser == null) {
            result.addErrorMessage("You must be logged in.", ResultType.FORBIDDEN);
            return true;
        }
        if (!actingUser.isHost()) {
            result.addErrorMessage("Only a host can do that.", ResultType.FORBIDDEN);
            return true;
        }
        return false;
    }

    private boolean isMissing(Result<?> result, TripRequest request, int requestId) {
        if (request == null) {
            result.addErrorMessage("Request %s was not found.", ResultType.NOT_FOUND, requestId);
            return true;
        }
        return false;
    }

    // ---------- validation ----------

    /**
     * Exactly the rules propose() applies, exposed so a drafted proposal can be checked
     * before any host sees it. Validates and nothing else: writes no row, claims no
     * request, touches no status.
     *
     * The point of sharing this rather than re-stating the rules in the drafter is that a
     * suggestion has to clear the same bar a human's typing does. If these rules change,
     * they change for both at once.
     */
    public Result<Booking> validateProposal(Booking proposal, TripRequest request) {
        Result<Booking> result = new Result<>();
        validate(result, proposal, request);
        return result;
    }

    /**
     * Validated against the request, not in isolation. The interesting rules are relative:
     * you cannot list more courses than rounds asked for, and you cannot start on a date
     * outside the window the group gave.
     *
     * Cost is checked for being a positive number and nothing else. Whether it is too much
     * is not a question this method can answer -- it is the client's to answer, on the
     * proposal screen, with the courses and the dates in front of them.
     */
    private void validate(Result<?> result, Booking booking, TripRequest request) {

        List<String> courses = booking.courseList();
        if (courses.isEmpty()) {
            result.addErrorMessage("List at least one course, one per line.", ResultType.INVALID);
        } else if (courses.size() > request.getRoundsRequested()) {
            result.addErrorMessage("The client asked for %s rounds, so list %s courses at most.",
                    ResultType.INVALID, request.getRoundsRequested(), request.getRoundsRequested());
        }

        if (booking.getLodging() != null && booking.getLodging().length() > MAX_LODGING_LENGTH) {
            result.addErrorMessage("Lodging must be %s characters or fewer.",
                    ResultType.INVALID, MAX_LODGING_LENGTH);
        }

        if (booking.getStartDate() == null) {
            result.addErrorMessage("A start date is required.", ResultType.INVALID);
        }

        if (booking.getEndDate() == null) {
            result.addErrorMessage("An end date is required.", ResultType.INVALID);
        }

        if (booking.getStartDate() != null && booking.getEndDate() != null
                && !booking.getEndDate().isAfter(booking.getStartDate())) {
            result.addErrorMessage("The end date must come after the start date.", ResultType.INVALID);
        }

        /*
         * The first rule that ties a booking back to what was actually asked for. Until
         * now the dates were checked only against each other, so a host could book March
         * for a group that said October and nothing anywhere complained.
         */
        if (!request.windowAllows(booking.getStartDate())) {
            result.addErrorMessage("This group can start between %s and %s. Pick a date in that "
                            + "range, or decline the request.",
                    ResultType.INVALID,
                    request.getEarliestStart(), request.getLatestStart());
        }

        BigDecimal total = booking.getTotalCost();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            result.addErrorMessage("Total cost must be more than $0.", ResultType.INVALID);
        }
    }
}
