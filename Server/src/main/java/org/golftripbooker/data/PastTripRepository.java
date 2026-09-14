package org.golftripbooker.data;

import java.util.List;

/**
 * Deliberately separate from BookingRepository. This exists only to feed the proposal
 * drafter, reads across a join rather than from the booking table alone, and should be
 * removable in one piece if the drafting feature ever is.
 */
public interface PastTripRepository {

    /**
     * Confirmed trips at a destination, newest first. Proposals are excluded: a trip
     * nobody agreed to is not evidence of what this place costs.
     */
    List<PastTrip> findByDestination(int destinationId, int limit) throws DataAccessException;
}
