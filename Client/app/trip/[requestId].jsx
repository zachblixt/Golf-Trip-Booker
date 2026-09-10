import { useCallback, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';

import BackLink from '../../components/BackLink';
import Button from '../../components/Button';
import ErrorList from '../../components/ErrorList';
import Field from '../../components/Field';
import StatusPill from '../../components/StatusPill';
import * as requestService from '../../services/requestService';
import { ApiError } from '../../services/api';
import { goBack } from '../../utils/nav';
import { colors, radius, spacing, type } from '../../theme';
import { dateRange, money, moneyExact, shortDate, summaryLine } from '../../utils/format';
import { templateFromOwnTrip, toQuery } from '../../utils/tripTemplate';

/*
 * The five things that can be wrong with a trip. The server takes the value; the label
 * is what the client taps. PRICE is one of five on purpose -- the flow this replaced
 * offered a single "not at that price" button, which quietly asserted that cost was the
 * only thing a group could object to. It is usually not even the most common one.
 */
const COUNTER_REASONS = [
    { value: 'PRICE', label: 'Too expensive' },
    { value: 'DATES', label: 'Dates do not work' },
    { value: 'COURSES', label: 'Wrong courses' },
    { value: 'LODGING', label: 'Lodging' },
    { value: 'OTHER', label: 'Something else' },
];

/** The server enforces this too. Here it is so the button can say why it is disabled. */
const MIN_NOTE = 10;

/** Wireframes 04 (booked) and 05 (pending), plus the declined and cancelled cases. */
export default function TripDetail() {
    const { requestId } = useLocalSearchParams();
    const router = useRouter();

    const [trip, setTrip] = useState(null);
    const [loading, setLoading] = useState(true);
    const [errors, setErrors] = useState([]);
    const [confirmingCancel, setConfirmingCancel] = useState(false);
    const [busy, setBusy] = useState(false);

    const [counteringOpen, setCounteringOpen] = useState(false);
    const [counterReason, setCounterReason] = useState(null);
    const [counterNote, setCounterNote] = useState('');

    useFocusEffect(
        useCallback(() => {
            let cancelled = false;

            requestService
                .findById(requestId)
                .then((found) => {
                    if (!cancelled) setTrip(found);
                })
                .catch((error) => {
                    if (!cancelled) {
                        setErrors(error instanceof ApiError ? error.messages : ['Could not load this trip.']);
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

    async function handleCancel() {
        setBusy(true);
        try {
            await requestService.cancel(requestId);
            goBack(router, '/trips');
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not cancel this request.']);
            setConfirmingCancel(false);
        } finally {
            setBusy(false);
        }
    }

    async function handleAccept() {
        setBusy(true);
        try {
            await requestService.acceptProposal(requestId);
            goBack(router, '/trips');
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not accept this proposal.']);
        } finally {
            setBusy(false);
        }
    }

    async function handleCounter() {
        setErrors([]);
        setBusy(true);
        try {
            await requestService.counterProposal(requestId, counterReason, counterNote.trim());
            goBack(router, '/trips');
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not send this back.']);
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

    // A 403 for someone else's request lands here. The server's own message is the
    // honest thing to show, rather than pretending the trip does not exist.
    if (!trip) {
        return (
            <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
                <View style={styles.body}>
                    <BackLink onPress={() => goBack(router, '/trips')} label="Back" />
                    <ErrorList errors={errors} />
                </View>
            </SafeAreaView>
        );
    }

    const booking = trip.booking;

    return (
        <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
            <ScrollView contentContainerStyle={styles.body}>
                <BackLink onPress={() => goBack(router, '/trips')} label={trip.placeLabel ?? trip.destination?.name} />

                <View style={styles.statusRow}>
                    <StatusPill status={trip.status} />
                    <Text style={type.muted}>Requested {shortDate(trip.createdAt)}</Text>
                </View>

                <ErrorList errors={errors} />

                <Section label="YOU ASKED FOR">
                    <Text style={type.body}>
                        {money(trip.budgetPerPlayer)} per player ·{' '}
                        {trip.playerCount} {trip.playerCount === 1 ? 'player' : 'players'}
                    </Text>
                    <Text style={type.body}>
                        {summaryLine([
                            `${trip.roundsRequested} ${trip.roundsRequested === 1 ? 'round' : 'rounds'}`,
                            `${trip.nights} ${trip.nights === 1 ? 'night' : 'nights'}`,
                        ])}
                    </Text>
                    {trip.earliestStart ? (
                        <Text style={type.body}>
                            {trip.earliestStart === trip.latestStart
                                ? `Starting ${shortDate(trip.earliestStart)}`
                                : `Can start ${dateRange(trip.earliestStart, trip.latestStart)}`}
                        </Text>
                    ) : null}
                    {trip.notes ? <Text style={[type.muted, styles.notes]}>{trip.notes}</Text> : null}
                </Section>

                {trip.status === 'BOOKED' && booking ? (
                    <>
                        <Section label="YOUR HOST BOOKED — COURSES">
                            {booking.courses.map((course, index) => (
                                <Text key={index} style={type.body}>
                                    • {course}
                                </Text>
                            ))}
                        </Section>

                        {booking.lodging ? (
                            <Section label="STAYING">
                                <Text style={type.body}>{booking.lodging}</Text>
                                <Text style={type.muted}>
                                    {dateRange(booking.startDate, booking.endDate)}
                                </Text>
                            </Section>
                        ) : null}

                        {booking.itinerary ? (
                            <Section label="ITINERARY">
                                <Text style={type.body}>{booking.itinerary}</Text>
                            </Section>
                        ) : null}

                        <View style={styles.total}>
                            <Text style={type.body}>Total</Text>
                            <Text style={styles.totalValue}>{moneyExact(booking.totalCost)}</Text>
                        </View>

                        {/* The likeliest copy of all: the trip you already know you liked. */}
                        <Button
                            title="Book this again"
                            variant="secondary"
                            onPress={() => router.push(`/new?${toQuery(templateFromOwnTrip(trip))}`)}
                        />
                    </>
                ) : null}

                {trip.status === 'PROPOSED' && booking ? (
                    <>
                        {trip.proposalCount > 1 ? (
                            <Text style={[type.muted, styles.round]}>
                                Proposal {trip.proposalCount}. You have sent{' '}
                                {trip.proposalCount - 1} back so far.
                            </Text>
                        ) : null}

                        <Section label="YOUR PROPOSED TRIP — COURSES">
                            {booking.courses.map((course, index) => (
                                <Text key={index} style={type.body}>
                                    • {course}
                                </Text>
                            ))}
                        </Section>

                        {booking.lodging ? (
                            <Section label="STAYING">
                                <Text style={type.body}>{booking.lodging}</Text>
                                <Text style={type.muted}>
                                    {dateRange(booking.startDate, booking.endDate)}
                                </Text>
                            </Section>
                        ) : null}

                        <View style={styles.total}>
                            <Text style={type.body}>Your host is asking</Text>
                            <Text style={styles.totalValue}>{moneyExact(booking.totalCost)}</Text>
                        </View>

                        {/*
                          * Conditional now. A proposal used to exist only because it was
                          * over budget, so this paragraph could be stated flatly; every
                          * trip is proposed today and most of them come in under. Per
                          * player stays, because it is the number a group argues about.
                          */}
                        {booking.totalCost > trip.budgetCeiling ? (
                            <View style={styles.note}>
                                <Text style={type.muted}>
                                    {money(booking.totalCost - trip.budgetCeiling)} over the{' '}
                                    {money(trip.budgetCeiling)} you budgeted, about{' '}
                                    {money((booking.totalCost - trip.budgetCeiling) / trip.playerCount)}{' '}
                                    more each.
                                </Text>
                            </View>
                        ) : null}

                        {!counteringOpen ? (
                            <>
                                <View style={styles.actions}>
                                    <Button
                                        title="Accept and book it"
                                        onPress={handleAccept}
                                        loading={busy}
                                    />
                                    <Button
                                        title="Ask for changes"
                                        variant="secondary"
                                        onPress={() => setCounteringOpen(true)}
                                        disabled={busy}
                                    />
                                </View>

                                <Text style={[type.muted, styles.notes]}>
                                    Asking for changes puts your request back in the queue for
                                    another try. It does not cancel the trip.
                                </Text>
                            </>
                        ) : (
                            <View style={styles.counter}>
                                <Text style={styles.counterLabel}>WHAT DOES NOT WORK?</Text>

                                <View style={styles.chips}>
                                    {COUNTER_REASONS.map((option) => (
                                        <Pressable
                                            key={option.value}
                                            onPress={() => setCounterReason(option.value)}
                                            style={[
                                                styles.chip,
                                                counterReason === option.value && styles.chipOn,
                                            ]}
                                        >
                                            <Text
                                                style={[
                                                    type.muted,
                                                    counterReason === option.value && styles.chipOnText,
                                                ]}
                                            >
                                                {option.label}
                                            </Text>
                                        </Pressable>
                                    ))}
                                </View>

                                <Field
                                    label="Tell your host what to change"
                                    value={counterNote}
                                    onChangeText={setCounterNote}
                                    placeholder="We can't do that weekend — anything after the 12th works."
                                    multiline
                                />

                                {/*
                                  * The count is here so the requirement is visible while
                                  * they type rather than arriving as a server error after
                                  * they have already written something and pressed send.
                                  */}
                                <Text style={type.muted}>
                                    {counterNote.trim().length < MIN_NOTE
                                        ? `${MIN_NOTE - counterNote.trim().length} more characters`
                                        : 'Your host will see this with the request.'}
                                </Text>

                                <View style={styles.actions}>
                                    <Button
                                        title="Send it back"
                                        onPress={handleCounter}
                                        loading={busy}
                                        disabled={
                                            counterReason === null
                                            || counterNote.trim().length < MIN_NOTE
                                        }
                                    />
                                    <Button
                                        title="Never mind"
                                        variant="secondary"
                                        onPress={() => setCounteringOpen(false)}
                                        disabled={busy}
                                    />
                                </View>
                            </View>
                        )}
                    </>
                ) : null}

                {trip.status === 'PENDING' ? (
                    <>
                        {/*
                          * A trip can arrive back here two ways: never proposed on, or
                          * proposed on and pulled back. Those look identical without this,
                          * and the second one is a trip the client was about to decide on
                          * that reverted while they were not looking.
                          */}
                        {booking?.withdrawnAt ? (
                            <View style={styles.note}>
                                <Text style={type.body}>
                                    Your host pulled their proposal back.
                                </Text>
                                <Text style={[type.body, styles.quote]}>
                                    “{booking.withdrawNote}”
                                </Text>
                                <Text style={type.muted}>
                                    Your request is back in the queue. They can send another.
                                </Text>
                            </View>
                        ) : (
                            <View style={styles.note}>
                                <Text style={type.muted}>
                                    Waiting on a host to propose a trip.
                                </Text>
                            </View>
                        )}

                        <View style={styles.actions}>
                            {/*
                              * A two-step confirm rather than Alert.alert, which does not
                              * render under react-native-web -- the cancel would silently
                              * do nothing when testing in a browser.
                              */}
                            {!confirmingCancel ? (
                                <Button
                                    title="Edit Request"
                                    variant="secondary"
                                    onPress={() => router.push(`/trip/edit/${requestId}`)}
                                />
                            ) : null}

                            {confirmingCancel ? (
                                <>
                                    <Button
                                        title="Yes, cancel this request"
                                        variant="danger"
                                        onPress={handleCancel}
                                        loading={busy}
                                    />
                                    <Button
                                        title="Keep it"
                                        variant="secondary"
                                        onPress={() => setConfirmingCancel(false)}
                                        disabled={busy}
                                    />
                                </>
                            ) : (
                                <Button
                                    title="Cancel Request"
                                    variant="danger"
                                    onPress={() => setConfirmingCancel(true)}
                                />
                            )}
                        </View>
                    </>
                ) : null}

                {trip.status === 'DECLINED' ? (
                    <Section label="WHY IT WAS DECLINED">
                        <Text style={type.body}>{trip.declineReason}</Text>
                    </Section>
                ) : null}

                {trip.status === 'CANCELLED' ? (
                    <View style={styles.note}>
                        <Text style={type.muted}>You cancelled this request.</Text>
                    </View>
                ) : null}
            </ScrollView>
        </SafeAreaView>
    );
}

function Section({ label, children }) {
    return (
        <View style={styles.section}>
            <Text style={type.label}>{label}</Text>
            <View style={styles.sectionBody}>{children}</View>
        </View>
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


    statusRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.lg },

    section: { marginBottom: spacing.lg },
    sectionBody: { marginTop: spacing.xs, gap: 2 },
    notes: { marginTop: spacing.sm, fontStyle: 'italic' },

    note: {
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.md,
        padding: spacing.md,
        marginBottom: spacing.lg,
    },

    total: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        borderTopWidth: 1,
        borderTopColor: colors.border,
        paddingTop: spacing.md,
        marginBottom: spacing.lg,
    },
    totalValue: { fontSize: 22, fontWeight: '700', color: colors.text },

    actions: { marginTop: spacing.sm },

    counter: {
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.md,
        padding: spacing.md,
        marginTop: spacing.sm,
    },
    counterLabel: { ...type.label, marginBottom: spacing.sm },
    chips: {
        flexDirection: 'row',
        flexWrap: 'wrap',
        gap: spacing.sm,
        marginBottom: spacing.md,
    },
    chip: {
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.pill,
        paddingVertical: spacing.xs,
        paddingHorizontal: spacing.md,
    },
    chipOn: { backgroundColor: colors.primary, borderColor: colors.primary },
    chipOnText: { color: colors.primaryText, fontWeight: '600' },
    quote: { marginVertical: spacing.sm },
    round: { marginBottom: spacing.md },
});
