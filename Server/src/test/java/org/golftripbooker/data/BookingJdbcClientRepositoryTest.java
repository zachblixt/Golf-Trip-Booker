package org.golftripbooker.data;

import org.golftripbooker.models.Booking;
import org.golftripbooker.models.CounterReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BookingJdbcClientRepositoryTest {

    @Autowired
    private BookingJdbcClientRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void setup() {
        jdbcClient.sql("call set_known_good_state();").update();
    }

    @Test
    void findByRequestIdHappyPath() {
        assertEquals(TestDataHelper.bookingOne(), repository.findByRequestId(1));
    }

    @Test
    void findByRequestIdReturnsNullForAnUnbookedRequest() {
        assertNull(repository.findByRequestId(2));
    }

    @Test
    void findByIdHappyPath() {
        assertEquals(TestDataHelper.bookingOne(), repository.findById(1));
    }

    @Test
    void findByRequestIdsReturnsOnlyTheOnesThatExist() {
        List<Booking> actual = repository.findByRequestIds(List.of(1, 2, 3));

        assertEquals(1, actual.size());
        assertEquals(TestDataHelper.bookingOne(), actual.get(0));
    }

    /** An empty list would build `in ()`, which MySQL rejects, so it short circuits. */
    @Test
    void findByRequestIdsHandlesAnEmptyList() {
        assertTrue(repository.findByRequestIds(List.of()).isEmpty());
        assertTrue(repository.findByRequestIds(null).isEmpty());
    }

    @Test
    void create() {
        Booking actual = repository.create(TestDataHelper.bookingToCreate());

        assertEquals(TestDataHelper.bookingAfterCreate(), actual);
        assertEquals(TestDataHelper.bookingAfterCreate(), repository.findByRequestId(2));
    }

    /** Courses round-trip with their line breaks intact -- that is the whole format. */
    @Test
    void createKeepsEveryCourseLine() {
        Booking created = repository.create(TestDataHelper.bookingToCreate());

        List<String> courses = repository.findById(created.getBookingId()).courseList();

        assertEquals(List.of("Pacific Dunes", "Bandon Trails", "Old Macdonald"), courses);
    }

    /** A proposal is an unconfirmed row, so the confirmed-only reads must not see it. */
    @Test
    void confirmedReadsIgnoreAnOutstandingProposal() {
        repository.create(TestDataHelper.proposalOverBudget());

        assertNull(repository.findByRequestId(2));
        assertTrue(repository.findByRequestIds(List.of(2)).isEmpty());
    }

    /** A request may hold several proposals over time; the live one is the newest. */
    @Test
    void findLiveProposalReturnsTheNewestUnansweredRow() {
        repository.create(TestDataHelper.proposalOverBudget());

        Booking second = TestDataHelper.proposalOverBudget();
        second.setTotalCost(new BigDecimal("4700.00"));
        second.setBookedAt(TestDataHelper.proposalOverBudget().getBookedAt().plusDays(1));
        repository.create(second);

        assertEquals(new BigDecimal("4700.00"),
                repository.findLiveProposal(2).getTotalCost());
    }

    @Test
    void confirmTurnsAProposalIntoABooking() {
        Booking proposal = repository.create(TestDataHelper.proposalOverBudget());

        assertTrue(repository.confirm(proposal.getBookingId(),
                LocalDateTime.of(2026, 9, 1, 10, 0, 0)));
        assertNotNull(repository.findByRequestId(2));
    }

    /** Two accepts racing: the second loses rather than overwriting the first. */
    @Test
    void confirmRefusesARowThatIsAlreadyConfirmed() {
        Booking proposal = repository.create(TestDataHelper.proposalOverBudget());
        repository.confirm(proposal.getBookingId(), LocalDateTime.of(2026, 9, 1, 10, 0, 0));

        assertFalse(repository.confirm(proposal.getBookingId(),
                LocalDateTime.of(2026, 9, 2, 10, 0, 0)));
    }

    /**
     * The predicate that makes the loop safe.
     *
     * A countered proposal stays in the table as history, and history looks exactly like
     * a live proposal apart from this column. Without it the client could answer the same
     * proposal twice and the host would read the second answer as being about a trip they
     * had already been told was wrong.
     */
    @Test
    void findLiveProposalSkipsACounteredRow() {
        Booking proposal = repository.create(TestDataHelper.proposalOverBudget());
        repository.counter(proposal.getBookingId(), LocalDateTime.of(2026, 9, 1, 10, 0, 0),
                CounterReason.DATES, "That weekend is a wedding.");

        assertNull(repository.findLiveProposal(2));
    }

    /** Same argument for the host's side of it. */
    @Test
    void findLiveProposalSkipsAWithdrawnRow() {
        Booking proposal = repository.create(TestDataHelper.proposalOverBudget());
        repository.withdraw(proposal.getBookingId(), LocalDateTime.of(2026, 9, 1, 10, 0, 0),
                "The lodge released our block.");

        assertNull(repository.findLiveProposal(2));
    }

    /**
     * The client's side of a withdrawal. findLatestWithdrawnProposal is what their trip
     * screen reads to explain why a trip they were deciding on went back to pending.
     */
    @Test
    void findLatestWithdrawnProposalReturnsTheHostsNote() {
        Booking proposal = repository.create(TestDataHelper.proposalOverBudget());
        repository.withdraw(proposal.getBookingId(), LocalDateTime.of(2026, 9, 1, 10, 0, 0),
                "The lodge released our block.");

        Booking found = repository.findLatestWithdrawnProposal(2);

        assertNotNull(found);
        assertEquals("The lodge released our block.", found.getWithdrawNote());
        assertTrue(found.isWithdrawn());
    }

    /**
     * A proposal the CLIENT countered is not a withdrawal, and must not be dressed up as
     * one -- it would tell them their host pulled back a trip they turned down themselves.
     */
    @Test
    void findLatestWithdrawnProposalIgnoresACounteredRow() {
        Booking proposal = repository.create(TestDataHelper.proposalOverBudget());
        repository.counter(proposal.getBookingId(), LocalDateTime.of(2026, 9, 1, 10, 0, 0),
                CounterReason.PRICE, "Over what the group will go for.");

        assertNull(repository.findLatestWithdrawnProposal(2));
    }

    @Test
    void counterRecordsTheReasonAndTheNote() {
        Booking proposal = repository.create(TestDataHelper.proposalOverBudget());

        assertTrue(repository.counter(proposal.getBookingId(),
                LocalDateTime.of(2026, 9, 1, 10, 0, 0),
                CounterReason.COURSES, "We played Bandon Trails in May."));

        Booking stored = repository.findById(proposal.getBookingId());
        assertEquals(CounterReason.COURSES, stored.getCounterReason());
        assertEquals("We played Bandon Trails in May.", stored.getCounterNote());
        assertTrue(stored.isCountered());
        assertFalse(stored.isLive());
    }

    /** Confirm and counter are the same conditional seen from two sides. Only one wins. */
    @Test
    void counterLosesToAnAcceptThatLandedFirst() {
        Booking proposal = repository.create(TestDataHelper.proposalOverBudget());
        repository.confirm(proposal.getBookingId(), LocalDateTime.of(2026, 9, 1, 10, 0, 0));

        assertFalse(repository.counter(proposal.getBookingId(),
                LocalDateTime.of(2026, 9, 1, 10, 1, 0),
                CounterReason.PRICE, "Changed our minds about the money."));
    }

    @Test
    void acceptLosesToACounterThatLandedFirst() {
        Booking proposal = repository.create(TestDataHelper.proposalOverBudget());
        repository.counter(proposal.getBookingId(), LocalDateTime.of(2026, 9, 1, 10, 0, 0),
                CounterReason.PRICE, "Over what the group will go for.");

        assertFalse(repository.confirm(proposal.getBookingId(),
                LocalDateTime.of(2026, 9, 1, 10, 1, 0)));
    }

    /** The round number both detail screens show, counted rather than listed. */
    @Test
    void countProposalsCountsLiveAndDeadAlike() {
        assertEquals(0, repository.countProposals(2));

        Booking first = repository.create(TestDataHelper.proposalOverBudget());
        repository.counter(first.getBookingId(), LocalDateTime.of(2026, 9, 1, 10, 0, 0),
                CounterReason.PRICE, "Over what the group will go for.");
        repository.create(TestDataHelper.proposalOverBudget());

        assertEquals(2, repository.countProposals(2));
    }

    /** History is everything, live and dead, newest first. */
    @Test
    void findProposalHistoryReturnsEveryRowNewestFirst() {
        Booking first = repository.create(TestDataHelper.proposalOverBudget());
        repository.counter(first.getBookingId(), LocalDateTime.of(2026, 9, 1, 10, 0, 0),
                CounterReason.DATES, "That weekend is a wedding.");

        Booking second = TestDataHelper.proposalOverBudget();
        second.setTotalCost(new BigDecimal("4700.00"));
        second.setBookedAt(TestDataHelper.proposalOverBudget().getBookedAt().plusDays(1));
        repository.create(second);

        List<Booking> history = repository.findProposalHistory(2);

        assertEquals(2, history.size());
        assertEquals(new BigDecimal("4700.00"), history.get(0).getTotalCost());
        assertTrue(history.get(1).isCountered());
    }

    /**
     * The cardinality rule the generated column exists for: many unconfirmed rows on one
     * request, and never a second confirmed one.
     */
    @Test
    void aRequestMayHoldSeveralProposalsButOnlyOneBooking() {
        Booking first = repository.create(TestDataHelper.proposalOverBudget());
        repository.counter(first.getBookingId(), LocalDateTime.of(2026, 9, 1, 10, 0, 0),
                CounterReason.PRICE, "Over what the group will go for.");

        Booking second = repository.create(TestDataHelper.proposalOverBudget());

        assertEquals(2, repository.findProposalHistory(2).size());
        assertTrue(repository.confirm(second.getBookingId(),
                LocalDateTime.of(2026, 9, 2, 10, 0, 0)));
        assertNotNull(repository.findByRequestId(2));
    }

    @Test
    void update() {
        Booking toUpdate = TestDataHelper.bookingOne();
        toUpdate.setLodging("The Holly Inn");
        toUpdate.setTotalCost(new BigDecimal("4700.00"));

        assertTrue(repository.update(toUpdate));

        Booking actual = repository.findById(1);
        assertEquals("The Holly Inn", actual.getLodging());
        assertEquals(new BigDecimal("4700.00"), actual.getTotalCost());
    }

    /** A correction is not a re-assignment: the booking stays on its own request. */
    @Test
    void updateCannotMoveABookingToAnotherRequest() {
        Booking toUpdate = TestDataHelper.bookingOne();
        toUpdate.setRequestId(2);

        repository.update(toUpdate);

        assertEquals(1, repository.findById(1).getRequestId());
        assertNull(repository.findByRequestId(2));
    }

    @Test
    void updateFailsWhenTheBookingIsMissing() {
        Booking missing = TestDataHelper.bookingOne();
        missing.setBookingId(999);

        assertFalse(repository.update(missing));
    }
}
