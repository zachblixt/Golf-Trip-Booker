package org.golftripbooker.data.mappers;

import org.golftripbooker.models.Booking;
import org.golftripbooker.models.CounterReason;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BookingMapper implements RowMapper<Booking> {
    @Override
    public Booking mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Booking(
                rs.getInt("booking_id"),
                rs.getInt("request_id"),
                rs.getString("courses"),
                rs.getString("lodging"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getBigDecimal("total_cost"),
                rs.getString("itinerary"),
                rs.getInt("booked_by"),
                rs.getTimestamp("booked_at").toLocalDateTime(),
                // Null on a live proposal, so getTimestamp can hand back null.
                rs.getTimestamp("confirmed_at") == null
                        ? null : rs.getTimestamp("confirmed_at").toLocalDateTime(),
                rs.getTimestamp("countered_at") == null
                        ? null : rs.getTimestamp("countered_at").toLocalDateTime(),
                rs.getString("counter_reason") == null
                        ? null : CounterReason.valueOf(rs.getString("counter_reason")),
                rs.getString("counter_note"),
                rs.getTimestamp("withdrawn_at") == null
                        ? null : rs.getTimestamp("withdrawn_at").toLocalDateTime(),
                rs.getString("withdraw_note")
        );
    }
}
