import { StyleSheet, Text, View } from 'react-native';

import { radius, spacing, statusStyles } from '../theme';

/**
 * Keyed straight off the API's RequestStatus, so a new status shows up rather than vanishing.
 *
 * `label` overrides the text without touching the colour, because one status can read two
 * ways: PROPOSED is "Needs your OK" to the client who has to answer it and "Waiting on
 * client" to the host who sent it. Same state, same colour, opposite sentence.
 */
export default function StatusPill({ status, label }) {
    const style = statusStyles[status] ?? { bg: '#ECECEF', fg: '#6B6E77', label: status };

    return (
        <View style={[styles.pill, { backgroundColor: style.bg }]}>
            <Text style={[styles.text, { color: style.fg }]}>{label ?? style.label}</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    pill: {
        paddingHorizontal: spacing.sm + 2,
        paddingVertical: 3,
        borderRadius: radius.pill,
        alignSelf: 'flex-start',
    },
    text: { fontSize: 11, fontWeight: '700' },
});
