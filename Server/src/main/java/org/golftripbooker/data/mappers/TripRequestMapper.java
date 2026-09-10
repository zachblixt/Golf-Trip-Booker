package org.golftripbooker.data.mappers;

import org.golftripbooker.models.Destination;
import org.golftripbooker.models.RequestStatus;
import org.golftripbooker.models.Role;
import org.golftripbooker.models.TripRequest;
import org.golftripbooker.models.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Reads the joined client and destination alongside the request. The client's
 * password is deliberately never selected, so a TripRequest read back from the
 * database always carries a null there.
 */
public class TripRequestMapper implements RowMapper<TripRequest> {

    @Override
    public TripRequest mapRow(ResultSet rs, int rowNum) throws SQLException {

        User client = new User(
                rs.getInt("client_id"),
                rs.getString("client_email"),
                rs.getString("client_username"),
                null,
                Role.valueOf(rs.getString("client_role"))
        );

        /*
         * The join is a LEFT JOIN now, because a request may name a place that is not
         * a destination yet. getInt returns 0 for a null column, so the id is what
         * says whether a destination is really there.
         */
        int destinationId = rs.getInt("destination_id");
        Destination destination = destinationId == 0 ? null : new Destination(
                destinationId,
                rs.getString("destination_name"),
                rs.getString("destination_region"),
                rs.getString("destination_description"),
                rs.getBigDecimal("destination_latitude"),
                rs.getBigDecimal("destination_longitude")
        );

        // The window is optional, so getDate can hand back null for either half.
        Date earliestStart = rs.getDate("earliest_start");
        Date latestStart = rs.getDate("latest_start");

        return new TripRequest(
                rs.getInt("request_id"),
                client,
                destination,
                rs.getString("requested_place"),
                rs.getBigDecimal("requested_latitude"),
                rs.getBigDecimal("requested_longitude"),
                rs.getBigDecimal("budget_per_player"),
                rs.getInt("player_count"),
                rs.getInt("rounds_requested"),
                rs.getInt("nights"),
                earliestStart == null ? null : earliestStart.toLocalDate(),
                latestStart == null ? null : latestStart.toLocalDate(),
                rs.getString("notes"),
                RequestStatus.valueOf(rs.getString("status")),
                rs.getString("decline_reason"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
