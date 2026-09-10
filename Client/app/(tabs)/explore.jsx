import { useCallback, useEffect, useMemo, useState } from 'react';
import {
    ActivityIndicator,
    FlatList,
    Pressable,
    RefreshControl,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import Ionicons from '@expo/vector-icons/Ionicons';
import { useFocusEffect, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';

import Button from '../../components/Button';
import ScreenTitle from '../../components/ScreenTitle';
import Card from '../../components/Card';
import DestinationMap from '../../components/DestinationMap';
import EmptyState from '../../components/EmptyState';
import ErrorList from '../../components/ErrorList';
import DestinationFilter from '../../components/DestinationFilter';
import Field from '../../components/Field';
import * as exploreService from '../../services/exploreService';
import { ApiError } from '../../services/api';
import { colors, radius, spacing, type } from '../../theme';
import { money, moneyExact, shortDate, summaryLine } from '../../utils/format';
import { templateFromBookedTrip, toQuery } from '../../utils/tripTemplate';

const ANYWHERE = null;

/** Wireframe 06, plus the map. */
export default function Explore() {
    const router = useRouter();

    // Every booked trip, unfiltered. Feeds the destination filter's options so they
    // stay the same whatever the budget field says -- narrowing the budget should not
    // silently remove places from the list you are choosing from.
    const [allTrips, setAllTrips] = useState([]);
    const [maxBudget, setMaxBudget] = useState('');
    const [appliedBudget, setAppliedBudget] = useState('');
    const [destinationId, setDestinationId] = useState(ANYWHERE);

    const [trips, setTrips] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [errors, setErrors] = useState([]);

    const [showMap, setShowMap] = useState(true);
    const [selectedPin, setSelectedPin] = useState(null);

    /*
     * On focus rather than on mount: a host who books a request for somewhere new
     * creates a destination, and switching straight to this tab should show it. On
     * mount only, it would stay missing until the app restarted.
     *
     * A failure here costs the filter's options, not the screen -- the filtered fetch
     * below reports its own errors.
     */
    useFocusEffect(
        useCallback(() => {
            let cancelled = false;
            exploreService
                .findBooked({})
                .then((found) => {
                    if (!cancelled) setAllTrips(found);
                })
                .catch(() => {});
            return () => {
                cancelled = true;
            };
        }, [])
    );

    /*
     * Typing "500" would otherwise fire three requests, and they can return out of
     * order -- leaving the screen showing results for "50". Waiting for a pause
     * fixes the race and the load together.
     */
    useEffect(() => {
        const timer = setTimeout(() => setAppliedBudget(maxBudget), 400);
        return () => clearTimeout(timer);
    }, [maxBudget]);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);

        exploreService
            .findBooked({
                maxCostPerPlayer: appliedBudget ? Number(appliedBudget) : null,
                destinationId,
            })
            .then((found) => {
                if (!cancelled) {
                    setTrips(found);
                    setErrors([]);
                }
            })
            .catch((error) => {
                if (!cancelled) {
                    setErrors(error instanceof ApiError ? error.messages : ['Could not load trips.']);
                }
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [appliedBudget, destinationId]);

    async function handleRefresh() {
        setRefreshing(true);
        try {
            // Both, because a pull should also pick up a destination booked since the
            // tab was opened -- otherwise the list refreshes and the filter does not.
            const [filtered, everything] = await Promise.all([
                exploreService.findBooked({
                    maxCostPerPlayer: appliedBudget ? Number(appliedBudget) : null,
                    destinationId,
                }),
                exploreService.findBooked({}),
            ]);
            setTrips(filtered);
            setAllTrips(everything);
            setErrors([]);
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not load trips.']);
        } finally {
            setRefreshing(false);
        }
    }

    /*
     * Pins are derived from the trips already on screen rather than fetched
     * separately, so the map and the list can never disagree about what matches the
     * filters. The label is the cheapest trip booked there -- "from $390" is both
     * the honest number and the persuasive one.
     */
    const points = useMemo(
        () => groupByDestination(trips, true).map((p) => ({ ...p, label: money(p.from) })),
        [trips]
    );

    /*
     * Built from every booked trip, not the filtered ones, and without requiring
     * coordinates -- a destination promoted from a free-text request may not have
     * been geocoded, and it should still be filterable even though it cannot be
     * pinned. This is why the map can show fewer places than the filter offers.
     */
    const filterOptions = useMemo(() => groupByDestination(allTrips, false), [allTrips]);

    const selected = points.find((p) => p.destinationId === selectedPin) ?? null;

    return (
        <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
            <View style={styles.header}>
                <ScreenTitle>Explore</ScreenTitle>
                <Text style={[type.muted, styles.tagline]}>
                    What other groups actually paid
                </Text>

                <Field
                    label="Max per player"
                    value={maxBudget}
                    onChangeText={(text) => setMaxBudget(text.replace(/[^0-9.]/g, ''))}
                    placeholder="Any budget"
                    keyboardType="numeric"
                />

                <Text style={type.label}>DESTINATION</Text>
                <DestinationFilter
                    options={filterOptions}
                    selectedValue={destinationId}
                    onSelect={setDestinationId}
                />

                <View style={styles.toggle}>
                    <ToggleButton label="Map" active={showMap} onPress={() => setShowMap(true)} />
                    <ToggleButton label="List" active={!showMap} onPress={() => setShowMap(false)} />
                </View>

                <View style={styles.errorSlot}>
                    <ErrorList errors={errors} />
                </View>
            </View>

            {loading ? (
                <ActivityIndicator size="large" color={colors.primary} style={styles.loading} />
            ) : showMap ? (
                <View style={styles.mapArea}>
                    <DestinationMap points={points} selectedId={selectedPin} onSelect={setSelectedPin} />

                    {selected ? (
                        <View style={styles.selected}>
                            <View style={styles.selectedHead}>
                                <View style={styles.selectedTitle}>
                                    <Text style={type.heading}>{selected.name}</Text>
                                    <Text style={type.muted}>{selected.region}</Text>
                                </View>
                                <View>
                                    <Text style={styles.from}>from {money(selected.from)}</Text>
                                    <Text style={[type.muted, styles.perPlayer]}>per player</Text>
                                </View>
                                <Pressable
                                    onPress={() => setSelectedPin(null)}
                                    hitSlop={12}
                                    accessibilityLabel="Close"
                                    style={({ pressed }) => [styles.close, pressed && styles.pressed]}
                                >
                                    <Ionicons name="close" size={18} color={colors.muted} />
                                </Pressable>
                            </View>

                            <Text style={[type.muted, styles.tripCount]}>
                                {selected.count} {selected.count === 1 ? 'trip' : 'trips'} booked here
                            </Text>

                            <Button
                                title="Request this trip"
                                onPress={() =>
                                    router.push(`/new?${toQuery(templateFromBookedTrip(selected.cheapest))}`)
                                }
                            />
                            <Button
                                title="See what they played"
                                variant="secondary"
                                onPress={() => {
                                    setDestinationId(selected.destinationId);
                                    setShowMap(false);
                                }}
                            />
                        </View>
                    ) : points.length > 0 ? (
                        <View style={styles.prompt}>
                            <Text style={type.muted}>Tap a price to see the trip</Text>
                        </View>
                    ) : null}
                </View>
            ) : (
                <FlatList
                    data={trips}
                    keyExtractor={(item, index) => String(index)}
                    contentContainerStyle={styles.list}
                    refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
                    ListEmptyComponent={
                        errors.length > 0 ? null : (
                            <EmptyState
                                title="Nothing matches"
                                message="Try a higher budget, or a different destination."
                            />
                        )
                    }
                    renderItem={({ item }) => (
                        <Card>
                            <View style={styles.cardHead}>
                                <View style={styles.cardTitle}>
                                    <Text style={type.heading}>{item.destination?.name}</Text>
                                    {item.destination?.region ? (
                                        <Text style={type.muted}>{item.destination.region}</Text>
                                    ) : null}
                                </View>
                                <Text style={styles.price}>{moneyExact(item.costPerPlayer)} pp</Text>
                            </View>

                            <Text style={[type.muted, styles.line]}>
                                {summaryLine([
                                    `${item.playerCount} ${item.playerCount === 1 ? 'player' : 'players'}`,
                                    `${item.roundsPlayed} ${item.roundsPlayed === 1 ? 'round' : 'rounds'}`,
                                    `${item.nights} ${item.nights === 1 ? 'night' : 'nights'}`,
                                ])}
                            </Text>

                            {item.courses?.length ? (
                                <Text style={type.muted}>{item.courses.join(' · ')}</Text>
                            ) : null}

                            <Text style={[type.muted, styles.meta]}>
                                {summaryLine([item.lodging, `played ${shortDate(item.startDate)}`])}
                            </Text>

                            <Pressable
                                onPress={() => router.push(`/new?${toQuery(templateFromBookedTrip(item))}`)}
                                style={({ pressed }) => [styles.copy, pressed && styles.pressed]}
                            >
                                <Text style={styles.copyText}>Request a trip like this</Text>
                            </Pressable>
                        </Card>
                    )}
                />
            )}
        </SafeAreaView>
    );
}

/**
 * Trips collapsed to one entry per destination: how many, and the cheapest one.
 *
 * Shared by the map and the filter because they must never disagree about what a
 * place costs. `requireCoordinates` is the only difference -- a pin needs somewhere
 * to sit, a filter row does not.
 */
function groupByDestination(trips, requireCoordinates) {
    const byDestination = new Map();

    for (const trip of trips) {
        const d = trip.destination;
        if (!d) continue;

        const mappable = d.latitude != null && d.longitude != null;
        if (requireCoordinates && !mappable) continue;

        const perPlayer = Number(trip.costPerPlayer);
        const existing = byDestination.get(d.destinationId);

        if (existing) {
            if (perPlayer < existing.from) {
                existing.from = perPlayer;
                existing.cheapest = trip;
            }
            existing.count += 1;
        } else {
            byDestination.set(d.destinationId, {
                destinationId: d.destinationId,
                name: d.name,
                region: d.region,
                latitude: mappable ? Number(d.latitude) : null,
                longitude: mappable ? Number(d.longitude) : null,
                from: perPlayer,
                count: 1,
                // The specific trip behind "from $390" -- what gets copied.
                cheapest: trip,
            });
        }
    }

    return [...byDestination.values()];
}

function ToggleButton({ label, active, onPress }) {
    return (
        <Pressable
            onPress={onPress}
            style={({ pressed }) => [
                styles.toggleButton,
                active && styles.toggleActive,
                pressed && styles.pressed,
            ]}
        >
            <Text style={[styles.toggleText, active && styles.toggleTextActive]}>{label}</Text>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    safe: { flex: 1, backgroundColor: colors.bg },
    header: { paddingHorizontal: spacing.lg, paddingTop: spacing.lg },
    tagline: { marginTop: spacing.xs, marginBottom: spacing.lg },
    errorSlot: { marginTop: spacing.sm },
    loading: { marginTop: spacing.xl },
    list: { padding: spacing.lg, flexGrow: 1 },

    toggle: {
        flexDirection: 'row',
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.sm,
        padding: 3,
        marginTop: spacing.md,
    },
    toggleButton: { flex: 1, paddingVertical: spacing.sm, alignItems: 'center', borderRadius: 4 },
    toggleActive: { backgroundColor: colors.primary },
    toggleText: { fontSize: 14, color: colors.text },
    toggleTextActive: { color: colors.primaryText, fontWeight: '600' },
    pressed: { opacity: 0.7 },

    mapArea: { flex: 1, marginTop: spacing.md },

    selected: {
        position: 'absolute',
        left: spacing.md,
        right: spacing.md,
        bottom: spacing.md,
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.md,
        padding: spacing.md,
    },
    selectedHead: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        gap: spacing.sm,
    },
    close: {
        width: 28,
        height: 28,
        borderRadius: 14,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: colors.bg,
        marginTop: -2,
    },
    selectedTitle: { flex: 1, paddingRight: spacing.sm },
    from: { fontSize: 18, fontWeight: '700', color: colors.money, textAlign: 'right' },
    perPlayer: { textAlign: 'right' },
    tripCount: { marginTop: spacing.xs, marginBottom: spacing.md },

    prompt: {
        position: 'absolute',
        bottom: spacing.lg,
        alignSelf: 'center',
        backgroundColor: colors.card,
        borderRadius: radius.pill,
        paddingHorizontal: spacing.md,
        paddingVertical: spacing.sm,
    },

    cardHead: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: spacing.sm },
    cardTitle: { flex: 1, paddingRight: spacing.sm },
    price: { fontSize: 15, fontWeight: '700', color: colors.money },
    line: { marginBottom: 2 },
    meta: { marginTop: spacing.sm },
    copy: {
        marginTop: spacing.md,
        paddingTop: spacing.sm,
        borderTopWidth: 1,
        borderTopColor: colors.border,
    },
    copyText: { fontSize: 14, fontWeight: '600', color: colors.primary },
});
