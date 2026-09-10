import { request } from './api';

/**
 * My Trips. Note there is no user id parameter -- the server scopes this to the
 * token, which is what makes another client's list unreachable rather than merely
 * un-asked-for.
 */
export function findMine() {
    return request('/request');
}

export function findById(requestId) {
    return request(`/request/${requestId}`);
}

/** 201 with the created request. The server sets the client, status, and timestamp. */
export function create(form) {
    return request('/request', { method: 'POST', body: form });
}

/** Post-MVP edit. 204, and only while the request is still PENDING. */
export function update(requestId, form) {
    return request(`/request/${requestId}`, { method: 'PUT', body: form });
}

/**
 * 204. Accept what the host arranged: the proposal becomes the booking. This is the
 * only route to a booked trip anywhere in the app -- nothing the host does gets there.
 */
export function acceptProposal(requestId) {
    return request(`/request/${requestId}/accept`, { method: 'POST' });
}

/**
 * 204. Turn the proposal down. Reason and note are both required by the server; the
 * request goes back to pending for the host to try again, which is exactly what makes
 * this different from cancelling.
 */
export function counterProposal(requestId, reason, note) {
    return request(`/request/${requestId}/counter`, { method: 'POST', body: { reason, note } });
}

/**
 * Cancel. A DELETE because that is what it means to the client, but the row
 * survives server-side as CANCELLED -- the host's record of what was asked for
 * should not evaporate.
 */
export function cancel(requestId) {
    return request(`/request/${requestId}`, { method: 'DELETE' });
}
