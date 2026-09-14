package org.golftripbooker.data;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PastTripJdbcClientRepository implements PastTripRepository {

    private final JdbcClient jdbcClient;

    public PastTripJdbcClientRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<PastTrip> findByDestination(int destinationId, int limit) throws DataAccessException {
        /*
         * player_count, nights and rounds_requested live on the request rather than the
         * booking, so this is a join. Without them a total cost means nothing: $3,200 is
         * cheap for eight players and steep for two.
         */
        final String sql = """
                select b.courses, b.lodging, r.player_count, r.nights,
                       r.rounds_requested, b.total_cost, b.start_date, b.itinerary
                from booking b
                join trip_request r on r.request_id = b.request_id
                where r.destination_id = ?
                  and b.confirmed_at is not null
                order by b.start_date desc
                limit ?;
                """;

        return jdbcClient.sql(sql)
                .params(destinationId, limit)
                .query((rs, rowNum) -> new PastTrip(
                        rs.getString("courses"),
                        rs.getString("lodging"),
                        rs.getInt("player_count"),
                        rs.getInt("nights"),
                        rs.getInt("rounds_requested"),
                        rs.getBigDecimal("total_cost"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getString("itinerary")))
                .list();
    }
}
