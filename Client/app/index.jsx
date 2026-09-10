import { Redirect } from 'expo-router';

import { useAuth } from '../contexts/AuthContext';

/**
 * The entry route. It renders nothing -- it only decides where "/" means.
 *
 * A host has no Trips tab, so sending one to /trips would land them on a screen
 * their own tab bar does not show. Role decides the landing screen.
 */
export default function Index() {
    const { user, isHost, initializing } = useAuth();

    if (initializing) {
        return null;
    }

    if (!user) {
        return <Redirect href="/login" />;
    }

    return <Redirect href={isHost ? '/queue' : '/trips'} />;
}
