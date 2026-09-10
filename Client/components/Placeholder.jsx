import { StyleSheet, Text, View } from 'react-native';

import Screen from './Screen';
import { colors, radius, spacing, type } from '../theme';

/**
 * A screen that has a route and a tab but no content yet. Names the wireframe it
 * will become and the endpoint it will call, so an empty tab is a to-do list
 * rather than a mystery.
 */
export default function Placeholder({ title, wireframe, endpoint, note }) {
    return (
        <Screen>
            <Text style={type.title}>{title}</Text>

            <View style={styles.card}>
                <Text style={type.label}>NEXT UP</Text>
                <Text style={[type.body, styles.line]}>{wireframe}</Text>

                {endpoint ? (
                    <>
                        <Text style={[type.label, styles.spaced]}>ENDPOINT</Text>
                        <Text style={styles.code}>{endpoint}</Text>
                    </>
                ) : null}

                {note ? <Text style={[type.muted, styles.spaced]}>{note}</Text> : null}
            </View>
        </Screen>
    );
}

const styles = StyleSheet.create({
    card: {
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.md,
        padding: spacing.md,
        marginTop: spacing.lg,
    },
    line: { marginTop: spacing.xs },
    spaced: { marginTop: spacing.md },
    code: {
        marginTop: spacing.xs,
        fontFamily: 'monospace',
        fontSize: 13,
        color: colors.primary,
    },
});
