import { Pressable, StyleSheet, View } from 'react-native';

import { colors, radius, spacing } from '../theme';

/** Renders as a Pressable only when there is somewhere to go. */
export default function Card({ children, onPress, style }) {
    if (!onPress) {
        return <View style={[styles.card, style]}>{children}</View>;
    }

    return (
        <Pressable
            onPress={onPress}
            style={({ pressed }) => [styles.card, pressed && styles.pressed, style]}
        >
            {children}
        </Pressable>
    );
}

const styles = StyleSheet.create({
    card: {
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.md,
        padding: spacing.md,
        marginBottom: spacing.sm + 4,
    },
    pressed: { opacity: 0.7 },
});
