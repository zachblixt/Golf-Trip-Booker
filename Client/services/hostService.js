import { request } from './api';

/*
 * Every path here is under /api/host, which SecurityConfig gates with
 * hasRole("HOST"). A client hitting any of these gets 403 before it reaches a
 * controller -- hiding the tabs is not what protects this.
 */

/** The queue, oldest first, each carrying the client who asked. */
export function findPending() {
    return request('/host/request');
}

/**
 * Requests in any status. `BOOKED` is what the Booked tab reads, and those
 * responses carry the booking -- which is how a correction finds its booking id.
 * The anonymised Explore list deliberately does not, so it cannot be the way in.
 */
export function findByStatus(status) {
    return request(`/host/request?status=${encodeURIComponent(status)}`);
}

/** One request plus its budget ceiling, for the booking form. */
export function findRequest(requestId) {
    return request(`/host/request/${requestId}`);
}

/**
 * 201. Sends the trip to the client as a proposal and flips the request to PROPOSED in
 * one transaction -- there is no state where a proposal exists but the request still
 * says pending.
 *
 * This is the only way a host commits anything. Booking is the client's to do.
 */
export function propose(requestId, form) {
    return request(`/host/request/${requestId}/proposal`, { method: 'POST', body: form });
}

/** 204. The reason is required and comes back to the client on their trip detail. */
export function decline(requestId, reason) {
    return request(`/host/request/${requestId}/decline`, { method: 'POST', body: { reason } });
}

/**
 * 204. Pull a proposal back, with a note. The request returns to the queue as pending.
 *
 * The note is required and has the same floor as the one a client owes when they
 * counter. A POST, not a DELETE: nothing is deleted, and the note needs a body.
 */
export function withdrawProposal(requestId, note) {
    return request(`/host/request/${requestId}/withdrawal`, { method: 'POST', body: { note } });
}

/**
 * Every proposal ever made on a request, newest first, each carrying how it ended.
 * This is what turns a request that keeps coming back into a readable conversation.
 */
export function findProposals(requestId) {
    return request(`/host/request/${requestId}/proposals`);
}

/** Post-MVP correction. 204. The booking stays on its own request. */
export function updateBooking(bookingId, form) {
    return request(`/host/booking/${bookingId}`, { method: 'PUT', body: form });
}

/*
 * Booked trips live in exploreService -- the /explore endpoint is not a host route,
 * and having two modules call it was the kind of duplication that drifts.
 */
