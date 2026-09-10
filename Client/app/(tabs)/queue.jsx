import { useCallback, useState } from 'react';
import { ActivityIndicator, FlatList, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';

import Card from '../../components/Card';
import ScreenTitle from '../../components/ScreenTitle';
import EmptyState from '../../components/EmptyState';
import ErrorList from '../../components/ErrorList';
import StatusPill from '../../components/StatusPill';
import * as hostService from '../../services/hostService';
import { ApiError } from '../../services/api';
import { colors, spacing, type } from '../../theme';
import { money, shortDate, summaryLine } from '../../utils/format';

/** Wireframe 07. */
export default function Queue() {
    const router = useRouter();

    const [pending, setPending] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [errors, setErrors] = useState([]);

    const load = useCallback(async () => {
        try {
            setPending(await hostService.findPending());
            setErrors([]);
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not load the queue.']);
        }
    }, []);

    // Booking or declining navigates back here, and either one removes a row.
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
                data={pending}
                keyExtractor={(item) => String(item.requestId)}
                contentContainerStyle={styles.content}
                refreshControl={<RefreshControl refreshing={refreshing} onRefresh={handleRefresh} />}
                ListHeaderComponent={
                    <View style={styles.header}>
                        <ScreenTitle>Request Queue</ScreenTitle>
                        <Text style={type.muted}>
                            {pending.length} waiting{pending.length > 0 ? ' · oldest first' : ''}
                        </Text>
                        <View style={styles.errorSlot}>
                            <ErrorList errors={errors} />
                        </View>
                    </View>
                }
                ListEmptyComponent={
                    errors.length > 0 ? null : (
                        <EmptyState
                            title="Nothing waiting"
                            message="Every request has been booked or declined."
                        />
                    )
                }
                renderItem={({ item }) => (
                    <Card onPress={() => router.push(`/host/${item.requestId}`)}>
                        <View style={styles.cardHead}>
                            <Text style={type.heading}>{item.placeLabel ?? item.destination?.name}</Text>
                            <StatusPill status={item.status} />
                        </View>

                        {/* Worth flagging in the queue, not only on the booking screen:
                            booking one of these adds a destination the whole app can
                            then request, so the host knows before they open it. */}
                        {item.destination == null && item.requestedPlace ? (
                            <Text style={styles.newSpot}>Somewhere new</Text>
                        ) : null}

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

                        {/* The client block only comes back on host routes -- a client
                            looking at their own list never receives another user's details. */}
                        <Text style={[type.muted, styles.from]}>
                            from {item.client?.username} · {shortDate(item.createdAt)}
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
    header: { marginBottom: spacing.sm },
    errorSlot: { marginTop: spacing.md },
    newSpot: {
        fontSize: 11,
        fontWeight: '700',
        letterSpacing: 0.6,
        textTransform: 'uppercase',
        color: colors.primary,
        marginBottom: spacing.xs,
    },
    cardHead: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: spacing.sm,
    },
    line: { marginBottom: 2 },
    from: { marginTop: spacing.sm },
});
