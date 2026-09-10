package org.golftripbooker.data;

import org.golftripbooker.data.mappers.TripRequestMapper;
import org.golftripbooker.models.RequestStatus;
import org.golftripbooker.models.TripRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TripRequestJdbcClientRepository implements TripRequestRepository {

    /*
     * The client and destination are aliased rather than selected bare because
     * both tables have a `name`-ish column and both have an id. TripRequestMapper
     * reads these exact aliases.
     */
    private static final String BASE_SELECT = """
            select tr.request_id, tr.budget_per_player, tr.player_count, tr.rounds_requested,
                   tr.nights, tr.earliest_start, tr.latest_start, tr.notes, tr.status, tr.decline_reason,
                   tr.created_at, tr.requested_place,
                   tr.requested_latitude, tr.requested_longitude,
                   c.user_id as client_id, c.email as client_email,
                   c.username as client_username, c.role as client_role,
                   d.destination_id, d.name as destination_name,
                   d.region as destination_region, d.description as destination_description,
                   d.latitude as destination_latitude, d.longitude as destination_longitude
            from trip_request tr
            inner join app_user c on tr.user_id = c.user_id
            left join destination d on tr.destination_id = d.destination_id
            """;

    private final JdbcClient jdbcClient;

    public TripRequestJdbcClientRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<TripRequest> findByClientId(int clientId) throws DataAccessException {
        final String sql = BASE_SELECT + " where tr.user_id = ? order by tr.created_at desc;";
        return jdbcClient.sql(sql)
                .param(clientId)
                .query(new TripRequestMapper())
                .list();
    }

    @Override
    public TripRequest findById(int requestId) throws DataAccessException {
        final String sql = BASE_SELECT + " where tr.request_id = ?;";
        return jdbcClient.sql(sql)
                .param(requestId)
                .query(new TripRequestMapper())
                .optional().orElse(null);
    }

    @Override
    public List<TripRequest> findByStatus(RequestStatus status) throws DataAccessException {
        final String sql = BASE_SELECT + " where tr.status = ? order by tr.created_at;";
        return jdbcClient.sql(sql)
                .param(status.name())
                .query(new TripRequestMapper())
                .list();
    }

    /**
     * Filters are appended only when supplied, so "any budget, anywhere" runs the
     * same query with no predicates rather than a sentinel value.
     */
    @Override
    public List<TripRequest> findBooked(BigDecimal maxCostPerPlayer, Integer destinationId)
            throws DataAccessException {

        /*
         * `d.destination_id is not null` is belt and braces: booking a free-text
         * request promotes its place to a real destination first, so a BOOKED request
         * always has one. If that ever stopped being true, Explore would skip the row
         * rather than render a card with no name.
         */
        StringBuilder sql = new StringBuilder(BASE_SELECT)
                /*
                 * `b.confirmed_at is not null` is not optional. A request may hold several
                 * booking rows -- offers the client turned down, plus the one they took --
                 * and without this an inner join would put the same trip on Explore once
                 * per offer it collected on the way.
                 */
                .append(" inner join booking b on b.request_id = tr.request_id")
                .append("  and b.confirmed_at is not null")
                .append(" where tr.status = 'BOOKED' and d.destination_id is not null");

        List<Object> params = new ArrayList<>();

        if (maxCostPerPlayer != null) {
            sql.append(" and (b.total_cost / tr.player_count) <= ?");
            params.add(maxCostPerPlayer);
        }

        if (destinationId != null) {
            sql.append(" and tr.destination_id = ?");
            params.add(destinationId);
        }

        sql.append(" order by b.start_date desc;");

        return jdbcClient.sql(sql.toString())
                .params(params)
                .query(new TripRequestMapper())
                .list();
    }

    /**
     * user_id comes off request.getClient(), which the service always overwrites with
     * the JWT's user. status is hard-coded PENDING -- a new request cannot arrive BOOKED.
     */
    @Override
    public TripRequest create(TripRequest request) throws DataAccessException {
        final String sql = """
                insert into trip_request
                    (user_id, destination_id, requested_place, requested_latitude,
                     requested_longitude, budget_per_player, player_count,
                     rounds_requested, nights, earliest_start, latest_start, notes, status, created_at)
                values
                    (:user_id, :destination_id, :requested_place, :requested_latitude,
                     :requested_longitude, :budget_per_player, :player_count,
                     :rounds_requested, :nights, :earliest_start, :latest_start, :notes, 'PENDING',
                     :created_at);
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int rowsAffected = jdbcClient.sql(sql)
                .param("user_id", request.getClient().getUserId())
                .param("destination_id", request.getDestination() == null
                        ? null : request.getDestination().getDestinationId())
                .param("requested_place", request.getRequestedPlace())
                .param("requested_latitude", request.getRequestedLatitude())
                .param("requested_longitude", request.getRequestedLongitude())
                .param("budget_per_player", request.getBudgetPerPlayer())
                .param("player_count", request.getPlayerCount())
                .param("rounds_requested", request.getRoundsRequested())
                .param("nights", request.getNights())
                .param("earliest_start", request.getEarliestStart())
                .param("latest_start", request.getLatestStart())
                .param("notes", request.getNotes())
                .param("created_at", request.getCreatedAt())
                .update(keyHolder, "request_id");

        if (rowsAffected == 0) {
            return null;
        }

        request.setRequestId(keyHolder.getKey().intValue());
        request.setStatus(RequestStatus.PENDING);
        return request;
    }

    /** Note what is absent: user_id, status, and created_at are not editable. */
    @Override
    public boolean update(TripRequest request) throws DataAccessException {
        final String sql = """
                update trip_request set
                    destination_id = :destination_id,
                    requested_place = :requested_place,
                    requested_latitude = :requested_latitude,
                    requested_longitude = :requested_longitude,
                    budget_per_player = :budget_per_player,
                    player_count = :player_count,
                    rounds_requested = :rounds_requested,
                    nights = :nights,
                    earliest_start = :earliest_start,
                    latest_start = :latest_start,
                    notes = :notes
                where request_id = :request_id;
                """;

        return jdbcClient.sql(sql)
                .param("destination_id", request.getDestination() == null
                        ? null : request.getDestination().getDestinationId())
                .param("requested_place", request.getRequestedPlace())
                .param("requested_latitude", request.getRequestedLatitude())
                .param("requested_longitude", request.getRequestedLongitude())
                .param("budget_per_player", request.getBudgetPerPlayer())
                .param("player_count", request.getPlayerCount())
                .param("rounds_requested", request.getRoundsRequested())
                .param("nights", request.getNights())
                .param("earliest_start", request.getEarliestStart())
                .param("latest_start", request.getLatestStart())
                .param("notes", request.getNotes())
                .param("request_id", request.getRequestId())
                .update() > 0;
    }

    /**
     * Attaches a newly created destination to a request that only named a place.
     * The text is kept rather than cleared -- it records that this trip started as a
     * request for somewhere the app did not know about, which is the demand signal
     * worth being able to count later.
     */
    @Override
    public boolean attachDestination(int requestId, int destinationId) throws DataAccessException {
        final String sql = "update trip_request set destination_id = ? where request_id = ?;";
        return jdbcClient.sql(sql).param(destinationId).param(requestId).update() > 0;
    }

    /**
     * Conditional on purpose. Two hosts working the queue, or a host booking while the
     * client cancels, both read PENDING and both pass their own in-memory guard -- the
     * database is the only place that can settle it. `and status = :from_status` makes
     * the check and the write one atomic step, and a row count of zero is how the loser
     * finds out.
     */
    @Override
    public boolean updateStatus(int requestId, RequestStatus from, RequestStatus to,
                                String declineReason) throws DataAccessException {

        final String sql = """
                update trip_request set
                    status = :to_status,
                    decline_reason = :decline_reason
                where request_id = :request_id
                  and status = :from_status;
                """;

        return jdbcClient.sql(sql)
                .param("to_status", to.name())
                .param("decline_reason", to == RequestStatus.DECLINED ? declineReason : null)
                .param("request_id", requestId)
                .param("from_status", from.name())
                .update() > 0;
    }
}
