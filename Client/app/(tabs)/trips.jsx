import { useCallback, useState } from 'react';
import { ActivityIndicator, FlatList, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';

import Card from '../../components/Card';
import ScreenTitle from '../../components/ScreenTitle';
import EmptyState from '../../components/EmptyState';
import ErrorList from '../../components/ErrorList';
import StatusPill from '../../components/StatusPill';
import * as requestService from '../../services/requestService';
import { ApiError } from '../../services/api';
import { colors, spacing, type } from '../../theme';
import { money, summaryLine } from '../../utils/format';

/** Wireframe 03. */
export default function Trips() {
    const router = useRouter();

    const [trips, setTrips] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [errors, setErrors] = useState([]);

    const load = useCallback(async () => {
        try {
            setTrips(await requestService.findMine());
            setErrors([]);
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not load your trips.']);
        }
    }, []);

    // On focus, not just on mount: submitting a request and cancelling one both
    // navigate back here, and a stale list is worse than a slow one.
    useFocusEffect(
        useCallback(() => {
            let cancelled = false;
            load().finally(() => {
                if (!cancelled) setLoading(false);
            });
            return () => {
                cancelled = true;
            };
        }, [load])
    );

    async function handleRefresh() {
        setRefreshing(true);
        await load();
        setRefreshing(false);
    }

    if (loading) {
        return (
            <SafeAreaView style={styles.centre} edges={['top']}>
                <ActivityIndicator size="large" color={colors.primary} />
            </SafeAreaView>
        );
    }

    return (
        <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
            <FlatList
                data={trips}
                keyExtractor={(item) => String(item.requestId)}
                contentContainerStyle={styles.content}
                refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
                ListHeaderComponent={
                    <View>
                        <ScreenTitle>My Trips</ScreenTitle>
                        <View style={styles.errorSlot}>
                            <ErrorList errors={errors} />
                        </View>
                    </View>
                }
                ListEmptyComponent={
                    errors.length > 0 ? null : (
                        <EmptyState
                            title="No trips yet"
                            message="Start on the New tab. A host picks it up from there."
                        />
                    )
                }
                renderItem={({ item }) => (
                    <Card onPress={() => router.push(`/trip/${item.requestId}`)}>
                        <View style={styles.cardHead}>
                            <Text style={type.heading}>{item.placeLabel ?? item.destination?.name}</Text>
                            <StatusPill status={item.status} />
                        </View>

                        <Text style={[type.muted, styles.line]}>
                            {summaryLine([
                                `${money(item.budgetPerPlayer)} pp`,
                                `${item.playerCount} ${item.playerCount === 1 ? 'player' : 'players'}`,
                            ])}
                        </Text>
                        <Text style={type.muted}>
                            {summaryLine([
                                `${item.roundsRequested} ${item.roundsRequested === 1 ? 'round' : 'rounds'}`,
                                `${item.nights} ${item.nights === 1 ? 'night' : 'nights'}`,
                            ])}
                        </Text>
                    </Card>
                )}
            />
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    safe: { flex: 1, backgroundColor: colors.bg },
    centre: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: colors.bg,
    },
    content: { padding: spacing.lg, flexGrow: 1 },
    errorSlot: { marginTop: spacing.md },
    cardHead: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: spacing.sm,
    },
    line: { marginBottom: 2 },
});
