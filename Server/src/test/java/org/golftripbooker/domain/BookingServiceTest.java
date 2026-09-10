package org.golftripbooker.domain;

import org.golftripbooker.data.BookingRepository;
import org.golftripbooker.data.DestinationRepository;
import org.golftripbooker.data.TestDataHelper;
import org.golftripbooker.data.TripRequestRepository;
import org.golftripbooker.models.Booking;
import org.golftripbooker.models.Destination;
import org.golftripbooker.models.RequestStatus;
import org.golftripbooker.models.TripRequest;
import org.golftripbooker.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class BookingServiceTest {

    private static final User HOST = TestDataHelper.hostWithPassword();
    private static final User CLIENT = TestDataHelper.clientAWithPassword();

    @Autowired
    BookingService service;

    @MockitoBean
    BookingRepository repository;

    @MockitoBean
    TripRequestRepository requestRepository;

    @MockitoBean
    DestinationRepository destinationRepository;

    /** Request two: 2 players at $2,000 each and 3 rounds, so the ceiling is $4,000. */
    @BeforeEach
    void setup() {
        when(requestRepository.findById(2)).thenReturn(TestDataHelper.requestTwo());
        /*
         * The conditional move succeeds unless a test says otherwise. Without this,
         * Mockito's default false would make every propose and decline look like a lost race.
         */
        when(requestRepository.updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any())).thenReturn(true);
    }

    // ---------- propose ----------

    @Test
    void proposeHappyPath() {
        when(repository.create(any(Booking.class))).thenReturn(TestDataHelper.bookingAfterCreate());

        Result<Booking> actual = service.propose(TestDataHelper.bookingToCreate(), HOST);

        assertTrue(actual.isSuccess());
        assertEquals(TestDataHelper.bookingAfterCreate(), actual.getPayload());
    }

    /** Writing the proposal and flipping the request are one change, so both must happen. */
    @Test
    void proposeMovesTheRequestToProposed() {
        when(repository.create(any(Booking.class))).thenReturn(TestDataHelper.bookingAfterCreate());

        service.propose(TestDataHelper.bookingToCreate(), HOST);

        verify(requestRepository).updateStatus(2, RequestStatus.PENDING, RequestStatus.PROPOSED, null);
    }

    /**
     * requestTwo's window is 3 to 17 September. Until the window existed the dates
     * were only ever checked against each other, so this booking would have sailed
     * through months away from anything the group said it could do.
     */
    @Test
    void proposeFailsWhenTheStartIsOutsideTheRequestedWindow() {
        Booking outside = TestDataHelper.bookingToCreate();
        outside.setStartDate(LocalDate.of(2026, 10, 1));
        outside.setEndDate(LocalDate.of(2026, 10, 4));

        Result<Booking> actual = service.propose(outside, HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).create(any(Booking.class));
    }

    /** requestThree names no window, so any date the host can arrange is fair game. */
    @Test
    void proposeAllowsAnyStartWhenTheRequestHasNoWindow() {
        when(requestRepository.findById(3)).thenReturn(TestDataHelper.requestThree());

        Booking anyDate = TestDataHelper.bookingToCreate();
        anyDate.setRequestId(3);
        // requestThree asked for two rounds, so two courses at most.
        anyDate.setCourses("Streamsong Red\nStreamsong Blue");
        anyDate.setStartDate(LocalDate.of(2027, 3, 1));
        anyDate.setEndDate(LocalDate.of(2027, 3, 4));
        when(repository.create(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Result<Booking> actual = service.propose(anyDate, HOST);

        assertTrue(actual.isSuccess());
    }

    /** The host on the booking comes from the token, not the body. */
    @Test
    void proposeRecordsTheAuthenticatedHost() {
        Booking spoofed = TestDataHelper.bookingToCreate();
        spoofed.setBookedBy(999);

        when(repository.create(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);

        service.propose(spoofed, HOST);

        verify(repository).create(captor.capture());
        assertEquals(HOST.getUserId(), captor.getValue().getBookedBy());
    }

    /** "As a client, I can't reach host features" -- checked here, not only in the filter chain. */
    @Test
    void proposeIsForbiddenForAClient() {
        Result<Booking> actual = service.propose(TestDataHelper.bookingToCreate(), CLIENT);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
        assertTrue(actual.getErrorMessages().contains("Only a host can do that."));
        verify(repository, never()).create(any(Booking.class));
    }

    @Test
    void proposeIsForbiddenWhenNotLoggedIn() {
        Result<Booking> actual = service.propose(TestDataHelper.bookingToCreate(), null);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
        verify(repository, never()).create(any(Booking.class));
    }

    @Test
    void proposeIsNotFoundWhenTheRequestDoesNotExist() {
        Booking orphan = TestDataHelper.bookingToCreate();
        orphan.setRequestId(999);
        when(requestRepository.findById(999)).thenReturn(null);

        Result<Booking> actual = service.propose(orphan, HOST);

        assertEquals(ResultType.NOT_FOUND, actual.getResultType());
        verify(repository, never()).create(any(Booking.class));
    }

    /** One booking per request: a request that already moved cannot be booked again. */
    @Test
    void proposeFailsWhenTheRequestIsAlreadyBooked() {
        Booking duplicate = TestDataHelper.bookingToCreate();
        duplicate.setRequestId(1);
        when(requestRepository.findById(1)).thenReturn(TestDataHelper.requestOne());

        Result<Booking> actual = service.propose(duplicate, HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).create(any(Booking.class));
    }

    /**
     * $4,001 against a $4,000 ceiling, and it goes through.
     *
     * This assertion used to read the other way round. The service refused anything over
     * the ceiling, which made price the one thing a host could not decide and, by
     * omission, the one thing a client was ever asked about. The number is now shown to
     * the client on the proposal and judged by them, so the server has no opinion.
     */
    @Test
    void proposeAllowsATotalOverTheBudget() {
        Booking overBudget = TestDataHelper.bookingToCreate();
        overBudget.setTotalCost(new BigDecimal("4001.00"));
        when(repository.create(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Result<Booking> actual = service.propose(overBudget, HOST);

        assertTrue(actual.isSuccess());
        verify(requestRepository).updateStatus(2, RequestStatus.PENDING, RequestStatus.PROPOSED, null);
    }

    /** Well over, not merely a dollar over. Same answer. */
    @Test
    void proposeAllowsATotalWellOverTheBudget() {
        when(repository.create(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Result<Booking> actual = service.propose(TestDataHelper.proposalOverBudget(), HOST);

        assertTrue(actual.isSuccess());
    }

    /** Exactly at the ceiling, which was the interesting boundary and no longer is. */
    @Test
    void proposeAllowsATotalExactlyAtTheBudget() {
        Booking atCeiling = TestDataHelper.bookingToCreate();
        atCeiling.setTotalCost(new BigDecimal("4000.00"));
        when(repository.create(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Result<Booking> actual = service.propose(atCeiling, HOST);

        assertTrue(actual.isSuccess());
    }

    @Test
    void proposeFailsWithMoreCoursesThanRoundsRequested() {
        Booking tooMany = TestDataHelper.bookingToCreate();
        tooMany.setCourses("Pacific Dunes\nBandon Trails\nOld Macdonald\nSheep Ranch");

        Result<Booking> actual = service.propose(tooMany, HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).create(any(Booking.class));
    }

    @Test
    void proposeFailsWithNoCourses() {
        Booking empty = TestDataHelper.bookingToCreate();
        empty.setCourses("   \n  \n");

        Result<Booking> actual = service.propose(empty, HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
        assertTrue(actual.getErrorMessages().contains("List at least one course, one per line."));
        verify(repository, never()).create(any(Booking.class));
    }

    @Test
    void proposeFailsWhenTheEndDateIsNotAfterTheStart() {
        Booking backwards = TestDataHelper.bookingToCreate();
        backwards.setStartDate(LocalDate.of(2026, 9, 6));
        backwards.setEndDate(LocalDate.of(2026, 9, 3));

        Result<Booking> actual = service.propose(backwards, HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).create(any(Booking.class));
    }

    /**
     * Deliberately uses the IN-BUDGET fixture, which is the whole point: this used to be
     * true only of offers, and a booking under the ceiling was confirmed the moment it
     * existed. Nothing the host sends is confirmed now, however cheap it is.
     *
     * The fixture arrives carrying a confirmedAt, so this also proves propose() clears
     * what it was handed rather than trusting the caller.
     */
    @Test
    void everyProposalIsSavedUnconfirmedEvenUnderBudget() {
        when(repository.create(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);

        service.propose(TestDataHelper.bookingToCreate(), HOST);

        verify(repository).create(captor.capture());
        assertNull(captor.getValue().getConfirmedAt());
    }

    /** Nothing a host does reaches BOOKED. The only status propose() can set is PROPOSED. */
    @Test
    void proposeNeverReachesBooked() {
        when(repository.create(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        service.propose(TestDataHelper.bookingToCreate(), HOST);

        verify(requestRepository, never())
                .updateStatus(anyInt(), any(RequestStatus.class), eq(RequestStatus.BOOKED), any());
    }

    // ---------- withdraw ----------

    private static final String WITHDRAW_NOTE = "The lodge released our block.";

    @Test
    void withdrawProposalReturnsTheRequestToTheQueue() {
        when(repository.findLiveProposal(2))
                .thenReturn(TestDataHelper.proposalOverBudgetAfterCreate());
        when(repository.withdraw(anyInt(), any(LocalDateTime.class), anyString())).thenReturn(true);

        Result<Void> actual = service.withdrawProposal(2, WITHDRAW_NOTE, HOST);

        assertTrue(actual.isSuccess());
        verify(requestRepository).updateStatus(2, RequestStatus.PROPOSED, RequestStatus.PENDING, null);
    }

    @Test
    void withdrawProposalFailsWhenThereIsNoProposalOutstanding() {
        when(repository.findLiveProposal(2)).thenReturn(null);

        Result<Void> actual = service.withdrawProposal(2, WITHDRAW_NOTE, HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(requestRepository, never()).updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any());
    }

    /** The row exists but the request has already moved: somebody got there first. */
    @Test
    void withdrawProposalFailsWhenTheRequestHasAlreadyMoved() {
        when(repository.findLiveProposal(2))
                .thenReturn(TestDataHelper.proposalOverBudgetAfterCreate());
        when(requestRepository.updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any())).thenReturn(false);

        Result<Void> actual = service.withdrawProposal(2, WITHDRAW_NOTE, HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
    }

    @Test
    void withdrawProposalIsForbiddenForAClient() {
        Result<Void> actual = service.withdrawProposal(2, WITHDRAW_NOTE, CLIENT);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
    }

    /**
     * The symmetry that matters. A client must explain a counter; a host must explain a
     * withdrawal. The first pass required one and not the other, which left a client
     * watching a trip they were asked to decide on revert to pending with nothing said.
     */
    @Test
    void withdrawProposalFailsWithoutAnAdequateNote() {
        Result<Void> actual = service.withdrawProposal(2, "sorry", HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(requestRepository, never()).updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any());
    }

    @Test
    void withdrawProposalFailsWithNoNoteAtAll() {
        Result<Void> actual = service.withdrawProposal(2, null, HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
    }

    /** Nothing is written at all when the note is short -- not the status, not the row. */
    @Test
    void withdrawProposalWritesNothingWhenTheNoteIsTooShort() {
        Result<Void> actual = service.withdrawProposal(2, "   nope   ", HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).withdraw(anyInt(), any(LocalDateTime.class), anyString());
    }

    /**
     * The row is stamped, not just the request.
     *
     * Without this the proposal would stay neither confirmed nor countered, which is
     * precisely what findLiveProposal selects on -- a withdrawn proposal would come back
     * as live, and the host's history could not tell it apart from one still waiting.
     */
    @Test
    void withdrawProposalStampsTheRowWithTheNote() {
        when(repository.findLiveProposal(2))
                .thenReturn(TestDataHelper.proposalOverBudgetAfterCreate());
        when(repository.withdraw(anyInt(), any(LocalDateTime.class), anyString())).thenReturn(true);

        Result<Void> actual = service.withdrawProposal(2, "  " + WITHDRAW_NOTE + "  ", HOST);

        assertTrue(actual.isSuccess());
        // Trimmed on the way in, like every other note in the app.
        verify(repository).withdraw(eq(2), any(LocalDateTime.class), eq(WITHDRAW_NOTE));
    }

    /** Losing the race to the client's accept undoes the status move as well. */
    @Test
    void withdrawProposalFailsWhenTheClientAnsweredFirst() {
        when(repository.findLiveProposal(2))
                .thenReturn(TestDataHelper.proposalOverBudgetAfterCreate());
        when(repository.withdraw(anyInt(), any(LocalDateTime.class), anyString())).thenReturn(false);

        Result<Void> actual = service.withdrawProposal(2, WITHDRAW_NOTE, HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
    }

    // ---------- decline ----------

    @Test
    void declineHappyPath() {
        Result<Void> actual = service.decline(2, "No tee times that week.", HOST);

        assertTrue(actual.isSuccess());
        verify(requestRepository).updateStatus(2, RequestStatus.PENDING, RequestStatus.DECLINED,
                "No tee times that week.");
    }

    @Test
    void declineIsForbiddenForAClient() {
        Result<Void> actual = service.decline(2, "Because I said so.", CLIENT);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
        verify(requestRepository, never()).updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any());
    }

    /** A decline the client cannot understand is not much of a decline. */
    @Test
    void declineFailsWithoutAReason() {
        Result<Void> actual = service.decline(2, "   ", HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
        assertTrue(actual.getErrorMessages().contains("Give the client a reason."));
        verify(requestRepository, never()).updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any());
    }

    @Test
    void declineFailsWhenTheRequestIsAlreadyBooked() {
        when(requestRepository.findById(1)).thenReturn(TestDataHelper.requestOne());

        Result<Void> actual = service.decline(1, "Changed my mind.", HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(requestRepository, never()).updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any());
    }

    @Test
    void declineIsNotFoundWhenTheRequestDoesNotExist() {
        when(requestRepository.findById(999)).thenReturn(null);

        Result<Void> actual = service.decline(999, "Gone.", HOST);

        assertEquals(ResultType.NOT_FOUND, actual.getResultType());
    }

    // ---------- update (post-MVP correction) ----------

    @Test
    void updateHappyPath() {
        when(repository.findById(2)).thenReturn(TestDataHelper.bookingAfterCreate());
        when(repository.update(any(Booking.class))).thenReturn(true);

        Booking corrected = TestDataHelper.bookingAfterCreate();
        corrected.setLodging("The Inn at Bandon Dunes");

        Result<Booking> actual = service.update(corrected, HOST);

        assertTrue(actual.isSuccess());
        verify(repository).update(any(Booking.class));
    }

    /** A correction cannot move the booking onto a different request. */
    @Test
    void updatePinsTheBookingToItsOriginalRequest() {
        when(repository.findById(2)).thenReturn(TestDataHelper.bookingAfterCreate());
        when(repository.update(any(Booking.class))).thenReturn(true);

        Booking moved = TestDataHelper.bookingAfterCreate();
        moved.setRequestId(3);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        service.update(moved, HOST);

        verify(repository).update(captor.capture());
        assertEquals(2, captor.getValue().getRequestId());
    }

    @Test
    void updateIsForbiddenForAClient() {
        Result<Booking> actual = service.update(TestDataHelper.bookingAfterCreate(), CLIENT);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
        assertNull(actual.getPayload());
        verify(repository, never()).update(any(Booking.class));
    }

    /**
     * The one place a total is still capped, and it is not capped by the budget.
     *
     * A correction may not charge more than the client accepted. The old rule capped
     * corrections at the budget or the agreed total, whichever was higher, which was a
     * validation rule; this is a guard, because the booking is well-formed and the host
     * simply may not make this particular change without asking again.
     */
    @Test
    void correctionCannotRaiseThePriceOnAnAcceptedBooking() {
        when(repository.findById(2)).thenReturn(TestDataHelper.confirmedBookingForRequestTwo());

        Booking dearer = TestDataHelper.confirmedBookingForRequestTwo();
        dearer.setTotalCost(new BigDecimal("3881.00"));

        Result<Booking> actual = service.update(dearer, HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).update(any(Booking.class));
    }

    /** Cheaper is always allowed, and so is over-budget-but-unchanged. */
    @Test
    void correctionMayLowerThePriceOnAnAcceptedBooking() {
        when(repository.findById(2)).thenReturn(TestDataHelper.confirmedBookingForRequestTwo());
        when(repository.update(any(Booking.class))).thenReturn(true);

        Booking cheaper = TestDataHelper.confirmedBookingForRequestTwo();
        cheaper.setTotalCost(new BigDecimal("3500.00"));

        Result<Booking> actual = service.update(cheaper, HOST);

        assertTrue(actual.isSuccess());
    }

    @Test
    void updateIsNotFoundWhenTheBookingDoesNotExist() {
        Booking missing = TestDataHelper.bookingAfterCreate();
        missing.setBookingId(999);
        when(repository.findById(999)).thenReturn(null);

        Result<Booking> actual = service.update(missing, HOST);

        assertEquals(ResultType.NOT_FOUND, actual.getResultType());
    }

    // ---------- promotion ----------

    /**
     * The growth loop: booking a request for somewhere unseeded creates the
     * destination, so the next person finds it in the list rather than typing it again.
     */
    @Test
    void proposingAnUnseededPlaceCreatesTheDestination() {
        TripRequest unseeded = TestDataHelper.requestFourUnseeded();
        Booking booking = TestDataHelper.bookingToCreate();
        booking.setRequestId(4);
        booking.setTotalCost(new BigDecimal("2400.00")); // under 700 x 4
        booking.setCourses("Canyon River");
        // Request four can only start on 5 October, so the dates have to say so too.
        booking.setStartDate(LocalDate.of(2026, 10, 5));
        booking.setEndDate(LocalDate.of(2026, 10, 7));

        when(requestRepository.findById(4)).thenReturn(unseeded);
        when(destinationRepository.findByName("Missoula, Montana")).thenReturn(null);
        when(destinationRepository.create(any(Destination.class)))
                .thenAnswer(i -> {
                    Destination d = i.getArgument(0);
                    d.setDestinationId(9);
                    return d;
                });
        when(repository.create(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Result<Booking> actual = service.propose(booking, HOST);

        assertTrue(actual.isSuccess());
        verify(destinationRepository).create(any(Destination.class));
        verify(requestRepository).attachDestination(4, 9);
    }

    /**
     * The race the conditional update exists for: the request left PENDING between the
     * in-memory check and this write, so nothing is booked and the host is told why.
     */
    @Test
    void proposeFailsWhenSomebodyElseHandledTheRequestFirst() {
        when(requestRepository.updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any())).thenReturn(false);

        Result<Booking> actual = service.propose(TestDataHelper.bookingToCreate(), HOST);

        assertEquals(ResultType.INVALID, actual.getResultType());
        // Claim-first: losing the race means no booking row was ever written.
        verify(repository, never()).create(any(Booking.class));
    }

    /** The client's geocoded coordinates ride along, so the new pin lands on the map. */
    @Test
    void promotionCarriesTheCoordinatesTheClientSupplied() {
        TripRequest unseeded = TestDataHelper.requestFourUnseeded();
        Booking booking = TestDataHelper.bookingToCreate();
        booking.setRequestId(4);
        booking.setTotalCost(new BigDecimal("2400.00"));
        booking.setCourses("Canyon River");
        booking.setStartDate(LocalDate.of(2026, 10, 5));
        booking.setEndDate(LocalDate.of(2026, 10, 7));

        when(requestRepository.findById(4)).thenReturn(unseeded);
        when(destinationRepository.findByName(anyString())).thenReturn(null);
        when(destinationRepository.create(any(Destination.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.create(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<Destination> captor = ArgumentCaptor.forClass(Destination.class);
        service.propose(booking, HOST);

        verify(destinationRepository).create(captor.capture());
        assertEquals(new BigDecimal("46.872200"), captor.getValue().getLatitude());
        assertEquals(new BigDecimal("-113.994000"), captor.getValue().getLongitude());
    }

    /** Booking "Missoula" twice must not leave two Missoulas splitting its trips. */
    @Test
    void promotionReusesADestinationThatAlreadyHasThatName() {
        TripRequest unseeded = TestDataHelper.requestFourUnseeded();
        Booking booking = TestDataHelper.bookingToCreate();
        booking.setRequestId(4);
        booking.setTotalCost(new BigDecimal("2400.00"));
        booking.setCourses("Canyon River");
        booking.setStartDate(LocalDate.of(2026, 10, 5));
        booking.setEndDate(LocalDate.of(2026, 10, 7));

        when(requestRepository.findById(4)).thenReturn(unseeded);
        when(destinationRepository.findByName("Missoula, Montana"))
                .thenReturn(TestDataHelper.pinehurst());
        when(repository.create(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        service.propose(booking, HOST);

        verify(destinationRepository, never()).create(any(Destination.class));
        verify(requestRepository).attachDestination(4, 1);
    }

    /** A request that already has a destination is left alone. */
    @Test
    void proposingASeededDestinationCreatesNothing() {
        when(repository.create(any(Booking.class))).thenReturn(TestDataHelper.bookingAfterCreate());

        service.propose(TestDataHelper.bookingToCreate(), HOST);

        verify(destinationRepository, never()).create(any(Destination.class));
        verify(requestRepository, never()).attachDestination(anyInt(), anyInt());
    }

    @Test
    void findPendingReadsTheQueue() {
        service.findPending();

        verify(requestRepository).findByStatus(RequestStatus.PENDING);
    }

    /** How the host reaches a booking in order to correct it. */
    @Test
    void findByStatusReadsWhateverStatusItIsGiven() {
        service.findByStatus(RequestStatus.BOOKED);

        verify(requestRepository).findByStatus(RequestStatus.BOOKED);
        verify(requestRepository, never()).findByStatus(RequestStatus.PENDING);
    }
}
