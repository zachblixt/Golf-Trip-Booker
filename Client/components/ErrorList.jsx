import { StyleSheet, Text, View } from 'react-native';

import { colors, radius, spacing } from '../theme';

/**
 * The server always sends errors as an array of strings, so this renders whatever
 * it is handed without inspecting it. Keyed by index on purpose: the same message
 * can legitimately appear twice in one response, and duplicate keys drop a row.
 */
export default function ErrorList({ errors }) {
    if (!errors || errors.length === 0) {
        return null;
    }

    return (
        <View style={styles.box}>
            {errors.map((error, index) => (
                <Text key={index} style={styles.text}>
                    {error}
                </Text>
            ))}
        </View>
    );
}

const styles = StyleSheet.create({
    box: {
        backgroundColor: '#FBE4E2',
        borderRadius: radius.sm,
        padding: spacing.md,
        marginBottom: spacing.md,
    },
    text: { color: colors.danger, fontSize: 14, lineHeight: 20 },
});
