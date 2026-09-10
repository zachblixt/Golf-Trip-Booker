import { useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';

import BackLink from '../../../components/BackLink';
import ErrorList from '../../../components/ErrorList';
import RequestForm from '../../../components/RequestForm';
import * as destinationService from '../../../services/destinationService';
import * as requestService from '../../../services/requestService';
import { ApiError } from '../../../services/api';
import { goBack } from '../../../utils/nav';
import { colors, spacing, type } from '../../../theme';

/**
 * Post-MVP: edit a request that is still pending.
 *
 * The server refuses this once a request has moved -- ownership and PENDING are
 * both checked in TripRequestService -- so the worst a stale screen can do is show
 * an error, not corrupt a booked trip.
 */
export default function EditRequest() {
    const { requestId } = useLocalSearchParams();
    const router = useRouter();

    const [trip, setTrip] = useState(null);
    const [destinations, setDestinations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errors, setErrors] = useState([]);
    const [busy, setBusy] = useState(false);

    // Loaded once, not on focus: reloading would discard whatever is half-typed.
    useEffect(() => {
        let cancelled = false;

        Promise.all([requestService.findById(requestId), destinationService.findAll()])
            .then(([found, allDestinations]) => {
                if (cancelled) return;
                setTrip(found);
                setDestinations(allDestinations);
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
    }, [requestId]);

    async function handleSubmit(payload) {
        setErrors([]);
        setBusy(true);

        try {
            await requestService.update(requestId, payload);
            goBack(router, '/trips');
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not save your changes.']);
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
                    <BackLink onPress={() => goBack(router, '/trips')} label="Back" />
                    <ErrorList errors={errors} />
                </View>
            </SafeAreaView>
        );
    }

    /*
     * The form holds text, the API sends numbers and nulls. Converting here keeps
     * RequestForm ignorant of where its values came from, which is what lets New
     * Request and this screen share it.
     */
    const initialValues = {
        destinationId: trip.destination?.destinationId ?? null,
        // Only one of these is ever set. A request for somewhere new stays editable
        // as text, so a typo in the town name is fixable before a host picks it up.
        requestedPlace: trip.requestedPlace ?? '',
        budgetPerPlayer: String(trip.budgetPerPlayer ?? ''),
        playerCount: String(trip.playerCount ?? ''),
        roundsRequested: String(trip.roundsRequested ?? ''),
        nights: String(trip.nights ?? ''),
        earliestStart: trip.earliestStart ?? '',
        latestStart: trip.latestStart ?? '',
        notes: trip.notes ?? '',
    };

    return (
        <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
            <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
                <BackLink onPress={() => goBack(router, '/trips')} label="Edit Request" />
                <Text style={[type.muted, styles.tagline]}>
                    Nobody has booked this yet, so you can still change it.
                </Text>

                <RequestForm
                    destinations={destinations}
                    initialValues={initialValues}
                    submitLabel="Save Changes"
                    onSubmit={handleSubmit}
                    busy={busy}
                    errors={errors}
                />
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
    tagline: { marginBottom: spacing.lg },
});
