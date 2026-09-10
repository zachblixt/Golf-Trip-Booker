/** Mean radius of the earth in miles. */
const EARTH_MILES = 3958.8;

const toRadians = (degrees) => (degrees * Math.PI) / 180;

/**
 * Straight-line distance between two coordinates.
 *
 * Haversine, not driving distance -- routing would mean an API key, a network call
 * per destination, and a quota. "182 mi" is enough to answer "could we drive there
 * on a Friday", which is the only question this number needs to settle.
 */
export function milesBetween(from, to) {
    if (!from || !to) {
        return null;
    }

    const dLat = toRadians(to.latitude - from.latitude);
    const dLon = toRadians(to.longitude - from.longitude);

    const a =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(toRadians(from.latitude)) *
            Math.cos(toRadians(to.latitude)) *
            Math.sin(dLon / 2) ** 2;

    return 2 * EARTH_MILES * Math.asin(Math.sqrt(a));
}

/** "182 mi". Rounded to ten above a hundred, because false precision reads as wrong. */
export function miles(distance) {
    if (distance == null) {
        return '';
    }
    if (distance < 100) {
        return `${Math.round(distance)} mi`;
    }
    return `${Math.round(distance / 10) * 10} mi`;
}
