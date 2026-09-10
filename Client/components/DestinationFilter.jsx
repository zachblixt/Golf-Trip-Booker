import { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import Ionicons from '@expo/vector-icons/Ionicons';

import useUserLocation from '../hooks/useUserLocation';
import { colors, radius, spacing, type } from '../theme';
import { miles, milesBetween } from '../utils/geo';
import { money } from '../utils/format';

/** Same cap as the New Request picker, for the same reason. */
const VISIBLE = 8;

/**
 * Explore's destination filter.
 *
 * A chip row was fine at three destinations and stopped being fine at forty --
 * finding Gaylord meant swiping past thirty-nine others with no way to jump ahead.
 * This is the same search-and-collapse shape as the New Request picker, so choosing
 * a destination works identically in both places.
 *
 * Two things it does that the picker does not:
 *
 * 1. Every row carries what trips there actually cost -- "from $390 · 4 trips". On a
 *    screen whose whole argument is the price, the price belongs in the list you
 *    choose from, not only in the results.
 *
 * 2. Only destinations with a booked trip are offered. Explore filters past trips,
 *    so a destination nobody has been to is a guaranteed empty state. Leaving it out
 *    is more honest than letting someone pick it and find nothing.
 *
 * Collapsed to a single line by default, because the map underneath is the point of
 * the screen and a permanently open list would push it off the bottom.
 */
export default function DestinationFilter({ options, selectedValue, onSelect }) {
    const { coords } = useUserLocation();
    const [open, setOpen] = useState(false);
    const [query, setQuery] = useState('');

    const selected = options.find((o) => o.destinationId === selectedValue) ?? null;

    const ranked = useMemo(() => {
        const withDistance = options.map((o) => ({
            ...o,
            distance:
                coords && o.latitude != null && o.longitude != null
                    ? milesBetween(coords, { latitude: o.latitude, longitude: o.longitude })
                    : null,
        }));

        // Nearest first, alphabetical without a fix -- the same order the New Request
        // picker uses, so the two lists never disagree about where things are.
        withDistance.sort((a, b) => {
            if (a.distance != null && b.distance != null) return a.distance - b.distance;
            if (a.distance != null) return -1;
            if (b.distance != null) return 1;
            return a.name.localeCompare(b.name);
        });

        return withDistance;
    }, [options, coords]);

    const matches = useMemo(() => {
        const q = query.trim().toLowerCase();
        if (!q) return ranked;
        return ranked.filter(
            (o) => o.name.toLowerCase().includes(q) || (o.region ?? '').toLowerCase().includes(q)
        );
    }, [ranked, query]);

    const shown = matches.slice(0, VISIBLE);
    const hidden = matches.length - shown.length;

    function choose(destinationId) {
        onSelect(destinationId);
        setQuery('');
        setOpen(false);
    }

    if (!open) {
        return (
            <Pressable
                onPress={() => setOpen(true)}
                style={({ pressed }) => [styles.collapsed, pressed && styles.pressed]}
            >
                <Ionicons name="location-outline" size={16} color={colors.muted} />
                <View style={styles.collapsedText}>
                    <Text style={styles.name}>{selected ? selected.name : 'Anywhere'}</Text>
                    {selected?.region ? <Text style={type.muted}>{selected.region}</Text> : null}
                </View>
                <Text style={styles.change}>Change</Text>
            </Pressable>
        );
    }

    return (
        <View>
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
                    autoFocus
                />
                <Pressable onPress={() => setOpen(false)} hitSlop={10}>
                    <Text style={styles.change}>Done</Text>
                </Pressable>
            </View>

            {/* Pinned, and never filtered out -- clearing the filter should not depend
                on remembering what you typed to get here. */}
            <Pressable
                onPress={() => choose(null)}
                style={({ pressed }) => [
                    styles.option,
                    selectedValue == null && styles.optionSelected,
                    pressed && styles.pressed,
                ]}
            >
                <Text style={[styles.name, selectedValue == null && styles.nameSelected]}>
                    Anywhere
                </Text>
            </Pressable>

            {shown.map((o) => {
                const isSelected = o.destinationId === selectedValue;
                return (
                    <Pressable
                        key={o.destinationId}
                        onPress={() => choose(o.destinationId)}
                        style={({ pressed }) => [
                            styles.option,
                            isSelected && styles.optionSelected,
                            pressed && styles.pressed,
                        ]}
                    >
                        <View style={styles.optionText}>
                            <Text style={[styles.name, isSelected && styles.nameSelected]}>{o.name}</Text>
                            <Text style={type.muted}>{o.region}</Text>
                        </View>
                        <View style={styles.optionMeta}>
                            <Text style={styles.from}>from {money(o.from)}</Text>
                            <Text style={[type.muted, styles.count]}>
                                {o.count} {o.count === 1 ? 'trip' : 'trips'}
                                {o.distance != null ? ` · ${miles(o.distance)}` : ''}
                            </Text>
                        </View>
                    </Pressable>
                );
            })}

            {shown.length === 0 ? (
                <Text style={[type.muted, styles.note]}>
                    Nobody has booked anywhere matching &ldquo;{query}&rdquo; yet. Ask for it on the New
                    Request tab and it will show up here once a host books it.
                </Text>
            ) : null}

            {hidden > 0 ? (
                <Text style={[type.muted, styles.note]}>
                    {hidden} more &mdash; keep typing to narrow it down
                    {coords ? '. Sorted by distance from you.' : ''}
                </Text>
            ) : null}
        </View>
    );
}

const styles = StyleSheet.create({
    collapsed: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: spacing.sm,
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.sm,
        paddingHorizontal: spacing.md,
        paddingVertical: spacing.sm + 2,
    },
    collapsedText: { flex: 1 },
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
    optionMeta: { alignItems: 'flex-end' },
    name: { fontSize: 15, color: colors.text },
    nameSelected: { fontWeight: '700' },
    from: { fontSize: 13, fontWeight: '700', color: colors.money },
    count: { fontSize: 11 },

    note: { marginTop: spacing.xs, marginBottom: spacing.xs, lineHeight: 18 },
    pressed: { opacity: 0.7 },
});
