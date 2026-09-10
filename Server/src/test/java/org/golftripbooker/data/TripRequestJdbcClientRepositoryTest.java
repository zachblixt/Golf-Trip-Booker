package org.golftripbooker.data;

import org.golftripbooker.models.RequestStatus;
import org.golftripbooker.models.TripRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class TripRequestJdbcClientRepositoryTest {

    @Autowired
    private TripRequestJdbcClientRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void setup() {
        jdbcClient.sql("call set_known_good_state();").update();
    }

    // ---------- reads ----------

    @Test
    void findByClientIdReturnsOnlyThatClientsRequestsNewestFirst() {
        List<TripRequest> actual = repository.findByClientId(1);

        assertEquals(2, actual.size());
        assertEquals(TestDataHelper.requestTwo(), actual.get(0));
        assertEquals(TestDataHelper.requestOne(), actual.get(1));
        assertTrue(actual.stream().allMatch(r -> r.getClient().getUserId() == 1));
    }

    @Test
    void findByClientIdReturnsEmptyForAClientWithNothing() {
        assertTrue(repository.findByClientId(3).isEmpty());
    }

    @Test
    void findByIdHappyPath() {
        assertEquals(TestDataHelper.requestTwo(), repository.findById(2));
    }

    @Test
    void findByIdFailsToFind() {
        assertNull(repository.findById(999));
    }

    /** The client is joined in but their password never is. */
    @Test
    void findByIdNeverReturnsTheClientsPasswordHash() {
        assertNull(repository.findById(1).getClient().getPassword());
    }

    @Test
    void findByStatusReturnsThePendingQueueOldestFirst() {
        List<TripRequest> actual = repository.findByStatus(RequestStatus.PENDING);

        assertEquals(3, actual.size());
        assertEquals(TestDataHelper.requestTwo(), actual.get(0));
        assertEquals(TestDataHelper.requestThree(), actual.get(1));
        assertEquals(TestDataHelper.requestFourUnseeded(), actual.get(2));
    }

    /** The left join: a request may name a place that is not a destination yet. */
    @Test
    void findByIdReadsARequestWithNoDestination() {
        TripRequest actual = repository.findById(4);

        assertNull(actual.getDestination());
        assertEquals("Missoula, Montana", actual.getRequestedPlace());
        assertTrue(actual.isUnseededPlace());
        assertEquals("Missoula, Montana", actual.placeLabel());
    }

    /** Promotion: what a host booking an unseeded request triggers. */
    @Test
    void attachDestinationTurnsAPlaceIntoADestination() {
        assertTrue(repository.attachDestination(4, 1));

        TripRequest actual = repository.findById(4);
        assertEquals(TestDataHelper.pinehurst(), actual.getDestination());
        // The text survives -- it records that this began as a request for somewhere new.
        assertEquals("Missoula, Montana", actual.getRequestedPlace());
    }

    /** Explore only shows trips with a real destination, so an unbooked place is absent. */
    @Test
    void findBookedSkipsRequestsWithNoDestination() {
        assertTrue(repository.findBooked(null, null).stream()
                .allMatch(r -> r.getDestination() != null));
    }

    @Test
    void findByStatusReturnsEmptyWhenNothingMatches() {
        assertTrue(repository.findByStatus(RequestStatus.CANCELLED).isEmpty());
    }

    // ---------- Explore filters ----------

    @Test
    void findBookedWithNoFiltersReturnsEveryBookedTrip() {
        List<TripRequest> actual = repository.findBooked(null, null);

        assertEquals(1, actual.size());
        assertEquals(TestDataHelper.requestOne(), actual.get(0));
    }

    /** $4,650 across 4 players is $1,162.50 each, so a $1,200 ceiling includes it. */
    @Test
    void findBookedFiltersOnWhatItActuallyCostPerPlayer() {
        assertEquals(1, repository.findBooked(new BigDecimal("1200.00"), null).size());
        assertTrue(repository.findBooked(new BigDecimal("1000.00"), null).isEmpty());
    }

    @Test
    void findBookedFiltersOnDestination() {
        assertEquals(1, repository.findBooked(null, 1).size());
        assertTrue(repository.findBooked(null, 2).isEmpty());
    }

    // ---------- writes ----------

    @Test
    void create() {
        TripRequest actual = repository.create(TestDataHelper.requestToCreate());

        assertEquals(TestDataHelper.requestAfterCreate(), actual);
        assertEquals(TestDataHelper.requestAfterCreate(), repository.findById(5));
    }

    /** A brand new request is always PENDING, whatever the caller set. */
    @Test
    void createAlwaysLandsAsPending() {
        TripRequest toCreate = TestDataHelper.requestToCreate();
        toCreate.setStatus(RequestStatus.BOOKED);

        TripRequest actual = repository.create(toCreate);

        assertEquals(RequestStatus.PENDING, repository.findById(actual.getRequestId()).getStatus());
    }

    @Test
    void update() {
        TripRequest toUpdate = TestDataHelper.requestTwo();
        toUpdate.setPlayerCount(4);
        toUpdate.setNotes("Two more people joined");

        assertTrue(repository.update(toUpdate));

        TripRequest actual = repository.findById(2);
        assertEquals(4, actual.getPlayerCount());
        assertEquals("Two more people joined", actual.getNotes());
    }

    @Test
    void updateFailsWhenTheRequestIsMissing() {
        TripRequest missing = TestDataHelper.requestTwo();
        missing.setRequestId(999);

        assertFalse(repository.update(missing));
    }

    @Test
    void updateStatusToBooked() {
        assertTrue(repository.updateStatus(2, RequestStatus.PENDING, RequestStatus.BOOKED, null));
        assertEquals(RequestStatus.BOOKED, repository.findById(2).getStatus());
    }

    @Test
    void updateStatusToDeclinedKeepsTheReason() {
        assertTrue(repository.updateStatus(2, RequestStatus.PENDING, RequestStatus.DECLINED,
                "No tee times that week."));

        TripRequest actual = repository.findById(2);
        assertEquals(RequestStatus.DECLINED, actual.getStatus());
        assertEquals("No tee times that week.", actual.getDeclineReason());
    }

    /** A reason only makes sense on a decline, so any other status stores null. */
    @Test
    void updateStatusDropsTheReasonForEveryStatusButDeclined() {
        repository.updateStatus(2, RequestStatus.PENDING, RequestStatus.CANCELLED,
                "should not be stored");

        assertNull(repository.findById(2).getDeclineReason());
    }

    @Test
    void updateStatusFailsWhenTheRequestIsMissing() {
        assertFalse(repository.updateStatus(999, RequestStatus.PENDING, RequestStatus.BOOKED, null));
    }

    /** The guard itself: the second caller loses and the row does not move. */
    @Test
    void updateStatusRefusesARequestThatHasAlreadyMoved() {
        assertTrue(repository.updateStatus(2, RequestStatus.PENDING, RequestStatus.BOOKED, null));
        assertFalse(repository.updateStatus(2, RequestStatus.PENDING, RequestStatus.CANCELLED, null));
        assertEquals(RequestStatus.BOOKED, repository.findById(2).getStatus());
    }
}
