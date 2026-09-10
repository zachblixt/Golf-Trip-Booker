import { request } from './api';

/**
 * Booked trips, anonymised. Both filters are optional -- omitting them both is
 * "any budget, anywhere".
 *
 * The query string is built by hand rather than with URLSearchParams, whose React
 * Native polyfill has historically been incomplete. Two parameters do not justify
 * trusting it.
 */
export function findBooked({ maxCostPerPlayer, destinationId } = {}) {
    const parts = [];

    if (maxCostPerPlayer) {
        parts.push(`maxCostPerPlayer=${encodeURIComponent(maxCostPerPlayer)}`);
    }
    if (destinationId) {
        parts.push(`destinationId=${encodeURIComponent(destinationId)}`);
    }

    return request(`/explore${parts.length ? `?${parts.join('&')}` : ''}`);
}
