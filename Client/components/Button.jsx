import { ActivityIndicator, Pressable, StyleSheet, Text } from 'react-native';

import { colors, radius, spacing } from '../theme';

/**
 * variant: 'primary' | 'secondary' | 'danger'
 *
 * Disabled while `loading` as well as while `disabled`, so a double tap cannot
 * submit a form twice -- which on New Request would mean two trip requests.
 */
export default function Button({
    title,
    onPress,
    variant = 'primary',
    loading = false,
    disabled = false,
}) {
    const isDisabled = disabled || loading;

    return (
        <Pressable
            onPress={onPress}
            disabled={isDisabled}
            style={({ pressed }) => [
                styles.base,
                styles[variant],
                pressed && !isDisabled && styles.pressed,
                isDisabled && styles.disabled,
            ]}
        >
            {loading ? (
                <ActivityIndicator color={variant === 'secondary' ? colors.text : colors.primaryText} />
            ) : (
                <Text style={[styles.text, styles[`${variant}Text`]]}>{title}</Text>
            )}
        </Pressable>
    );
}

const styles = StyleSheet.create({
    base: {
        borderRadius: radius.sm,
        paddingVertical: spacing.md,
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: spacing.sm + 4,
        minHeight: 50,
    },
    primary: { backgroundColor: colors.primary },
    secondary: {
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
    },
    danger: { backgroundColor: colors.danger },

    text: { fontSize: 15, fontWeight: '600' },
    primaryText: { color: colors.primaryText },
    secondaryText: { color: colors.text },
    dangerText: { color: colors.primaryText },

    pressed: { opacity: 0.85 },
    disabled: { opacity: 0.5 },
});
