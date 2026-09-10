import { ActivityIndicator, StyleSheet, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { Stack } from 'expo-router';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { AuthProvider, useAuth } from '../contexts/AuthContext';
import { colors } from '../theme';

export default function RootLayout() {
    return (
        <SafeAreaProvider>
            <AuthProvider>
                <StatusBar style="dark" />
                <RootNavigator />
            </AuthProvider>
        </SafeAreaProvider>
    );
}

/**
 * Stack.Protected is what actually gates the app. A guard that evaluates false
 * makes those screens unreachable, including by deep link -- unlike a redirect
 * inside a screen, which only runs once the screen has already mounted.
 *
 * The two guards are exact opposites, so exactly one branch is ever live.
 */
function RootNavigator() {
    const { user, initializing } = useAuth();

    // Nothing is rendered until the stored token has been checked. Without this the
    // login screen flashes for a moment on every launch of an already-signed-in app.
    if (initializing) {
        return (
            <View style={styles.booting}>
                <ActivityIndicator size="large" color={colors.primary} />
            </View>
        );
    }

    return (
        <Stack screenOptions={{ headerShown: false }}>
            <Stack.Screen name="index" />

            <Stack.Protected guard={!!user}>
                <Stack.Screen name="(tabs)" />
                {/* Sits outside (tabs) so it pushes over the tab bar as its own screen,
                    the way a detail view should. Inside the guard, so a deep link to
                    someone's trip is unreachable when logged out. */}
                <Stack.Screen name="trip/[requestId]" />
                <Stack.Screen name="trip/edit/[requestId]" />
                {/* The host screens. Reachable only from the host tabs, and the
                    server rejects them for a client regardless of how they are reached. */}
                <Stack.Screen name="host/[requestId]" />
                <Stack.Screen name="host/correct/[requestId]" />
            </Stack.Protected>

            <Stack.Protected guard={!user}>
                <Stack.Screen name="(auth)" />
            </Stack.Protected>
        </Stack>
    );
}

const styles = StyleSheet.create({
    booting: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: colors.bg,
    },
});
