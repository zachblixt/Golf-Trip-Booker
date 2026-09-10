package org.golftripbooker.data;

import org.golftripbooker.models.Destination;

import java.util.List;

public interface DestinationRepository {

    List<Destination> findAll() throws DataAccessException;

    Destination findById(int destinationId) throws DataAccessException;

    /** Case-insensitive, so booking "missoula" twice does not create two of them. */
    Destination findByName(String name) throws DataAccessException;

    /**
     * Seeded destinations are still never created by a client. This exists for one
     * caller: a host booking a trip to a place the app did not know about yet.
     */
    Destination create(Destination destination) throws DataAccessException;
}
