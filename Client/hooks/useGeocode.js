import { useEffect, useState } from 'react';
import * as Location from 'expo-location';

/** Long enough that typing "Missoula, Montana" is one lookup, not fifteen. */
const DEBOUNCE_MS = 700;

/** Below this we do not bother -- "Mi" geocodes to something, and it is never right. */
const MIN_LENGTH = 4;

/**
 * Turning a typed place name into coordinates, on the phone.
 *
 * expo-location wraps the OS geocoder (CLGeocoder on iOS, android.location.Geocoder
 * on Android), so this costs no API key, no billing account, and no network call we
 * have to own. It is also allowed to fail, and does -- on web, on an Android build
 * with no Play Services, on a name nobody recognises.
 *
 * That is why nothing here is required. Coordinates are a bonus: with them a
 * promoted destination lands on the Explore map the moment a host books it. Without
 * them the request still goes through, and the place is simply list-only until
 * someone fills the coordinates in.
 *
 * Returns { coords, status } where status is one of:
 *   idle     -- nothing typed worth looking up
 *   looking  -- a lookup is in flight
 *   found    -- coords are set
 *   unknown  -- we tried and got nothing back
 */
export default function useGeocode(place) {
    const [coords, setCoords] = useState(null);
    const [status, setStatus] = useState('idle');

    useEffect(() => {
        const query = (place ?? '').trim();

        if (query.length < MIN_LENGTH) {
            setCoords(null);
            setStatus('idle');
            return undefined;
        }

        let cancelled = false;
        setStatus('looking');

        const timer = setTimeout(async () => {
            try {
                const results = await Location.geocodeAsync(query);
                if (cancelled) return;

                const first = results?.[0];
                if (first?.latitude != null && first?.longitude != null) {
                    setCoords({ latitude: first.latitude, longitude: first.longitude });
                    setStatus('found');
                } else {
                    setCoords(null);
                    setStatus('unknown');
                }
            } catch {
                // No geocoder on this platform, or it refused. Not an error worth
                // showing -- the request works without coordinates.
                if (!cancelled) {
                    setCoords(null);
                    setStatus('unknown');
                }
            }
        }, DEBOUNCE_MS);

        return () => {
            cancelled = true;
            clearTimeout(timer);
        };
    }, [place]);

    return { coords, status };
}
