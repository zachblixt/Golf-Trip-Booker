import { useCallback, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';

import BackLink from '../../components/BackLink';
import BookingForm, { EMPTY_BOOKING } from '../../components/BookingForm';
import Button from '../../components/Button';
import ErrorList from '../../components/ErrorList';
import RequestSummary from '../../components/RequestSummary';
import StatusPill from '../../components/StatusPill';
import Field from '../../components/Field';
import * as hostService from '../../services/hostService';
import { ApiError } from '../../services/api';
import { goBack } from '../../utils/nav';
import { colors, radius, spacing, type } from '../../theme';
import { dateRange, money, shortDate } from '../../utils/format';

/** How a client's five-value reason reads to the host who has to act on it. */
const REASON_LABELS = {
    PRICE: 'the price',
    DATES: 'the dates',
    COURSES: 'the courses',
    LODGING: 'the lodging',
    OTHER: 'something else',
};

/**
 * What a reason means the host has to change, so the form can leave that gap open.
 * OTHER clears nothing: the note is the only thing that says what went wrong.
 */
const CLEARED_BY_REASON = {
    PRICE: ['totalCost'],
    DATES: ['startDate', 'endDate'],
    COURSES: ['courses'],
    LODGING: ['lodging'],
    OTHER: [],
};

/**
 * The last proposal, ready to edit, minus whatever was wrong with it.
 *
 * Prefilling everything would invite the host to send back the exact price a group has
 * just refused. Clearing only the objected-to field leaves one conspicuous gap sitting
 * where the problem is, and still saves retyping the four fields that were fine.
 *
 * Courses arrive as an array and the field is newline-delimited text, which is the same
 * translation Booking.courseList() does in the other direction on the server.
 */
function seedFrom(proposal) {
    if (!proposal) {
        return { values: EMPTY_BOOKING, cleared: null };
    }

    const values = {
        courses: (proposal.courses ?? []).join('\n'),
        lodging: proposal.lodging ?? '',
        startDate: proposal.startDate ?? '',
        endDate: proposal.endDate ?? '',
        totalCost: proposal.totalCost != null ? String(proposal.totalCost) : '',
        itinerary: proposal.itinerary ?? '',
    };

    // A withdrawal carries no reason. Nothing is cleared, because the host pulled that
    // one back themselves and already knows what they meant to change.
    const cleared = proposal.counterReason ? CLEARED_BY_REASON[proposal.counterReason] ?? [] : [];
    cleared.forEach((field) => {
        values[field] = '';
    });

    return { values, cleared: cleared.length > 0 ? proposal.counterReason : null };
}

/**
 * Why there is no form, per status.
 *
 * Only a PENDING request can be proposed on -- propose() refuses anything else. The
 * screen used to render the form regardless, so the first thing telling a host they
 * were on a dead request was a server error after they had filled the whole thing in.
 */
const CLOSED_STATES = {
    PROPOSED: {
        pill: 'Waiting on client',
        line: 'Your proposal is with them. You can pull it back from the Sent tab.',
    },
    BOOKED: {
        pill: undefined,
        line: 'They accepted. Corrections to a booked trip are made from the Sent tab.',
    },
    DECLINED: {
        pill: undefined,
        line: 'You turned this one down, and the client was told why.',
    },
    CANCELLED: {
        pill: undefined,
        line: 'The client withdrew this request. Nothing left to arrange.',
    },
};

/** Wireframe 08, now a proposal rather than a booking. */
export default function BookTrip() {
    const { requestId } = useLocalSearchParams();
    const router = useRouter();

    const [trip, setTrip] = useState(null);
    const [loading, setLoading] = useState(true);
    const [errors, setErrors] = useState([]);
    const [busy, setBusy] = useState(false);

    const [decliningOpen, setDecliningOpen] = useState(false);
    const [reason, setReason] = useState('');

    /*
     * Every proposal ever made on this request, newest first. A request that comes back
     * to the queue looks identical to one that has never been seen, so without this the
     * host's most likely move is to propose the same trip that was just turned down.
     */
    const [proposals, setProposals] = useState([]);
    const [showAllHistory, setShowAllHistory] = useState(false);

    useFocusEffect(
        useCallback(() => {
            let cancelled = false;

            /*
             * Both together, and nothing renders until both land. The booking form is
             * seeded from the history, and BookingForm captures initialValues once with
             * useState -- a prefill arriving after it mounts is silently ignored.
             *
             * A failed history load is still not fatal. It falls back to an empty list,
             * which gives an empty form, which is where this screen used to start anyway.
             */
            Promise.all([
                hostService.findRequest(requestId),
                hostService.findProposals(requestId).catch(() => []),
            ])
                .then(([found, history]) => {
                    if (cancelled) return;
                    setTrip(found);
                    setProposals(history);
                })
                .catch((error) => {
                    if (!cancelled) {
                        setErrors(error instanceof ApiError ? error.messages : ['Could not load this request.']);
                    }
                })
                .finally(() => {
                    if (!cancelled) setLoading(false);
                });

            return () => {
                cancelled = true;
            };
        }, [requestId])
    );

    async function handlePropose(payload) {
        setErrors([]);
        setBusy(true);
        try {
            await hostService.propose(requestId, payload);
            goBack(router, '/queue');
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not send this proposal.']);
        } finally {
            setBusy(false);
        }
    }

    async function handleDecline() {
        if (!reason.trim()) {
            setErrors(['Give the client a reason.']);
            return;
        }

        setErrors([]);
        setBusy(true);
        try {
            await hostService.decline(requestId, reason.trim());
            goBack(router, '/queue');
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not decline this request.']);
        } finally {
            setBusy(false);
        }
    }

    if (loading) {
        return (
            <SafeAreaView style={styles.centre}>
                <ActivityIndicator size="large" color={colors.primary} />
            </SafeAreaView>
        );
    }

    if (!trip) {
        return (
            <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
                <View style={styles.body}>
                    <BackLink onPress={() => goBack(router, '/queue')} label="Back" />
                    <ErrorList errors={errors} />
                </View>
            </SafeAreaView>
        );
    }

    // Only the ones that ended. A live proposal is not history, and there is at most one.
    const answered = proposals.filter((proposal) => proposal.counteredAt || proposal.withdrawnAt);

    /*
     * The loop is unbounded on purpose -- a negotiation ends when somebody decides it
     * has, and both sides can already walk away. What is bounded is the reading of it:
     * three attempts is enough to see the pattern, and the rest is one tap away.
     */
    // Newest first from the server, so the most recent one that ended seeds the form.
    const lastAnswered = answered[0] ?? null;
    const { values: seedValues, cleared } = seedFrom(lastAnswered);

    const closed = CLOSED_STATES[trip.status];

    const HISTORY_PREVIEW = 3;
    const shown = showAllHistory ? answered : answered.slice(0, HISTORY_PREVIEW);
    const hidden = answered.length - shown.length;

    return (
        <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
            <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
                <BackLink onPress={() => goBack(router, '/queue')} label="Propose a Trip" />

                <RequestSummary trip={trip} />

                {answered.length > 0 ? (
                    <View style={styles.history}>
                        <Text style={styles.historyLabel}>
                            WHAT THEY SAID BEFORE · {answered.length}{' '}
                            {answered.length === 1 ? 'ATTEMPT' : 'ATTEMPTS'}
                        </Text>
                        {shown.map((proposal) => (
                            <View key={proposal.bookingId} style={styles.attempt}>
                                <Text style={type.muted}>
                                    You proposed {money(proposal.totalCost)} ·{' '}
                                    {dateRange(proposal.startDate, proposal.endDate)} ·{' '}
                                    {shortDate(proposal.bookedAt)}
                                </Text>
                                {proposal.counteredAt ? (
                                    <>
                                        <Text style={styles.verdict}>
                                            Turned down — {REASON_LABELS[proposal.counterReason]}
                                        </Text>
                                        <Text style={type.body}>“{proposal.counterNote}”</Text>
                                    </>
                                ) : (
                                    <Text style={type.muted}>You withdrew this one.</Text>
                                )}
                            </View>
                        ))}

                        {hidden > 0 ? (
                            <Button
                                title={`Show ${hidden} earlier ${hidden === 1 ? 'attempt' : 'attempts'}`}
                                variant="secondary"
                                onPress={() => setShowAllHistory(true)}
                            />
                        ) : null}
                    </View>
                ) : null}

                {trip.status !== 'PENDING' ? (
                    <View style={styles.closed}>
                        <View style={styles.closedPill}>
                            <StatusPill status={trip.status} label={closed?.pill} />
                        </View>
                        <Text style={type.body}>{closed?.line ?? 'This request is closed.'}</Text>
                        {trip.status === 'DECLINED' && trip.declineReason ? (
                            <Text style={[type.muted, styles.closedReason]}>
                                You said: “{trip.declineReason}”
                            </Text>
                        ) : null}
                        <View style={styles.closedAction}>
                            <Button
                                title="Back to the queue"
                                variant="secondary"
                                onPress={() => goBack(router, '/queue')}
                            />
                        </View>
                    </View>
                ) : null}

                {trip.status === 'PENDING' && answered.length > 0 ? (
                    <Text style={[type.muted, styles.round]}>
                        This will be proposal {answered.length + 1}.
                        {lastAnswered ? (
                            cleared
                                ? ` Filled in from the last one, with ${REASON_LABELS[cleared]} left blank.`
                                : ' Filled in from the one you pulled back.'
                        ) : ''}
                    </Text>
                ) : null}

                {trip.status === 'PENDING' ? (
                <BookingForm
                    request={trip}
                    initialValues={seedValues}
                    submitLabel="Send Proposal"
                    onSubmit={handlePropose}
                    busy={busy}
                    errors={errors}
                >
                    {decliningOpen ? (
                        <View style={styles.decline}>
                            <Field
                                label="Reason"
                                value={reason}
                                onChangeText={setReason}
                                placeholder="No tee times that week."
                                multiline
                            />
                            <Button title="Send Decline" variant="danger" onPress={handleDecline} loading={busy} />
                            <Button
                                title="Never mind"
                                variant="secondary"
                                onPress={() => setDecliningOpen(false)}
                                disabled={busy}
                            />
                        </View>
                    ) : (
                        <Button
                            title="Decline with a reason"
                            variant="secondary"
                            onPress={() => setDecliningOpen(true)}
                            disabled={busy}
                        />
                    )}
                </BookingForm>
                ) : null}
            </ScrollView>
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
    body: { padding: spacing.lg, paddingBottom: spacing.xl },


    decline: { marginTop: spacing.md },

    history: {
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.md,
        padding: spacing.md,
        marginBottom: spacing.lg,
    },
    historyLabel: {
        ...type.label,
        marginBottom: spacing.sm,
    },
    attempt: { marginBottom: spacing.md },
    verdict: { ...type.body, fontWeight: '600', marginTop: spacing.xs },
    round: { marginBottom: spacing.md },

    closed: {
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.md,
        padding: spacing.md,
        marginBottom: spacing.lg,
    },
    closedPill: { flexDirection: 'row', marginBottom: spacing.sm },
    closedReason: { marginTop: spacing.sm, fontStyle: 'italic' },
    closedAction: { marginTop: spacing.md },
});
