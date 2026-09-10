package org.golftripbooker.data.mappers;

import org.golftripbooker.models.Destination;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DestinationMapper implements RowMapper<Destination> {
    @Override
    public Destination mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Destination(
                rs.getInt("destination_id"),
                rs.getString("name"),
                rs.getString("region"),
                rs.getString("description"),
                // getBigDecimal returns null for a null column, which is what we want.
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude")
        );
    }
}
