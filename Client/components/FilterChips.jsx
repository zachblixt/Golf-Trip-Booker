import { Pressable, ScrollView, StyleSheet, Text } from 'react-native';

import { colors, radius, spacing } from '../theme';

/**
 * A horizontal row of single-select pills.
 *
 * Chips rather than a dropdown because the options are few and a dropdown on a
 * phone costs a tap to open, a tap to choose, and a native picker that looks
 * different on each platform. Everything visible, one tap to change.
 */
export default function FilterChips({ options, selectedValue, onSelect }) {
    return (
        <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.row}
        >
            {options.map((option) => {
                const selected = option.value === selectedValue;

                return (
                    <Pressable
                        key={String(option.value)}
                        onPress={() => onSelect(option.value)}
                        style={({ pressed }) => [
                            styles.chip,
                            selected && styles.chipSelected,
                            pressed && styles.pressed,
                        ]}
                    >
                        <Text style={[styles.text, selected && styles.textSelected]}>
                            {option.label}
                        </Text>
                    </Pressable>
                );
            })}
        </ScrollView>
    );
}

const styles = StyleSheet.create({
    row: { gap: spacing.sm, paddingVertical: spacing.xs, paddingRight: spacing.lg },
    chip: {
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.pill,
        paddingHorizontal: spacing.md,
        paddingVertical: spacing.sm,
    },
    chipSelected: { backgroundColor: colors.primary, borderColor: colors.primary },
    pressed: { opacity: 0.7 },
    text: { fontSize: 14, color: colors.text },
    textSelected: { color: colors.primaryText, fontWeight: '600' },
});
