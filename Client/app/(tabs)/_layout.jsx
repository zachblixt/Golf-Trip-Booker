import Ionicons from '@expo/vector-icons/Ionicons';
import { Tabs } from 'expo-router';

import { useAuth } from '../../contexts/AuthContext';
import { colors } from '../../theme';

/**
 * One tab bar, two shapes. A client gets New / Trips / Explore / Me; a host gets
 * Queue / Booked / Explore / Me.
 *
 * `href: null` hides a route from the bar without removing it, which is why all six
 * screens are declared here regardless of who is looking. Note this is presentation
 * only -- hiding the Queue tab is not what stops a client reaching host data. The
 * server does that, and would still 403 if someone typed the route by hand.
 */
export default function TabsLayout() {
    const { isHost } = useAuth();

    return (
        <Tabs
            screenOptions={{
                headerShown: false,
                tabBarActiveTintColor: colors.primary,
                tabBarInactiveTintColor: colors.muted,
                tabBarStyle: {
                    backgroundColor: colors.card,
                    borderTopColor: colors.border,
                },
                tabBarLabelStyle: { fontSize: 11 },
            }}
        >
            {/* ---- client ---- */}
            <Tabs.Screen
                name="new"
                options={{
                    title: 'New',
                    href: isHost ? null : undefined,
                    tabBarIcon: ({ color, size }) => (
                        <Ionicons name="add-circle-outline" size={size} color={color} />
                    ),
                }}
            />
            <Tabs.Screen
                name="trips"
                options={{
                    title: 'Trips',
                    href: isHost ? null : undefined,
                    tabBarIcon: ({ color, size }) => (
                        <Ionicons name="golf-outline" size={size} color={color} />
                    ),
                }}
            />

            {/* ---- host ---- */}
            <Tabs.Screen
                name="queue"
                options={{
                    title: 'Queue',
                    href: isHost ? undefined : null,
                    tabBarIcon: ({ color, size }) => (
                        <Ionicons name="list-outline" size={size} color={color} />
                    ),
                }}
            />
            <Tabs.Screen
                name="booked"
                options={{
                    title: 'Sent',
                    href: isHost ? undefined : null,
                    tabBarIcon: ({ color, size }) => (
                        <Ionicons name="checkmark-done-outline" size={size} color={color} />
                    ),
                }}
            />

            {/* ---- both ---- */}
            <Tabs.Screen
                name="explore"
                options={{
                    title: 'Explore',
                    tabBarIcon: ({ color, size }) => (
                        <Ionicons name="compass-outline" size={size} color={color} />
                    ),
                }}
            />
            <Tabs.Screen
                name="me"
                options={{
                    title: 'Me',
                    tabBarIcon: ({ color, size }) => (
                        <Ionicons name="person-outline" size={size} color={color} />
                    ),
                }}
            />
        </Tabs>
    );
}
