package org.golftripbooker.domain;

import org.golftripbooker.data.BookingRepository;
import org.golftripbooker.data.DestinationRepository;
import org.golftripbooker.data.TestDataHelper;
import org.golftripbooker.data.TripRequestRepository;
import org.golftripbooker.models.CounterReason;
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
class TripRequestServiceTest {

    /** clientA owns requests one and two. */
    private static final User OWNER = TestDataHelper.clientAWithPassword();
    /** clientB owns request three, and has no business touching the others. */
    private static final User SOMEONE_ELSE = TestDataHelper.clientBWithPassword();
    private static final User HOST = TestDataHelper.hostWithPassword();

    @Autowired
    TripRequestService service;

    @MockitoBean
    TripRequestRepository repository;

    @MockitoBean
    DestinationRepository destinationRepository;

    @MockitoBean
    BookingRepository bookingRepository;

    @BeforeEach
    void setup() {
        when(destinationRepository.findById(3)).thenReturn(TestDataHelper.streamsong());
        // As in BookingServiceTest: the conditional move succeeds unless a test overrides it.
        when(repository.updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any())).thenReturn(true);
    }

    // ---------- create ----------

    @Test
    void createHappyPath() {
        when(repository.create(any(TripRequest.class))).thenReturn(TestDataHelper.requestAfterCreate());

        Result<TripRequest> actual = service.create(TestDataHelper.requestToCreate(), OWNER);

        assertTrue(actual.isSuccess());
        assertEquals(TestDataHelper.requestAfterCreate(), actual.getPayload());
    }

    /**
     * "As a client, I can't see or change another client's request" starts here: the
     * owner is taken from the authenticated user, so a body claiming someone else
     * changes nothing.
     */
    @Test
    void createIgnoresTheClientFromTheRequestBody() {
        TripRequest spoofed = TestDataHelper.requestToCreate();
        spoofed.setClient(SOMEONE_ELSE);

        when(repository.create(any(TripRequest.class))).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<TripRequest> captor = ArgumentCaptor.forClass(TripRequest.class);

        service.create(spoofed, OWNER);

        verify(repository).create(captor.capture());
        assertEquals(OWNER.getUserId(), captor.getValue().getClient().getUserId());
    }

    /** A request cannot arrive already booked. */
    @Test
    void createForcesTheStatusToPending() {
        TripRequest spoofed = TestDataHelper.requestToCreate();
        spoofed.setStatus(RequestStatus.BOOKED);

        when(repository.create(any(TripRequest.class))).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<TripRequest> captor = ArgumentCaptor.forClass(TripRequest.class);

        service.create(spoofed, OWNER);

        verify(repository).create(captor.capture());
        assertEquals(RequestStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    void createFailsWhenNotLoggedIn() {
        Result<TripRequest> actual = service.create(TestDataHelper.requestToCreate(), null);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
        verify(repository, never()).create(any(TripRequest.class));
    }

    /** Hosts book trips; they do not request them. */
    @Test
    void createFailsForAHost() {
        Result<TripRequest> actual = service.create(TestDataHelper.requestToCreate(), HOST);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
        verify(repository, never()).create(any(TripRequest.class));
    }

    @Test
    void createFailsWhenTheDestinationDoesNotExist() {
        TripRequest toCreate = TestDataHelper.requestToCreate();
        toCreate.getDestination().setDestinationId(999);

        Result<TripRequest> actual = service.create(toCreate, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).create(any(TripRequest.class));
    }

    @Test
    void createFailsWhenBudgetIsZero() {
        TripRequest toCreate = TestDataHelper.requestToCreate();
        toCreate.setBudgetPerPlayer(BigDecimal.ZERO);

        Result<TripRequest> actual = service.create(toCreate, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        assertTrue(actual.getErrorMessages().contains("Budget per player must be more than $0."));
        verify(repository, never()).create(any(TripRequest.class));
    }

    @Test
    void createFailsWhenThereAreNoPlayers() {
        TripRequest toCreate = TestDataHelper.requestToCreate();
        toCreate.setPlayerCount(0);

        Result<TripRequest> actual = service.create(toCreate, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).create(any(TripRequest.class));
    }

    @Test
    void createFailsWhenThereAreNoRounds() {
        TripRequest toCreate = TestDataHelper.requestToCreate();
        toCreate.setRoundsRequested(0);

        Result<TripRequest> actual = service.create(toCreate, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).create(any(TripRequest.class));
    }

    @Test
    void createFailsWhenTheEarliestStartIsInThePast() {
        TripRequest toCreate = TestDataHelper.requestToCreate();
        toCreate.setEarliestStart(LocalDate.now().minusDays(1));

        Result<TripRequest> actual = service.create(toCreate, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        assertTrue(actual.getErrorMessages()
                .contains("The earliest start date cannot be in the past."));
        verify(repository, never()).create(any(TripRequest.class));
    }

    /** Half a window is no window, the same rule half a coordinate pair follows. */
    @Test
    void createFailsWithOnlyOneEndOfTheWindow() {
        TripRequest toCreate = TestDataHelper.requestToCreate();
        toCreate.setLatestStart(null);

        Result<TripRequest> actual = service.create(toCreate, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        assertTrue(actual.getErrorMessages()
                .contains("Give both ends of the date range, or leave both empty."));
        verify(repository, never()).create(any(TripRequest.class));
    }

    @Test
    void createFailsWhenTheWindowEndsBeforeItStarts() {
        TripRequest toCreate = TestDataHelper.requestToCreate();
        toCreate.setEarliestStart(LocalDate.now().plusMonths(3));
        toCreate.setLatestStart(LocalDate.now().plusMonths(2));

        Result<TripRequest> actual = service.create(toCreate, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        assertTrue(actual.getErrorMessages()
                .contains("The latest start date cannot come before the earliest."));
        verify(repository, never()).create(any(TripRequest.class));
    }

    /** No window at all is a group with no constraint, not a validation failure. */
    @Test
    void createAllowsARequestWithNoWindow() {
        TripRequest toCreate = TestDataHelper.requestToCreate();
        toCreate.setEarliestStart(null);
        toCreate.setLatestStart(null);
        when(repository.create(any(TripRequest.class)))
                .thenReturn(TestDataHelper.requestAfterCreate());

        Result<TripRequest> actual = service.create(toCreate, OWNER);

        assertTrue(actual.isSuccess());
    }

    // ---------- somewhere not seeded yet ----------

    /**
     * The escape hatch from a closed destination list: somebody in Montana can ask
     * for Missoula today, and a host booking it turns Missoula into a destination
     * everybody after them picks from the list.
     */
    @Test
    void createAcceptsAPlaceThatIsNotADestinationYet() {
        when(repository.create(any(TripRequest.class))).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<TripRequest> captor = ArgumentCaptor.forClass(TripRequest.class);

        Result<TripRequest> actual = service.create(TestDataHelper.placeRequestToCreate(), OWNER);

        assertTrue(actual.isSuccess());
        verify(repository).create(captor.capture());
        assertNull(captor.getValue().getDestination());
        assertEquals("Missoula, Montana", captor.getValue().getRequestedPlace());
    }

    @Test
    void createFailsWhenNeitherADestinationNorAPlaceIsGiven() {
        TripRequest empty = TestDataHelper.placeRequestToCreate();
        empty.setRequestedPlace(null);

        Result<TripRequest> actual = service.create(empty, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        assertTrue(actual.getErrorMessages()
                .contains("Pick a destination, or tell us where you want to go."));
        verify(repository, never()).create(any(TripRequest.class));
    }

    @Test
    void createFailsWhenBothADestinationAndAPlaceAreGiven() {
        TripRequest both = TestDataHelper.requestToCreate();
        both.setRequestedPlace("Missoula, Montana");

        Result<TripRequest> actual = service.create(both, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).create(any(TripRequest.class));
    }

    /** A destination from the list wins, and any stray place text is dropped. */
    @Test
    void createDiscardsPlaceTextWhenADestinationWasPicked() {
        when(repository.create(any(TripRequest.class))).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<TripRequest> captor = ArgumentCaptor.forClass(TripRequest.class);

        service.create(TestDataHelper.requestToCreate(), OWNER);

        verify(repository).create(captor.capture());
        assertNull(captor.getValue().getRequestedPlace());
    }

    /**
     * The pin comes from the phone's geocoder by way of the request body, so it is
     * client input like any other. decimal(9,6) holds three integer digits -- a
     * latitude of 999 is a column overflow, not a bad map marker.
     */
    @Test
    void createDropsCoordinatesThatAreNotOnEarth() {
        when(repository.create(any(TripRequest.class))).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<TripRequest> captor = ArgumentCaptor.forClass(TripRequest.class);

        TripRequest nonsense = TestDataHelper.placeRequestToCreate();
        nonsense.setRequestedLatitude(new BigDecimal("999.000000"));

        Result<TripRequest> actual = service.create(nonsense, OWNER);

        // The trip still goes through -- the client cannot fix a coordinate they
        // never typed, and the host only needs the name.
        assertTrue(actual.isSuccess());
        verify(repository).create(captor.capture());
        assertEquals("Missoula, Montana", captor.getValue().getRequestedPlace());
        assertNull(captor.getValue().getRequestedLatitude());
        assertNull(captor.getValue().getRequestedLongitude());
    }

    /** Half a pin is no pin: a longitude with no latitude cannot be mapped. */
    @Test
    void createDropsAHalfCoordinate() {
        when(repository.create(any(TripRequest.class))).thenAnswer(i -> i.getArgument(0));
        ArgumentCaptor<TripRequest> captor = ArgumentCaptor.forClass(TripRequest.class);

        TripRequest half = TestDataHelper.placeRequestToCreate();
        half.setRequestedLatitude(null);

        assertTrue(service.create(half, OWNER).isSuccess());

        verify(repository).create(captor.capture());
        assertNull(captor.getValue().getRequestedLongitude());
    }

    // ---------- reads ----------

    @Test
    void findForClientOnlyEverAsksForTheCallersOwnId() {
        service.findForClient(SOMEONE_ELSE);

        verify(repository).findByClientId(SOMEONE_ELSE.getUserId());
        verify(repository, never()).findByClientId(OWNER.getUserId());
    }

    @Test
    void findForClientReturnsNothingWhenNotLoggedIn() {
        assertTrue(service.findForClient(null).isEmpty());
        verify(repository, never()).findByClientId(anyInt());
    }

    @Test
    void findByIdHappyPathForTheOwner() {
        when(repository.findById(2)).thenReturn(TestDataHelper.requestTwo());

        Result<TripRequest> actual = service.findById(2, OWNER);

        assertTrue(actual.isSuccess());
        assertEquals(TestDataHelper.requestTwo(), actual.getPayload());
    }

    /** "As a client, I can't see or change another client's request." */
    @Test
    void findByIdIsForbiddenForAnotherClient() {
        when(repository.findById(2)).thenReturn(TestDataHelper.requestTwo());

        Result<TripRequest> actual = service.findById(2, SOMEONE_ELSE);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
        assertNull(actual.getPayload());
    }

    /** The host has to be able to read a request in order to book it. */
    @Test
    void findByIdIsAllowedForAHost() {
        when(repository.findById(2)).thenReturn(TestDataHelper.requestTwo());

        Result<TripRequest> actual = service.findById(2, HOST);

        assertTrue(actual.isSuccess());
    }

    @Test
    void findByIdIsNotFoundWhenTheRequestDoesNotExist() {
        when(repository.findById(999)).thenReturn(null);

        Result<TripRequest> actual = service.findById(999, OWNER);

        assertEquals(ResultType.NOT_FOUND, actual.getResultType());
    }

    @Test
    void findByIdIsForbiddenWhenNotLoggedIn() {
        Result<TripRequest> actual = service.findById(2, null);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
        verify(repository, never()).findById(anyInt());
    }

    // ---------- proposals ----------

    /** requestTwo, but with a host's proposal outstanding on it. */
    private TripRequest proposedRequest() {
        TripRequest proposed = TestDataHelper.requestTwo();
        proposed.setStatus(RequestStatus.PROPOSED);
        return proposed;
    }

    @Test
    void acceptProposalConfirmsTheBookingAndBooksTheRequest() {
        when(repository.findById(2)).thenReturn(proposedRequest());
        when(bookingRepository.findLiveProposal(2))
                .thenReturn(TestDataHelper.proposalOverBudgetAfterCreate());
        when(bookingRepository.confirm(anyInt(), any())).thenReturn(true);

        Result<Void> actual = service.acceptProposal(2, OWNER);

        assertTrue(actual.isSuccess());
        verify(repository).updateStatus(2, RequestStatus.PROPOSED, RequestStatus.BOOKED, null);
        verify(bookingRepository).confirm(anyInt(), any());
    }

    /**
     * The only place a client changes a status a host set, so this is the ownership
     * check that matters most in the whole API.
     */
    @Test
    void acceptProposalIsForbiddenForAnotherClient() {
        when(repository.findById(2)).thenReturn(proposedRequest());

        Result<Void> actual = service.acceptProposal(2, SOMEONE_ELSE);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
        verify(bookingRepository, never()).confirm(anyInt(), any());
    }

    @Test
    void acceptProposalFailsWhenThereIsNoLiveProposal() {
        when(repository.findById(2)).thenReturn(proposedRequest());
        when(bookingRepository.findLiveProposal(2)).thenReturn(null);

        Result<Void> actual = service.acceptProposal(2, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
    }

    /** Countering is not declining: it goes back in the queue for the host to try again. */
    @Test
    void counterSendsTheRequestBackToPending() {
        when(repository.findById(2)).thenReturn(proposedRequest());
        when(bookingRepository.findLiveProposal(2))
                .thenReturn(TestDataHelper.proposalOverBudgetAfterCreate());
        when(bookingRepository.counter(anyInt(), any(), any(), anyString())).thenReturn(true);

        Result<Void> actual = service.counterProposal(
                2, CounterReason.DATES, "That weekend is a wedding.", OWNER);

        assertTrue(actual.isSuccess());
        verify(repository).updateStatus(2, RequestStatus.PROPOSED, RequestStatus.PENDING, null);
    }

    /**
     * The reason and the note go onto the proposal row, not the request.
     *
     * Sending it back with nothing attached is the failure this whole flow exists to
     * stop: the host sees the request return to the queue looking exactly as it did and
     * proposes the same trip again.
     */
    @Test
    void counterRecordsTheReasonAndNoteOnTheProposal() {
        when(repository.findById(2)).thenReturn(proposedRequest());
        when(bookingRepository.findLiveProposal(2))
                .thenReturn(TestDataHelper.proposalOverBudgetAfterCreate());
        when(bookingRepository.counter(anyInt(), any(), any(), anyString())).thenReturn(true);

        service.counterProposal(2, CounterReason.COURSES, "We played Bandon Trails in May.", OWNER);

        verify(bookingRepository).counter(eq(2), any(), eq(CounterReason.COURSES),
                eq("We played Bandon Trails in May."));
    }

    /** A counter that says nothing leaves the host no better off than a silent one. */
    @Test
    void counterFailsWithoutAnAdequateNote() {
        when(repository.findById(2)).thenReturn(proposedRequest());

        Result<Void> actual = service.counterProposal(2, CounterReason.PRICE, "too much", OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any());
    }

    @Test
    void counterFailsWithNoNoteAtAll() {
        when(repository.findById(2)).thenReturn(proposedRequest());

        Result<Void> actual = service.counterProposal(2, CounterReason.PRICE, null, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
    }

    /** Whitespace is not a note. The service trims before it measures. */
    @Test
    void counterFailsWhenTheNoteIsOnlyWhitespace() {
        when(repository.findById(2)).thenReturn(proposedRequest());

        Result<Void> actual = service.counterProposal(
                2, CounterReason.PRICE, "               ", OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
    }

    @Test
    void counterFailsWithoutAReason() {
        when(repository.findById(2)).thenReturn(proposedRequest());

        Result<Void> actual = service.counterProposal(
                2, null, "The dates do not work for us at all.", OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
    }

    @Test
    void counterIsForbiddenForAnotherClient() {
        when(repository.findById(2)).thenReturn(proposedRequest());

        Result<Void> actual = service.counterProposal(
                2, CounterReason.DATES, "That weekend is a wedding.", SOMEONE_ELSE);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
        verify(repository, never()).updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any());
    }

    /** A proposal already answered is not there to answer again. */
    @Test
    void counterFailsWhenThereIsNoLiveProposal() {
        when(repository.findById(2)).thenReturn(proposedRequest());
        when(bookingRepository.findLiveProposal(2)).thenReturn(null);

        Result<Void> actual = service.counterProposal(
                2, CounterReason.DATES, "That weekend is a wedding.", OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any());
    }

    /** A client with a proposal outstanding can still walk away from the trip entirely. */
    @Test
    void cancelWorksWhileAProposalIsOutstanding() {
        when(repository.findById(2)).thenReturn(proposedRequest());

        Result<Void> actual = service.cancel(2, OWNER);

        assertTrue(actual.isSuccess());
        verify(repository).updateStatus(2, RequestStatus.PROPOSED, RequestStatus.CANCELLED, null);
    }

    /** Editing under a live proposal is not allowed: it would invalidate what was proposed. */
    @Test
    void updateIsRefusedWhileAProposalIsOutstanding() {
        when(repository.findById(2)).thenReturn(proposedRequest());

        Result<TripRequest> actual = service.update(TestDataHelper.requestTwo(), OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).update(any(TripRequest.class));
    }

    // ---------- cancel ----------

    @Test
    void cancelHappyPath() {
        when(repository.findById(2)).thenReturn(TestDataHelper.requestTwo());

        Result<Void> actual = service.cancel(2, OWNER);

        assertTrue(actual.isSuccess());
        verify(repository).updateStatus(2, RequestStatus.PENDING, RequestStatus.CANCELLED, null);
    }

    /** The host booked it in the moment between the read above and the write. */
    @Test
    void cancelFailsWhenTheHostGotThereFirst() {
        when(repository.findById(2)).thenReturn(TestDataHelper.requestTwo());
        when(repository.updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any())).thenReturn(false);

        Result<Void> actual = service.cancel(2, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
    }

    @Test
    void cancelIsForbiddenForAnotherClient() {
        when(repository.findById(2)).thenReturn(TestDataHelper.requestTwo());

        Result<Void> actual = service.cancel(2, SOMEONE_ELSE);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
        verify(repository, never()).updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any());
    }

    /** "Cancel a request that hasn't been booked yet" -- once it is booked, it is too late. */
    @Test
    void cancelFailsWhenTheTripIsAlreadyBooked() {
        when(repository.findById(1)).thenReturn(TestDataHelper.requestOne());

        Result<Void> actual = service.cancel(1, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).updateStatus(anyInt(), any(RequestStatus.class),
                any(RequestStatus.class), any());
    }

    @Test
    void cancelIsNotFoundWhenTheRequestDoesNotExist() {
        when(repository.findById(999)).thenReturn(null);

        Result<Void> actual = service.cancel(999, OWNER);

        assertEquals(ResultType.NOT_FOUND, actual.getResultType());
    }

    // ---------- update (post-MVP) ----------

    @Test
    void updateHappyPath() {
        when(repository.findById(2)).thenReturn(TestDataHelper.requestTwo());
        when(repository.update(any(TripRequest.class))).thenReturn(true);

        TripRequest edited = TestDataHelper.requestToCreate();
        edited.setRequestId(2);

        Result<TripRequest> actual = service.update(edited, OWNER);

        assertTrue(actual.isSuccess());
        verify(repository).update(any(TripRequest.class));
    }

    @Test
    void updateIsForbiddenForAnotherClient() {
        when(repository.findById(2)).thenReturn(TestDataHelper.requestTwo());

        TripRequest edited = TestDataHelper.requestToCreate();
        edited.setRequestId(2);

        Result<TripRequest> actual = service.update(edited, SOMEONE_ELSE);

        assertEquals(ResultType.FORBIDDEN, actual.getResultType());
        verify(repository, never()).update(any(TripRequest.class));
    }

    @Test
    void updateFailsOnceTheTripIsBooked() {
        when(repository.findById(1)).thenReturn(TestDataHelper.requestOne());

        TripRequest edited = TestDataHelper.requestToCreate();
        edited.setRequestId(1);

        Result<TripRequest> actual = service.update(edited, OWNER);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).update(any(TripRequest.class));
    }

    /** An edit cannot quietly hand the request to somebody else. */
    @Test
    void updateKeepsTheOriginalOwner() {
        when(repository.findById(2)).thenReturn(TestDataHelper.requestTwo());
        when(repository.update(any(TripRequest.class))).thenReturn(true);

        TripRequest edited = TestDataHelper.requestToCreate();
        edited.setRequestId(2);
        edited.setClient(SOMEONE_ELSE);

        ArgumentCaptor<TripRequest> captor = ArgumentCaptor.forClass(TripRequest.class);
        service.update(edited, OWNER);

        verify(repository).update(captor.capture());
        assertEquals(OWNER.getUserId(), captor.getValue().getClient().getUserId());
    }
}
