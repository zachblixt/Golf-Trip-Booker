import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, radius, spacing, type } from '../theme';

/**
 * A list of choices rendered inline rather than behind a dropdown.
 *
 * Deliberately not @react-native-picker/picker: that is a native module with a
 * different look on each platform and nothing to show while it loads. With five
 * seeded destinations, showing all of them costs one screenful and removes a
 * dependency, a platform difference, and a tap.
 */
export default function Select({ label, options, selectedValue, onSelect, keyExtractor, renderLabel, renderMeta }) {
    return (
        <View style={styles.wrap}>
            <Text style={styles.label}>{label.toUpperCase()}</Text>

            {options.map((option) => {
                const value = keyExtractor(option);
                const selected = value === selectedValue;

                return (
                    <Pressable
                        key={value}
                        onPress={() => onSelect(value)}
                        style={({ pressed }) => [
                            styles.option,
                            selected && styles.selected,
                            pressed && styles.pressed,
                        ]}
                    >
                        <View style={styles.optionText}>
                            <Text style={[styles.name, selected && styles.selectedName]}>
                                {renderLabel(option)}
                            </Text>
                            {renderMeta ? <Text style={type.muted}>{renderMeta(option)}</Text> : null}
                        </View>

                        <View style={[styles.dot, selected && styles.dotSelected]} />
                    </Pressable>
                );
            })}
        </View>
    );
}

const styles = StyleSheet.create({
    wrap: { marginBottom: spacing.md },
    label: { ...type.label, marginBottom: spacing.xs },

    option: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.sm,
        paddingHorizontal: spacing.md,
        paddingVertical: spacing.sm + 4,
        marginBottom: spacing.sm,
    },
    selected: { borderColor: colors.primary, borderWidth: 2 },
    pressed: { opacity: 0.7 },

    optionText: { flex: 1, paddingRight: spacing.sm },
    name: { fontSize: 15, color: colors.text },
    selectedName: { fontWeight: '700' },

    dot: {
        width: 18,
        height: 18,
        borderRadius: 9,
        borderWidth: 2,
        borderColor: colors.border,
    },
    dotSelected: { borderColor: colors.primary, backgroundColor: colors.primary },
});
