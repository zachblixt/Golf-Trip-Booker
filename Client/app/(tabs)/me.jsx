import { StyleSheet, Text, View } from 'react-native';

import Button from '../../components/Button';
import Screen from '../../components/Screen';
import ScreenTitle from '../../components/ScreenTitle';
import { useAuth } from '../../contexts/AuthContext';
import { BASE_URL } from '../../services/api';
import { colors, radius, spacing, type } from '../../theme';

export default function Me() {
    const { user, isHost, logout } = useAuth();

    return (
        <Screen>
            <ScreenTitle>Me</ScreenTitle>

            {/*
              * No role pill in this card any more -- ScreenTitle renders one two lines
              * above it, and the same fact twice on one screen reads as two facts.
              */}
            <View style={styles.card}>
                <Text style={type.heading}>{user?.username}</Text>
                <Text style={[type.muted, styles.email]}>{user?.email}</Text>
                <Text style={[type.muted, styles.role]}>
                    {isHost
                        ? 'You book and decline other people’s requests.'
                        : 'You ask for trips. A host books them.'}
                </Text>
            </View>

            {/*
              * The address the app actually resolved, shown rather than hidden. When
              * a request fails on a phone this is the first thing worth knowing, and
              * digging it out of a bundler log is miserable.
              */}
            <View style={styles.card}>
                <Text style={type.label}>CONNECTED TO</Text>
                <Text style={styles.code}>{BASE_URL}</Text>
            </View>

            <View style={styles.actions}>
                <Button title="Log Out" variant="danger" onPress={logout} />
                <Text style={type.muted}>
                    Logging out deletes the token from this device. Nothing is stored on the server.
                </Text>
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
    email: { marginTop: spacing.xs },
    role: { marginTop: spacing.sm, lineHeight: 18 },

    code: { marginTop: spacing.xs, fontFamily: 'monospace', fontSize: 13, color: colors.primary },

    actions: { marginTop: 'auto', paddingTop: spacing.xl },
});
