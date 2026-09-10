package org.golftripbooker.models;

/**
 * A request starts PENDING. BOOKED, DECLINED and CANCELLED are terminal, but PROPOSED
 * is not: a trip the host has arranged and the client has not answered yet can go
 * forward to BOOKED or back to PENDING, which is the one loop in this machine.
 *
 * BOOKED is reachable only by the client accepting. An earlier version let a host book
 * outright whenever the total came in under budget, which quietly treated "affordable"
 * as "acceptable" -- but the host also picks the courses, the lodging and the exact
 * dates inside the window, and no ceiling can check any of those.
 */
public enum RequestStatus {
    PENDING,
    /** A host has arranged a trip and is waiting on the client. */
    PROPOSED,
    BOOKED,
    DECLINED,
    CANCELLED
}
