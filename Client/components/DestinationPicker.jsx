import { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import Ionicons from '@expo/vector-icons/Ionicons';

import useUserLocation from '../hooks/useUserLocation';
import { colors, radius, spacing, type } from '../theme';
import { miles, milesBetween } from '../utils/geo';

/** How many rows to show before asking you to narrow it down. */
const VISIBLE = 8;

/**
 * Where you want to go: one of ours, or somewhere new.
 *
 * Four decisions worth knowing about:
 *
 * 1. Sorted by distance, not alphabetically. Somebody looking for a weekend of golf
 *    for $500 does not know that Gaylord is a golf destination -- but they do know
 *    how far they will drive. Nearest-first answers the question they actually have.
 *    Without a location fix it falls back to alphabetical.
 *
 * 2. Search matches the region as well as the name, so "Michigan" finds Gaylord and
 *    Traverse City even though neither word appears in the query.
 *
 * 3. Eight rows, then a prompt to keep typing. A capped list keeps the form short
 *    and avoids a scrolling list nested inside the form's own ScrollView, which is
 *    the thing React Native handles worst.
 *
 * 4. "Somewhere else" is always the last row, and it carries whatever you already
 *    typed. The list is what we have booked before, not the limit of what we will
 *    book -- a picker that can only offer the past cannot grow the map. Naming a new
 *    place sends `requestedPlace` instead of `destinationId`; if a host books it, the
 *    server promotes it to a real destination and the next person finds it in this
 *    list.
 *
 * The two are mutually exclusive by construction: choosing a row clears the place,
 * naming a place clears the row. The server enforces the same rule, but doing it
 * here means nobody ever sees that error.
 */
export default function DestinationPicker({
    label = 'Destination',
    destinations,
    selectedValue,
    onSelect,
    place = '',
    onPlaceChange,
    // From useGeocode in RequestForm: idle | looking | found | unknown.
    geoStatus = 'idle',
}) {
    const { coords } = useUserLocation();
    const selected = destinations.find((d) => d.destinationId === selectedValue) ?? null;
    const hasPlace = place.trim().length > 0;

    // Open when there is nothing chosen yet -- a fresh form should not need a tap
    // before it shows you anything. A prefilled one stays collapsed to one line.
    const [open, setOpen] = useState(!selectedValue && !hasPlace);
    const [query, setQuery] = useState('');
    // Which half of the picker is showing. Somebody who arrived with a place typed
    // should land back on that panel, not on the list.
    const [mode, setMode] = useState(hasPlace ? 'place' : 'list');

    const ranked = useMemo(() => {
        const withDistance = destinations.map((d) => ({
            ...d,
            distance:
                coords && d.latitude != null && d.longitude != null
                    ? milesBetween(coords, { latitude: Number(d.latitude), longitude: Number(d.longitude) })
                    : null,
        }));

        withDistance.sort((a, b) => {
            if (a.distance != null && b.distance != null) return a.distance - b.distance;
            if (a.distance != null) return -1;
            if (b.distance != null) return 1;
            return a.name.localeCompare(b.name);
        });

        return withDistance;
    }, [destinations, coords]);

    const matches = useMemo(() => {
        const q = query.trim().toLowerCase();
        if (!q) return ranked;
        return ranked.filter(
            (d) => d.name.toLowerCase().includes(q) || (d.region ?? '').toLowerCase().includes(q)
        );
    }, [ranked, query]);

    const shown = matches.slice(0, VISIBLE);
    const hidden = matches.length - shown.length;

    /*
     * Only onSelect. The parent's handler already clears the place text, and calling
     * onPlaceChange here as well queued a second state update that set destinationId
     * back to null -- so a tap selected the row and immediately unselected it, which
     * looked exactly like the rows not being tappable at all.
     */
    function chooseDestination(destinationId) {
        onSelect(destinationId);
        setQuery('');
        setOpen(false);
    }

    /** Carries the search text over, so typing "Missoula" then tapping does not retype it. */
    function switchToPlace() {
        onSelect(null);
        if (query.trim()) {
            onPlaceChange(query.trim());
        }
        setMode('place');
    }

    function switchToList() {
        onPlaceChange('');
        setMode('list');
    }

    // ---------- collapsed ----------

    if (!open && (selected || hasPlace)) {
        return (
            <View style={styles.wrap}>
                <Text style={styles.label}>{label.toUpperCase()}</Text>
                <Pressable
                    onPress={() => setOpen(true)}
                    style={({ pressed }) => [styles.collapsed, pressed && styles.pressed]}
                >
                    <View style={styles.collapsedText}>
                        <Text style={styles.name}>{selected ? selected.name : place.trim()}</Text>
                        <Text style={type.muted}>{selected ? selected.region : 'Somewhere new'}</Text>
                    </View>
                    <Text style={styles.change}>Change</Text>
                </Pressable>
            </View>
        );
    }

    // ---------- somewhere new ----------

    if (mode === 'place') {
        return (
            <View style={styles.wrap}>
                <Text style={styles.label}>{label.toUpperCase()}</Text>

                <View style={styles.searchRow}>
                    <Ionicons name="location-outline" size={16} color={colors.muted} />
                    <TextInput
                        style={styles.search}
                        value={place}
                        onChangeText={onPlaceChange}
                        placeholder="Town and state, e.g. Missoula, Montana"
                        placeholderTextColor={colors.muted}
                        autoCorrect={false}
                    />
                    {hasPlace ? (
                        <Pressable onPress={() => setOpen(false)} hitSlop={10}>
                            <Text style={styles.change}>Done</Text>
                        </Pressable>
                    ) : null}
                </View>

                <Text style={[type.muted, styles.note]}>{geocodeNote(geoStatus, hasPlace)}</Text>

                <Pressable
                    onPress={switchToList}
                    style={({ pressed }) => [styles.elsewhere, pressed && styles.pressed]}
                >
                    <Ionicons name="arrow-back" size={16} color={colors.primary} />
                    <Text style={styles.elsewhereText}>Back to places we have booked</Text>
                </Pressable>
            </View>
        );
    }

    // ---------- the list ----------

    return (
        <View style={styles.wrap}>
            <Text style={styles.label}>{label.toUpperCase()}</Text>

            <View style={styles.searchRow}>
                <Ionicons name="search" size={16} color={colors.muted} />
                <TextInput
                    style={styles.search}
                    value={query}
                    onChangeText={setQuery}
                    placeholder="Search town or state"
                    placeholderTextColor={colors.muted}
                    autoCapitalize="none"
                    autoCorrect={false}
                />
                {selected ? (
                    <Pressable onPress={() => setOpen(false)} hitSlop={10}>
                        <Text style={styles.change}>Done</Text>
                    </Pressable>
                ) : null}
            </View>

            {shown.map((d) => {
                const isSelected = d.destinationId === selectedValue;
                return (
                    <Pressable
                        key={d.destinationId}
                        onPress={() => chooseDestination(d.destinationId)}
                        style={({ pressed }) => [
                            styles.option,
                            isSelected && styles.optionSelected,
                            pressed && styles.pressed,
                        ]}
                    >
                        <View style={styles.optionText}>
                            <Text style={[styles.name, isSelected && styles.nameSelected]}>{d.name}</Text>
                            <Text style={type.muted}>{d.region}</Text>
                        </View>
                        {d.distance != null ? (
                            <Text style={styles.distance}>{miles(d.distance)}</Text>
                        ) : null}
                    </Pressable>
                );
            })}

            {shown.length === 0 ? (
                <Text style={[type.muted, styles.note]}>
                    Nothing we have booked matches &ldquo;{query}&rdquo;.
                </Text>
            ) : null}

            {hidden > 0 ? (
                <Text style={[type.muted, styles.note]}>
                    {hidden} more &mdash; keep typing to narrow it down
                    {coords ? '. Sorted by distance from you.' : ''}
                </Text>
            ) : null}

            {/*
              * Always present, not just when the search comes up empty. Somebody who
              * knows exactly where they want to go should not have to fail a search
              * first to find out they are allowed to ask for it.
              */}
            <Pressable
                onPress={switchToPlace}
                style={({ pressed }) => [styles.elsewhere, pressed && styles.pressed]}
            >
                <Ionicons name="add-circle-outline" size={16} color={colors.primary} />
                <Text style={styles.elsewhereText}>
                    {query.trim() ? `Ask for ${query.trim()} anyway` : 'Somewhere else — name it'}
                </Text>
            </Pressable>
        </View>
    );
}

/**
 * Coordinates are optional, so none of these is an error. The point is to tell the
 * truth about whether this trip will show up on the map if a host books it.
 */
function geocodeNote(status, hasPlace) {
    if (!hasPlace) {
        return 'Name a town and state. A host will price it and get back to you.';
    }
    if (status === 'looking') {
        return 'Looking that up…';
    }
    if (status === 'found') {
        return 'Found it. If a host books this, it joins the map for everyone else.';
    }
    if (status === 'unknown') {
        return 'We could not put that on a map, which is fine — the host still gets the name.';
    }
    return 'Keep typing.';
}

const styles = StyleSheet.create({
    wrap: { marginBottom: spacing.md },
    label: { ...type.label, marginBottom: spacing.xs },

    collapsed: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: colors.card,
        borderWidth: 2,
        borderColor: colors.primary,
        borderRadius: radius.sm,
        paddingHorizontal: spacing.md,
        paddingVertical: spacing.sm + 4,
    },
    collapsedText: { flex: 1, paddingRight: spacing.sm },
    change: { fontSize: 13, fontWeight: '600', color: colors.primary },

    searchRow: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: spacing.sm,
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.sm,
        paddingHorizontal: spacing.md,
        paddingVertical: spacing.sm,
        marginBottom: spacing.sm,
    },
    search: { flex: 1, fontSize: 15, color: colors.text, paddingVertical: 2 },

    option: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.sm,
        paddingHorizontal: spacing.md,
        paddingVertical: spacing.sm + 2,
        marginBottom: spacing.xs + 2,
    },
    optionSelected: { borderColor: colors.primary, borderWidth: 2 },
    optionText: { flex: 1, paddingRight: spacing.sm },
    name: { fontSize: 15, color: colors.text },
    nameSelected: { fontWeight: '700' },
    distance: {
        fontSize: 12,
        fontWeight: '600',
        color: colors.money,
        fontVariant: ['tabular-nums'],
    },

    elsewhere: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: spacing.xs + 2,
        borderWidth: 1,
        borderColor: colors.primary,
        borderStyle: 'dashed',
        borderRadius: radius.sm,
        paddingHorizontal: spacing.md,
        paddingVertical: spacing.sm + 2,
        marginTop: spacing.xs,
    },
    elsewhereText: { flex: 1, fontSize: 14, fontWeight: '600', color: colors.primary },

    note: { marginTop: spacing.xs, marginBottom: spacing.xs },
    pressed: { opacity: 0.7 },
});
