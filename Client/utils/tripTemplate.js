import { monthYear } from './format';

/**
 * Turning a trip somebody already took into a request of your own.
 *
 * Everything here is client-side. The template travels as query parameters over
 * data /explore already returns, so no endpoint changes and nothing leaks -- no
 * request id, no client, nothing the anonymised card did not already show.
 */

/** $390 -> $400. A small cushion, because the copied price is a reference, not a quote. */
function roundUpTo(value, step) {
    return Math.ceil(Number(value) / step) * step;
}

/**
 * trip_request has no course column -- courses live on booking, which only a host
 * creates. Notes is the one channel a client has, and putting the course list there
 * is what turns "4 players, 3 rounds, Branson" into something a host can book exactly.
 */
function noteFrom(courses, lodging) {
    const parts = [];
    if (courses?.length) {
        parts.push(`Hoping to play: ${courses.join(', ')}.`);
    }
    if (lodging) {
        parts.push(`That group stayed at ${lodging}.`);
    }
    return parts.join(' ');
}

/** From an Explore card (BookedTripResponse). */
export function templateFromBookedTrip(trip) {
    const perPlayer = Number(trip.costPerPlayer);
    return {
        destinationId: trip.destination?.destinationId,
        players: trip.playerCount,
        rounds: trip.roundsPlayed,
        nights: trip.nights,
        budget: roundUpTo(perPlayer, 25),
        notes: noteFrom(trip.courses, trip.lodging),
        fromLabel: trip.placeLabel ?? trip.destination?.name,
        fromPrice: Math.round(perPlayer),
        fromPlayers: trip.playerCount,
        fromWhen: monthYear(trip.startDate),
    };
}

/** From your own booked trip (TripRequestResponse with a booking attached). */
export function templateFromOwnTrip(trip) {
    const booking = trip.booking;
    const perPlayer = trip.playerCount > 0 ? Number(booking.totalCost) / trip.playerCount : 0;

    return {
        destinationId: trip.destination?.destinationId,
        players: trip.playerCount,
        // What was actually played, not what was asked for -- a host may have booked
        // two courses against a three-round request.
        rounds: booking.courses?.length || trip.roundsRequested,
        nights: trip.nights,
        budget: roundUpTo(perPlayer, 25),
        notes: noteFrom(booking.courses, booking.lodging),
        fromLabel: trip.placeLabel ?? trip.destination?.name,
        fromPrice: Math.round(perPlayer),
        fromPlayers: trip.playerCount,
        fromWhen: monthYear(booking.startDate),
    };
}

export function toQuery(template) {
    const pairs = Object.entries(template)
        .filter(([, v]) => v !== null && v !== undefined && v !== '')
        .map(([k, v]) => `${k}=${encodeURIComponent(v)}`);
    return pairs.join('&');
}

/**
 * Query params arrive as strings, and RequestForm holds strings, so most of this is
 * a pass-through. The travel window is deliberately absent: the source trip's dates
 * are in the past, and the server rejects an earliest start that has already been.
 */
export function formValuesFromParams(params) {
    return {
        destinationId: params.destinationId ? Number(params.destinationId) : null,
        // A template always comes from a booked trip, so it always has a real
        // destination -- but the key has to exist for the form's shape to be whole.
        requestedPlace: '',
        budgetPerPlayer: params.budget ?? '',
        playerCount: params.players ?? '',
        roundsRequested: params.rounds ?? '',
        nights: params.nights ?? '',
        earliestStart: '',
        latestStart: '',
        notes: params.notes ?? '',
    };
}

/** The banner's content, or null when nobody arrived from a template. */
export function sourceFromParams(params) {
    if (!params.fromLabel) {
        return null;
    }
    return {
        label: params.fromLabel,
        price: params.fromPrice ? Number(params.fromPrice) : null,
        players: params.fromPlayers ? Number(params.fromPlayers) : null,
        when: params.fromWhen ?? null,
        rounds: params.rounds ? Number(params.rounds) : null,
        nights: params.nights ? Number(params.nights) : null,
    };
}
