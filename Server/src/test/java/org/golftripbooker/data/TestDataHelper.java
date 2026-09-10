package org.golftripbooker.data;

import org.golftripbooker.models.Booking;
import org.golftripbooker.models.Destination;
import org.golftripbooker.models.RequestStatus;
import org.golftripbooker.models.Role;
import org.golftripbooker.models.TripRequest;
import org.golftripbooker.models.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures that mirror set_known_good_state() in test-schema.sql.
 * If you change the procedure, change this file in the same commit.
 */
public class TestDataHelper {

    // ---------- users ----------

    public static User clientAWithPassword() {
        return new User(1, "a@a.com", "clientA", "passwordA", Role.CLIENT);
    }

    public static User clientBWithPassword() {
        return new User(2, "b@b.com", "clientB", "passwordB", Role.CLIENT);
    }

    public static User hostWithPassword() {
        return new User(3, "h@h.com", "hostH", "passwordH", Role.HOST);
    }

    /**
     * The same people, but with a null password.
     *
     * TripRequestMapper deliberately never selects the client's password, so a client
     * read back through a request always has a null there. Assert a fetched request
     * against clientAWithPassword() and it fails on that one field.
     */
    public static User clientAAsRequester() {
        return new User(1, "a@a.com", "clientA", null, Role.CLIENT);
    }

    public static User clientBAsRequester() {
        return new User(2, "b@b.com", "clientB", null, Role.CLIENT);
    }

    public static User userToCreate() {
        return new User(0, "c@c.com", "clientC", "passwordC", Role.CLIENT);
    }

    public static User userAfterCreate() {
        User user = userToCreate();
        user.setUserId(4);
        return user;
    }

    // ---------- destinations ----------

    /*
     * Coordinates carry an explicit scale of 6 to match decimal(9,6). BigDecimal
     * equality is scale-sensitive, so "35.1954" and "35.195400" are not equal and
     * the whole-object assertions below would fail on a value that is numerically
     * identical.
     */
    public static Destination pinehurst() {
        return new Destination(1, "Pinehurst", "North Carolina", "Nine courses in one village.",
                new BigDecimal("35.195400"), new BigDecimal("-79.469500"));
    }

    public static Destination bandonDunes() {
        return new Destination(2, "Bandon Dunes", "Oregon", "Walking-only links on the Pacific.",
                new BigDecimal("43.189000"), new BigDecimal("-124.392000"));
    }

    public static Destination streamsong() {
        return new Destination(3, "Streamsong", "Florida", "Three courses built on a phosphate mine.",
                new BigDecimal("27.662000"), new BigDecimal("-81.899000"));
    }

    // ---------- trip requests ----------

    /** clientA's booked Pinehurst trip. */
    public static TripRequest requestOne() {
        return new TripRequest(1,
                clientAAsRequester(),
                pinehurst(),
                new BigDecimal("1200.00"),
                4, 3, 4,
                LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 12),
                "Two rounds a day if the budget allows",
                RequestStatus.BOOKED,
                null,
                LocalDateTime.of(2026, 8, 2, 9, 0, 0));
    }

    /**
     * clientA's pending Bandon Dunes trip. The one most tests act on, and the one
     * carrying a real travel window: 3 to 17 September. bookingToCreate starts on the
     * 3rd, so the happy path also proves an in-window booking is allowed.
     */
    public static TripRequest requestTwo() {
        return new TripRequest(2,
                clientAAsRequester(),
                bandonDunes(),
                new BigDecimal("2000.00"),
                2, 3, 3,
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 17),
                null,
                RequestStatus.PENDING,
                null,
                LocalDateTime.of(2026, 8, 9, 10, 0, 0));
    }

    /**
     * clientB's pending Streamsong trip. Belongs to someone else, on purpose, and has
     * no travel window at all -- which is what covers "this group can go any time".
     */
    public static TripRequest requestThree() {
        return new TripRequest(3,
                clientBAsRequester(),
                streamsong(),
                new BigDecimal("900.00"),
                8, 2, 2,
                null, null,
                null,
                RequestStatus.PENDING,
                null,
                LocalDateTime.of(2026, 8, 12, 11, 0, 0));
    }

    /** clientB's request for a place that is not a destination yet. */
    public static TripRequest requestFourUnseeded() {
        return new TripRequest(4,
                clientBAsRequester(),
                null,
                "Missoula, Montana",
                new BigDecimal("46.872200"),
                new BigDecimal("-113.994000"),
                new BigDecimal("700.00"),
                4, 2, 2,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5),
                null,
                RequestStatus.PENDING,
                null,
                LocalDateTime.of(2026, 8, 15, 12, 0, 0));
    }

    /** A new request naming somewhere unseeded, for the service tests. */
    public static TripRequest placeRequestToCreate() {
        TripRequest request = new TripRequest();
        request.setClient(clientAAsRequester());
        request.setRequestedPlace("Missoula, Montana");
        request.setRequestedLatitude(new BigDecimal("46.872200"));
        request.setRequestedLongitude(new BigDecimal("-113.994000"));
        request.setBudgetPerPlayer(new BigDecimal("700.00"));
        request.setPlayerCount(4);
        request.setRoundsRequested(2);
        request.setNights(2);
        request.setEarliestStart(LocalDate.now().plusMonths(2));
        request.setLatestStart(LocalDate.now().plusMonths(2).plusDays(14));
        request.setStatus(RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.of(2026, 8, 20, 10, 0, 0));
        return request;
    }

    /**
     * The window is computed rather than hard-coded because TripRequestService rejects
     * an earliest start in the past, and a literal date would quietly start failing
     * that test the year after it was written.
     */
    public static TripRequest requestToCreate() {
        return new TripRequest(0,
                clientAAsRequester(),
                streamsong(),
                new BigDecimal("1500.00"),
                4, 2, 3,
                LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(2).plusDays(14),
                "Prefer a Thursday start",
                RequestStatus.PENDING,
                null,
                LocalDateTime.of(2026, 8, 20, 10, 0, 0));
    }

    /** Five, not four: set_known_good_state seeds four requests. */
    public static TripRequest requestAfterCreate() {
        TripRequest request = requestToCreate();
        request.setRequestId(5);
        return request;
    }

    // ---------- bookings ----------

    public static Booking bookingOne() {
        return new Booking(1, 1,
                "Pinehurst No. 2\nPinehurst No. 4\nMid Pines",
                "The Carolina Hotel",
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 16),
                new BigDecimal("4650.00"),
                "Arrive Monday, first tee Tuesday.",
                3,
                LocalDateTime.of(2026, 8, 3, 12, 0, 0),
                // Seeded as a real booking, so it is confirmed.
                LocalDateTime.of(2026, 8, 3, 12, 0, 0));
    }

    /** A booking for requestTwo: 2 players at $2,000 each, so the ceiling is $4,000. */
    public static Booking bookingToCreate() {
        return new Booking(0, 2,
                "Pacific Dunes\nBandon Trails\nOld Macdonald",
                "Lily Pond",
                LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 6),
                new BigDecimal("3880.00"),
                "One round a day, walking.",
                3,
                LocalDateTime.of(2026, 8, 20, 14, 0, 0),
                // A row the repository tests insert directly. propose() never sets this.
                LocalDateTime.of(2026, 8, 20, 14, 0, 0));
    }

    public static Booking bookingAfterCreate() {
        Booking booking = bookingToCreate();
        booking.setBookingId(2);
        return booking;
    }

    /**
     * The same trip as bookingToCreate but $600 over requestTwo's $4,000 ceiling.
     *
     * It used to be the fixture that proved book() refused what offer() accepted. There
     * is no such split any more -- propose() takes it, and the client is the one who gets
     * to mind the price -- so it now proves the opposite: over budget is not an error.
     */
    public static Booking proposalOverBudget() {
        return new Booking(0, 2,
                "Pacific Dunes\nBandon Trails\nOld Macdonald",
                "Lily Pond",
                LocalDate.of(2026, 9, 3),
                LocalDate.of(2026, 9, 6),
                new BigDecimal("4600.00"),
                "One round a day, walking.",
                3,
                LocalDateTime.of(2026, 8, 20, 14, 0, 0),
                null);
    }

    public static Booking proposalOverBudgetAfterCreate() {
        Booking proposal = proposalOverBudget();
        proposal.setBookingId(2);
        return proposal;
    }

    /**
     * A confirmed booking on requestTwo, which is what a correction acts on. Corrections
     * are the one place a total is still capped -- not by the budget, but by what the
     * client already said yes to.
     */
    public static Booking confirmedBookingForRequestTwo() {
        Booking booking = bookingToCreate();
        booking.setBookingId(2);
        booking.setConfirmedAt(LocalDateTime.of(2026, 8, 21, 9, 0, 0));
        return booking;
    }
}
