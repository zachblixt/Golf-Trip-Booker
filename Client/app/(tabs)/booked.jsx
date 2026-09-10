import { useCallback, useState } from 'react';
import { ActivityIndicator, FlatList, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';

import Button from '../../components/Button';
import Card from '../../components/Card';
import Field from '../../components/Field';
import StatusPill from '../../components/StatusPill';
import ScreenTitle from '../../components/ScreenTitle';
import EmptyState from '../../components/EmptyState';
import ErrorList from '../../components/ErrorList';
import * as hostService from '../../services/hostService';
import { ApiError } from '../../services/api';
import { colors, spacing, type } from '../../theme';
import { dateRange, moneyExact, summaryLine } from '../../utils/format';

/** Matches the server floor on a withdrawal note, so the button can say why it is off. */
const MIN_NOTE = 10;

/**
 * Everything the host has sent: proposals still waiting on a client, and trips those
 * clients accepted.
 *
 * Both, because a host action now produces a PROPOSED request rather than a BOOKED one.
 * A tab reading only BOOKED would lose a trip the moment it was sent and show it again
 * only if the client happened to accept -- the host would have no way to see what was
 * outstanding, and no way to pull anything back.
 *
 * Reads /host/request rather than /explore. Explore is anonymised -- no client, no
 * booking id -- so a row there has nothing to open. These rows do, which is what makes
 * correcting a booking reachable at all.
 */
export default function Booked() {
    const router = useRouter();

    const [trips, setTrips] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [errors, setErrors] = useState([]);

    // At most one card has its withdrawal note open at a time.
    const [withdrawingId, setWithdrawingId] = useState(null);
    const [note, setNote] = useState('');
    const [busy, setBusy] = useState(false);

    const load = useCallback(async () => {
        try {
            // Proposals first: they are the ones that might still need something doing.
            const [proposed, booked] = await Promise.all([
                hostService.findByStatus('PROPOSED'),
                hostService.findByStatus('BOOKED'),
            ]);
            setTrips([...proposed, ...booked]);
            setErrors([]);
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not load your trips.']);
        }
    }, []);

    async function handleWithdraw(requestId) {
        setErrors([]);
        setBusy(true);
        try {
            await hostService.withdrawProposal(requestId, note.trim());
            setWithdrawingId(null);
            setNote('');
            await load();
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not pull that back.']);
        } finally {
            setBusy(false);
        }
    }

    // Correcting a booking navigates back here, so the numbers must be re-read.
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
                    <View style={styles.header}>
                        <ScreenTitle>Sent</ScreenTitle>
                        <Text style={type.muted}>Tap a booked trip to correct it</Text>
                        <View style={styles.errorSlot}>
                            <ErrorList errors={errors} />
                        </View>
                    </View>
                }
                ListEmptyComponent={
                    errors.length > 0 ? null : (
                        <EmptyState
                            title="Nothing sent yet"
                            message="Trips you propose show up here while you wait on the client."
                        />
                    )
                }
                renderItem={({ item }) => (
                    // Only a booked trip can be corrected. A proposal is not yours to edit --
                    // the client is looking at those exact numbers deciding on them.
                    <Card onPress={item.status === 'BOOKED'
                        ? () => router.push(`/host/correct/${item.requestId}`)
                        : undefined}>
                        <View style={styles.cardHead}>
                            <Text style={type.heading}>{item.placeLabel ?? item.destination?.name}</Text>
                            <Text style={styles.cost}>{moneyExact(item.booking?.totalCost)}</Text>
                        </View>

                        <View style={styles.pillRow}>
                            <StatusPill
                                status={item.status}
                                label={item.status === 'PROPOSED' ? 'Waiting on client' : undefined}
                            />
                        </View>

                        <Text style={[type.muted, styles.line]}>
                            {summaryLine([
                                `${item.playerCount} ${item.playerCount === 1 ? 'player' : 'players'}`,
                                `${item.nights} ${item.nights === 1 ? 'night' : 'nights'}`,
                                dateRange(item.booking?.startDate, item.booking?.endDate),
                            ])}
                        </Text>

                        {item.booking?.courses?.length ? (
                            <Text style={type.muted}>{item.booking.courses.join(' · ')}</Text>
                        ) : null}

                        <Text style={[type.muted, styles.from]}>for {item.client?.username}</Text>

                        {item.status === 'PROPOSED' ? (
                            withdrawingId === item.requestId ? (
                                <View style={styles.withdraw}>
                                    <Field
                                        label="Why are you pulling this back?"
                                        value={note}
                                        onChangeText={setNote}
                                        placeholder="The lodge released our block."
                                        multiline
                                    />
                                    {/*
                                      * Same floor and same live count as the client's
                                      * counter sheet. Whoever ends a proposal owes the
                                      * other side a sentence about it.
                                      */}
                                    <Text style={type.muted}>
                                        {note.trim().length < MIN_NOTE
                                            ? `${MIN_NOTE - note.trim().length} more characters`
                                            : 'They will see this on their trip.'}
                                    </Text>
                                    <Button
                                        title="Pull it back"
                                        variant="danger"
                                        onPress={() => handleWithdraw(item.requestId)}
                                        loading={busy}
                                        disabled={note.trim().length < MIN_NOTE}
                                    />
                                    <Button
                                        title="Never mind"
                                        variant="secondary"
                                        onPress={() => setWithdrawingId(null)}
                                        disabled={busy}
                                    />
                                </View>
                            ) : (
                                <View style={styles.withdraw}>
                                    <Button
                                        title="Withdraw this proposal"
                                        variant="secondary"
                                        onPress={() => {
                                            setNote('');
                                            setWithdrawingId(item.requestId);
                                        }}
                                        disabled={busy}
                                    />
                                </View>
                            )
                        ) : null}
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
    cardHead: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: spacing.sm,
    },
    cost: { fontSize: 15, fontWeight: '700', color: colors.money },
    line: { marginBottom: 2 },
    from: { marginTop: spacing.sm },
    pillRow: { flexDirection: 'row', marginBottom: spacing.sm },
    withdraw: { marginTop: spacing.md },
});
