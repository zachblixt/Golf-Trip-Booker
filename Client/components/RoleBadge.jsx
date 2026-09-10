import { StyleSheet, Text, View } from 'react-native';
import Ionicons from '@expo/vector-icons/Ionicons';

import { useAuth } from '../contexts/AuthContext';
import { radius, roleStyles, spacing } from '../theme';

/**
 * Which account you are signed in as.
 *
 * It reads the role straight from AuthContext rather than taking a prop, so there
 * is no way for a screen to render a badge that disagrees with the session behind
 * it -- the failure mode worth designing out is a stale HOST badge on a client's
 * screen, which would be worse than no badge at all.
 *
 * Icon as well as colour, because green-versus-navy alone is not a distinction
 * everyone can see, and this badge's whole job is being unambiguous.
 *
 * Renders nothing when logged out. The tab bar does not exist then either.
 */
export default function RoleBadge() {
    const { user } = useAuth();

    if (!user?.role) {
        return null;
    }

    const role = roleStyles[user.role] ?? roleStyles.CLIENT;

    return (
        <View
            style={[styles.pill, { backgroundColor: role.bg }]}
            accessibilityLabel={`Signed in as ${user.username}, ${role.label.toLowerCase()}`}
        >
            <Ionicons name={role.icon} size={12} color={role.fg} />
            <Text style={[styles.text, { color: role.fg }]}>{role.label}</Text>
        </View>
    );
}

const styles = StyleSheet.create({
    pill: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 4,
        paddingHorizontal: spacing.sm + 2,
        paddingVertical: 4,
        borderRadius: radius.pill,
    },
    text: { fontSize: 11, fontWeight: '700', letterSpacing: 0.5 },
});
