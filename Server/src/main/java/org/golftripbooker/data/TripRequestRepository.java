package org.golftripbooker.data;

import org.golftripbooker.models.RequestStatus;
import org.golftripbooker.models.TripRequest;

import java.math.BigDecimal;
import java.util.List;

public interface TripRequestRepository {

    /** Every request belonging to one client, newest first. */
    List<TripRequest> findByClientId(int clientId) throws DataAccessException;

    TripRequest findById(int requestId) throws DataAccessException;

    /** The host queue. Oldest first, so the person who waited longest is on top. */
    List<TripRequest> findByStatus(RequestStatus status) throws DataAccessException;

    /**
     * Post-MVP Explore: every BOOKED trip, optionally filtered by what it actually
     * cost per player and by destination. Both filters are optional -- pass null to skip.
     */
    List<TripRequest> findBooked(BigDecimal maxCostPerPlayer, Integer destinationId)
            throws DataAccessException;

    TripRequest create(TripRequest request) throws DataAccessException;

    /** Only the fields a client may edit while the request is still PENDING. */
    boolean update(TripRequest request) throws DataAccessException;

    /** Promotes a free-text request to a real destination once a host books it. */
    boolean attachDestination(int requestId, int destinationId) throws DataAccessException;

    /**
     * The one way status changes, and it is conditional: the row moves only if it is
     * still in `from`. A false return means somebody else moved it first, not that the
     * request is missing. declineReason is null for every status but DECLINED.
     */
    boolean updateStatus(int requestId, RequestStatus from, RequestStatus to, String declineReason)
            throws DataAccessException;
}
