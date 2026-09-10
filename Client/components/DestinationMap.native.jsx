import { useEffect, useRef, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import MapView, { Marker } from 'react-native-maps';
import * as Location from 'expo-location';

import { colors, radius, spacing } from '../theme';

/*
 * Split by platform on purpose. react-native-maps is a native module with no web
 * implementation, and importing it under react-native-web crashes the bundle --
 * a guard inside render is too late, because the import runs first. Metro resolves
 * .native.jsx for iOS and Android and .web.jsx for the browser, so the import below
 * never happens on web at all.
 */

/** Continental US, used when we have neither a location fix nor any pins. */
const FALLBACK_REGION = {
    latitude: 39.5,
    longitude: -92.0,
    latitudeDelta: 30,
    longitudeDelta: 30,
};

/**
 * Destinations plotted by what trips there have actually cost.
 *
 * The price on the pin is the point. A map of place names tells a golfer nothing
 * they did not already know; a map that says "$390 a head, four hours away" is the
 * whole product.
 *
 * `points` come from Explore's own results, so the map always shows exactly what
 * the current filters show -- there is no second query to drift out of sync.
 */
export default function DestinationMap({ points, selectedId, onSelect }) {
    const mapRef = useRef(null);
    const [userRegion, setUserRegion] = useState(null);
    const [locationDenied, setLocationDenied] = useState(false);

    /*
     * Centring on the user is the difference between "here are ten places" and
     * "here is what is near you". Permission is asked once and a refusal is not an
     * error -- the map just falls back to fitting every pin.
     */
    useEffect(() => {
        let cancelled = false;

        (async () => {
            try {
                const { status } = await Location.requestForegroundPermissionsAsync();
                if (status !== 'granted') {
                    if (!cancelled) setLocationDenied(true);
                    return;
                }

                // Low accuracy on purpose: this centres a map, it does not navigate.
                // It returns in a fraction of the time and costs far less battery.
                const position = await Location.getCurrentPositionAsync({
                    accuracy: Location.Accuracy.Low,
                });

                if (cancelled) return;

                setUserRegion({
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                    // Roughly a 350-mile view: a plausible drive for a weekend.
                    latitudeDelta: 8,
                    longitudeDelta: 8,
                });
            } catch {
                if (!cancelled) setLocationDenied(true);
            }
        })();

        return () => {
            cancelled = true;
        };
    }, []);

    /*
     * Frame the map once the pins are known.
     *
     * Fitting the pins beats centring on the user. Explore exists to show the trips
     * that exist, and somebody with nothing inside a few hundred miles would otherwise
     * open the tab on empty ground. Their own position still shows as the blue dot, so
     * "where am I relative to these" is answered without the map being anchored there.
     *
     * A single pin is the exception: fitToCoordinates on one coordinate zooms to the
     * maximum, which puts you on somebody's roof. That gets a fixed regional view.
     */
    useEffect(() => {
        if (!mapRef.current) {
            return;
        }

        const timer = setTimeout(() => {
            if (points.length > 1) {
                mapRef.current?.fitToCoordinates(
                    points.map((p) => ({ latitude: p.latitude, longitude: p.longitude })),
                    { edgePadding: { top: 60, right: 60, bottom: 60, left: 60 }, animated: false }
                );
            } else if (points.length === 1) {
                mapRef.current?.animateToRegion({
                    latitude: points[0].latitude,
                    longitude: points[0].longitude,
                    latitudeDelta: 4,
                    longitudeDelta: 4,
                }, 400);
            } else if (userRegion) {
                mapRef.current?.animateToRegion(userRegion, 400);
            }
        }, 300); // the map needs a beat to lay out before it can fit anything

        return () => clearTimeout(timer);
    }, [points, userRegion]);

    return (
        <View style={styles.wrap}>
            <MapView
                ref={mapRef}
                style={StyleSheet.absoluteFill}
                /*
                 * initialRegion ONLY. `region` is a controlled prop: passing it and not
                 * feeding gestures back through onRegionChangeComplete makes the map
                 * spring back to that region after every pan and pinch, so zooming out
                 * is impossible and zooming in only appears to work while it animates.
                 * Framing is done imperatively above instead.
                 */
                initialRegion={FALLBACK_REGION}
                showsUserLocation={!!userRegion}
                /*
                 * Tapping bare map dismisses the card, so you can go back to browsing
                 * pins without hunting for a close button.
                 *
                 * The guard matters: on Android a marker tap ALSO fires the map's
                 * onPress, with action 'marker-press'. Without this check the card
                 * would open and close in the same gesture and the map would look
                 * broken on Android only.
                 */
                onPress={(event) => {
                    if (event?.nativeEvent?.action === 'marker-press') {
                        return;
                    }
                    onSelect(null);
                }}
            >
                {points.map((point) => {
                    const selected = point.destinationId === selectedId;

                    return (
                        <Marker
                            key={point.destinationId}
                            coordinate={{ latitude: point.latitude, longitude: point.longitude }}
                            // Tapping the open pin again closes it -- the third way out,
                            // for anyone who treats a pin as a toggle.
                            onPress={() => onSelect(selected ? null : point.destinationId)}
                            // Stops the default callout bubble; the card below the map
                            // is the detail view, and two of them would fight.
                            tracksViewChanges={false}
                        >
                            <View style={[styles.pin, selected && styles.pinSelected]}>
                                <Text style={[styles.pinText, selected && styles.pinTextSelected]}>
                                    {point.label}
                                </Text>
                            </View>
                        </Marker>
                    );
                })}
            </MapView>

            {locationDenied ? (
                <View style={styles.notice}>
                    <Text style={styles.noticeText}>
                        Location off — showing everywhere instead
                    </Text>
                </View>
            ) : null}

            {/* ODbL and Apple both require attribution for the underlying map data. */}
            <View style={styles.credit}>
                <Text style={styles.creditText}>Prices from past bookings</Text>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    wrap: { flex: 1, overflow: 'hidden' },

    pin: {
        backgroundColor: colors.card,
        borderWidth: 1.5,
        borderColor: colors.money,
        borderRadius: radius.pill,
        paddingHorizontal: spacing.sm + 2,
        paddingVertical: 4,
    },
    pinSelected: { backgroundColor: colors.primary, borderColor: colors.primary },
    pinText: { fontSize: 12, fontWeight: '700', color: colors.money },
    pinTextSelected: { color: colors.primaryText },

    notice: {
        position: 'absolute',
        top: spacing.sm,
        alignSelf: 'center',
        backgroundColor: colors.card,
        borderRadius: radius.pill,
        paddingHorizontal: spacing.md,
        paddingVertical: spacing.xs,
    },
    noticeText: { fontSize: 12, color: colors.muted },

    credit: {
        position: 'absolute',
        bottom: spacing.xs,
        left: spacing.sm,
        backgroundColor: 'rgba(255,255,255,0.8)',
        borderRadius: radius.sm,
        paddingHorizontal: spacing.sm,
        paddingVertical: 2,
    },
    creditText: { fontSize: 10, color: colors.muted },

});
