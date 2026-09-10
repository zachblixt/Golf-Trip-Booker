import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, radius, spacing, type } from '../theme';
import { money, summaryLine } from '../utils/format';

/**
 * Explains why the form arrived full.
 *
 * Without this, someone who tapped through from Explore sees a New Request screen
 * carrying somebody else's numbers and no clue where they came from. Clear wipes it
 * back to empty, because "I want to start over" is a normal thing to want.
 */
export default function TemplateBanner({ source, onClear }) {
    if (!source) {
        return null;
    }

    const shape = summaryLine([
        source.players ? `${source.players} ${source.players === 1 ? 'player' : 'players'}` : null,
        source.rounds ? `${source.rounds} ${source.rounds === 1 ? 'round' : 'rounds'}` : null,
        source.nights ? `${source.nights} ${source.nights === 1 ? 'night' : 'nights'}` : null,
    ]);

    return (
        <View style={styles.wrap}>
            <View style={styles.head}>
                <Text style={styles.title}>Based on a trip to {source.label}</Text>
                <Pressable onPress={onClear} hitSlop={10} style={({ pressed }) => pressed && styles.pressed}>
                    <Text style={styles.clear}>Clear</Text>
                </Pressable>
            </View>

            <Text style={type.muted}>{shape}</Text>

            {/*
              * The original figure stays visible rather than being folded silently into
              * the budget. Prices move; this one is a reference, not a quote, and the
              * person deciding should be able to see what they are aiming at.
              */}
            {source.price ? (
                <Text style={[type.muted, styles.price]}>
                    That trip cost {money(source.price)} a player
                    {source.when ? ` in ${source.when}` : ''}. Your budget is rounded up from it &mdash;
                    change anything below before you send it.
                </Text>
            ) : null}
        </View>
    );
}

const styles = StyleSheet.create({
    wrap: {
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderLeftWidth: 3,
        borderLeftColor: colors.money,
        borderRadius: radius.sm,
        padding: spacing.md,
        marginBottom: spacing.lg,
    },
    head: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: spacing.xs,
    },
    title: { fontSize: 15, fontWeight: '700', color: colors.text, flex: 1, paddingRight: spacing.sm },
    clear: { fontSize: 13, fontWeight: '600', color: colors.primary },
    pressed: { opacity: 0.6 },
    price: { marginTop: spacing.sm, lineHeight: 19 },
});
