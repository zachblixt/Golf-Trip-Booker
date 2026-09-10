package org.golftripbooker.data;

import org.golftripbooker.data.mappers.BookingMapper;
import org.golftripbooker.models.Booking;
import org.golftripbooker.models.CounterReason;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class BookingJdbcClientRepository implements BookingRepository {

    private static final String BASE_SELECT = """
            select booking_id, request_id, courses, lodging, start_date, end_date,
                   total_cost, itinerary, booked_by, booked_at, confirmed_at,
                   countered_at, counter_reason, counter_note, withdrawn_at, withdraw_note
            from booking
            """;

    private final JdbcClient jdbcClient;

    public BookingJdbcClientRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Confirmed only. Without that predicate a request that collected a proposal, had it
     * countered, and was booked later would have several rows here and `optional()`
     * would throw on the second one.
     */
    @Override
    public Booking findByRequestId(int requestId) throws DataAccessException {
        final String sql = BASE_SELECT + " where request_id = ? and confirmed_at is not null;";
        return jdbcClient.sql(sql)
                .param(requestId)
                .query(new BookingMapper())
                .optional().orElse(null);
    }

    /**
     * The live proposal: newest row nobody has answered. All three predicates matter --
     * without `countered_at is null` a proposal the client already refused would come
     * back as if it were still waiting on them, and they could refuse it twice.
     *
     * booking_id breaks a tie, because two proposals made inside the same second would
     * otherwise be ordered arbitrarily.
     */
    @Override
    public Booking findLiveProposal(int requestId) throws DataAccessException {
        final String sql = BASE_SELECT
                + " where request_id = ? and confirmed_at is null"
                + " and countered_at is null and withdrawn_at is null"
                + " order by booked_at desc, booking_id desc limit 1;";
        return jdbcClient.sql(sql)
                .param(requestId)
                .query(new BookingMapper())
                .optional().orElse(null);
    }

    @Override
    public int countProposals(int requestId) throws DataAccessException {
        final String sql = "select count(*) from booking where request_id = ?;";
        return jdbcClient.sql(sql).param(requestId).query(Integer.class).single();
    }

    /** Everything, live and dead, newest first. The host reads this as a conversation. */
    @Override
    public List<Booking> findProposalHistory(int requestId) throws DataAccessException {
        final String sql = BASE_SELECT
                + " where request_id = ? order by booked_at desc, booking_id desc;";
        return jdbcClient.sql(sql)
                .param(requestId)
                .query(new BookingMapper())
                .list();
    }

    @Override
    public Booking findById(int bookingId) throws DataAccessException {
        final String sql = BASE_SELECT + " where booking_id = ?;";
        return jdbcClient.sql(sql)
                .param(bookingId)
                .query(new BookingMapper())
                .optional().orElse(null);
    }

    /**
     * An empty id list would produce `in ()`, which is a syntax error, so it short
     * circuits instead of building the query.
     */
    @Override
    public List<Booking> findByRequestIds(List<Integer> requestIds) throws DataAccessException {
        if (requestIds == null || requestIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(", ", requestIds.stream().map(id -> "?").toList());
        final String sql = BASE_SELECT
                + " where request_id in (" + placeholders + ") and confirmed_at is not null;";

        return jdbcClient.sql(sql)
                .params(requestIds)
                .query(new BookingMapper())
                .list();
    }

    @Override
    public Booking create(Booking booking) throws DataAccessException {
        final String sql = """
                insert into booking
                    (request_id, courses, lodging, start_date, end_date,
                     total_cost, itinerary, booked_by, booked_at, confirmed_at)
                values
                    (:request_id, :courses, :lodging, :start_date, :end_date,
                     :total_cost, :itinerary, :booked_by, :booked_at, :confirmed_at);
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int rowsAffected = jdbcClient.sql(sql)
                .param("request_id", booking.getRequestId())
                .param("courses", booking.getCourses())
                .param("lodging", booking.getLodging())
                .param("start_date", booking.getStartDate())
                .param("end_date", booking.getEndDate())
                .param("total_cost", booking.getTotalCost())
                .param("itinerary", booking.getItinerary())
                .param("booked_by", booking.getBookedBy())
                .param("booked_at", booking.getBookedAt())
                // Always null on insert now: propose() is the only caller, and a
                // proposal is by definition something nobody has agreed to yet.
                .param("confirmed_at", booking.getConfirmedAt())
                .update(keyHolder, "booking_id");

        if (rowsAffected == 0) {
            return null;
        }

        booking.setBookingId(keyHolder.getKey().intValue());
        return booking;
    }

    /** request_id and booked_by are not editable: a correction is not a re-assignment. */
    /*
     * The three writes below are the same conditional update seen from three sides. A
     * proposal has exactly one outcome, so each of them insists the other two have not
     * happened; whoever gets there first wins and everybody else is told, rather than
     * silently overwriting the winner.
     */
    private static final String STILL_LIVE =
            " and confirmed_at is null and countered_at is null and withdrawn_at is null;";

    @Override
    public boolean confirm(int bookingId, LocalDateTime confirmedAt) throws DataAccessException {
        final String sql = "update booking set confirmed_at = :confirmed_at"
                + " where booking_id = :booking_id" + STILL_LIVE;

        return jdbcClient.sql(sql)
                .param("confirmed_at", confirmedAt)
                .param("booking_id", bookingId)
                .update() > 0;
    }

    @Override
    public boolean counter(int bookingId, LocalDateTime counteredAt,
                           CounterReason reason, String note) throws DataAccessException {
        final String sql = """
                update booking set
                    countered_at = :countered_at,
                    counter_reason = :counter_reason,
                    counter_note = :counter_note
                where booking_id = :booking_id"""
                + STILL_LIVE;

        return jdbcClient.sql(sql)
                .param("countered_at", counteredAt)
                // The enum goes in by name; the column is an enum of the same five values.
                .param("counter_reason", reason == null ? null : reason.name())
                .param("counter_note", note)
                .param("booking_id", bookingId)
                .update() > 0;
    }

    @Override
    public boolean withdraw(int bookingId, LocalDateTime withdrawnAt, String note)
            throws DataAccessException {

        final String sql = """
                update booking set
                    withdrawn_at = :withdrawn_at,
                    withdraw_note = :withdraw_note
                where booking_id = :booking_id"""
                + STILL_LIVE;

        return jdbcClient.sql(sql)
                .param("withdrawn_at", withdrawnAt)
                .param("withdraw_note", note)
                .param("booking_id", bookingId)
                .update() > 0;
    }

    /** Withdrawn only, newest first. A row the client countered is not their business. */
    @Override
    public Booking findLatestWithdrawnProposal(int requestId) throws DataAccessException {
        final String sql = BASE_SELECT
                + " where request_id = ? and withdrawn_at is not null"
                + " order by withdrawn_at desc, booking_id desc limit 1;";
        return jdbcClient.sql(sql)
                .param(requestId)
                .query(new BookingMapper())
                .optional().orElse(null);
    }

    @Override
    public boolean update(Booking booking) throws DataAccessException {
        final String sql = """
                update booking set
                    courses = :courses,
                    lodging = :lodging,
                    start_date = :start_date,
                    end_date = :end_date,
                    total_cost = :total_cost,
                    itinerary = :itinerary
                where booking_id = :booking_id;
                """;

        return jdbcClient.sql(sql)
                .param("courses", booking.getCourses())
                .param("lodging", booking.getLodging())
                .param("start_date", booking.getStartDate())
                .param("end_date", booking.getEndDate())
                .param("total_cost", booking.getTotalCost())
                .param("itinerary", booking.getItinerary())
                .param("booking_id", booking.getBookingId())
                .update() > 0;
    }
}
