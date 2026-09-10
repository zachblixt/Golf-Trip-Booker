package org.golftripbooker.data;

import org.golftripbooker.models.Booking;
import org.golftripbooker.models.CounterReason;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository {

    /**
     * The CONFIRMED booking for a request, or null. Live proposals are deliberately
     * invisible here -- a request may collect several over time, and none of them is
     * "the booking" until a client says so.
     */
    Booking findByRequestId(int requestId) throws DataAccessException;

    /**
     * The live proposal on a request: the most recent row that is neither confirmed,
     * countered, nor withdrawn. Dead rows stay in the table as history, so excluding them
     * here is what stops an already-answered proposal being answered twice. Only
     * meaningful while the request itself is PROPOSED.
     */
    Booking findLiveProposal(int requestId) throws DataAccessException;

    /** Every proposal ever made on a request, newest first. Feeds the host's history. */
    List<Booking> findProposalHistory(int requestId) throws DataAccessException;

    /**
     * How many proposals a request has collected. A count rather than the list, because
     * the detail screens want the round number and not the rounds themselves.
     */
    int countProposals(int requestId) throws DataAccessException;

    Booking findById(int bookingId) throws DataAccessException;

    /**
     * Used to attach bookings to a list of requests in one query instead of one
     * query per row. Confirmed only, for the same reason as findByRequestId -- a list
     * screen shows a PROPOSED pill and the detail screen fetches the proposal itself.
     */
    List<Booking> findByRequestIds(List<Integer> requestIds) throws DataAccessException;

    Booking create(Booking booking) throws DataAccessException;

    /** Post-MVP: a host correcting a booking they already made. */
    boolean update(Booking booking) throws DataAccessException;

    /**
     * Turns a proposal into a booking. Conditional on it still being live, so two accepts
     * racing each other cannot both win.
     */
    boolean confirm(int bookingId, LocalDateTime confirmedAt) throws DataAccessException;

    /**
     * The client refusing a proposal, with the reason they gave. Conditional on the same
     * liveness confirm() requires, from the other side: a proposal already accepted or
     * already countered cannot be countered again.
     */
    boolean counter(int bookingId, LocalDateTime counteredAt,
                    CounterReason reason, String note) throws DataAccessException;

    /**
     * The host pulling a proposal back, with the note they owe the client. Same
     * conditional, third door into it.
     */
    boolean withdraw(int bookingId, LocalDateTime withdrawnAt, String note)
            throws DataAccessException;

    /**
     * The most recent proposal the HOST pulled back, or null.
     *
     * A client whose trip reverts to pending is owed an explanation, and this is where
     * the detail screen finds it. Deliberately not "the latest dead proposal" -- one the
     * client countered themselves needs no explaining back to them.
     */
    Booking findLatestWithdrawnProposal(int requestId) throws DataAccessException;
}
