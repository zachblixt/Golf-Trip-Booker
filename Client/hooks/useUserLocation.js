import { useEffect, useState } from 'react';
import * as Location from 'expo-location';

/**
 * The caller's rough position, or null.
 *
 * Asks once. If the permission was already granted for the map, this resolves with
 * no second prompt -- expo-location remembers the grant, so the user is never asked
 * twice for the same thing.
 *
 * A refusal is not an error. Everything that uses this has to work without it; the
 * only difference is that destinations come back alphabetical instead of nearest
 * first.
 */
export default function useUserLocation() {
    const [coords, setCoords] = useState(null);
    const [ready, setReady] = useState(false);

    useEffect(() => {
        let cancelled = false;

        (async () => {
            try {
                const { status } = await Location.requestForegroundPermissionsAsync();
                if (status !== 'granted') {
                    return;
                }

                // Low accuracy: this sorts a list, it does not navigate. Returns in a
                // fraction of the time and costs a fraction of the battery.
                const position = await Location.getCurrentPositionAsync({
                    accuracy: Location.Accuracy.Low,
                });

                if (!cancelled) {
                    setCoords({
                        latitude: position.coords.latitude,
                        longitude: position.coords.longitude,
                    });
                }
            } catch {
                // Denied, unavailable, or timed out. Alphabetical it is.
            } finally {
                if (!cancelled) setReady(true);
            }
        })();

        return () => {
            cancelled = true;
        };
    }, []);

    return { coords, ready };
}
