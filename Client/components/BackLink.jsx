import Ionicons from '@expo/vector-icons/Ionicons';
import { Pressable, StyleSheet, Text } from 'react-native';

import { colors, spacing } from '../theme';

/** The chevron-and-title header the detail screens share. */
export default function BackLink({ onPress, label }) {
    return (
        <Pressable onPress={onPress} style={styles.back} hitSlop={8}>
            <Ionicons name="chevron-back" size={22} color={colors.text} />
            <Text style={styles.text}>{label}</Text>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    back: { flexDirection: 'row', alignItems: 'center', marginBottom: spacing.md },
    text: { fontSize: 22, fontWeight: '700', color: colors.text, marginLeft: 2 },
});
