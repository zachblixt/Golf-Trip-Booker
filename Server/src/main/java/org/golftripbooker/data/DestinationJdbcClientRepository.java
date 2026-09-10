package org.golftripbooker.data;

import org.golftripbooker.data.mappers.DestinationMapper;
import org.golftripbooker.models.Destination;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Destinations are seeded, and a client can never create one -- which is what keeps
 * a client from inventing a place the host cannot book. The single exception is a
 * host booking a request for somewhere new, which promotes that place into a real
 * destination so the next person finds it in the list.
 */
@Repository
public class DestinationJdbcClientRepository implements DestinationRepository {

    private static final String BASE_SELECT =
            "select destination_id, name, region, description, latitude, longitude from destination";

    private final JdbcClient jdbcClient;

    public DestinationJdbcClientRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Destination> findAll() throws DataAccessException {
        final String sql = BASE_SELECT + " order by name;";
        return jdbcClient.sql(sql).query(new DestinationMapper()).list();
    }

    @Override
    public Destination findById(int destinationId) throws DataAccessException {
        final String sql = BASE_SELECT + " where destination_id = ?;";
        return jdbcClient.sql(sql)
                .param(destinationId)
                .query(new DestinationMapper())
                .optional().orElse(null);
    }

    @Override
    public Destination findByName(String name) throws DataAccessException {
        // MySQL's default collation is already case-insensitive, but lower() on both
        // sides says so out loud rather than relying on the server's configuration.
        final String sql = BASE_SELECT + " where lower(name) = lower(?);";
        return jdbcClient.sql(sql)
                .param(name)
                .query(new DestinationMapper())
                .optional().orElse(null);
    }

    @Override
    public Destination create(Destination destination) throws DataAccessException {
        final String sql = """
                insert into destination (name, region, description, latitude, longitude)
                values (:name, :region, :description, :latitude, :longitude);
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int rowsAffected = jdbcClient.sql(sql)
                .param("name", destination.getName())
                .param("region", destination.getRegion())
                .param("description", destination.getDescription())
                .param("latitude", destination.getLatitude())
                .param("longitude", destination.getLongitude())
                .update(keyHolder, "destination_id");

        if (rowsAffected == 0) {
            return null;
        }

        destination.setDestinationId(keyHolder.getKey().intValue());
        return destination;
    }
}
