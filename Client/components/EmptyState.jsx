import { StyleSheet, Text, View } from 'react-native';

import { spacing, type } from '../theme';

/**
 * Shown when a list is legitimately empty, which is not the same as still loading
 * or having failed. Keeping those three states distinct is most of what makes a
 * list screen feel trustworthy.
 */
export default function EmptyState({ title, message }) {
    return (
        <View style={styles.wrap}>
            <Text style={type.heading}>{title}</Text>
            <Text style={[type.muted, styles.message]}>{message}</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    wrap: { paddingVertical: spacing.xl, alignItems: 'center' },
    message: { marginTop: spacing.sm, textAlign: 'center' },
});
