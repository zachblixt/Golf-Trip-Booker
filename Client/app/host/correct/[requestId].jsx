import { useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';

import BackLink from '../../../components/BackLink';
import BookingForm from '../../../components/BookingForm';
import ErrorList from '../../../components/ErrorList';
import RequestSummary from '../../../components/RequestSummary';
import * as hostService from '../../../services/hostService';
import { ApiError } from '../../../services/api';
import { goBack } from '../../../utils/nav';
import { colors, spacing, type } from '../../../theme';

/**
 * Post-MVP: a host correcting a booking they already made.
 *
 * Keyed on requestId rather than bookingId because one call to
 * GET /host/request/{id} returns both -- the request, for its ceiling and round
 * count, and the booking nested inside it. Keying on the booking would mean two
 * fetches to fill one form.
 */
export default function CorrectBooking() {
    const { requestId } = useLocalSearchParams();
    const router = useRouter();

    const [trip, setTrip] = useState(null);
    const [loading, setLoading] = useState(true);
    const [errors, setErrors] = useState([]);
    const [busy, setBusy] = useState(false);

    // Loaded once, not on focus: reloading would discard whatever is half-typed.
    useEffect(() => {
        let cancelled = false;

        hostService
            .findRequest(requestId)
            .then((found) => {
                if (!cancelled) setTrip(found);
            })
            .catch((error) => {
                if (!cancelled) {
                    setErrors(error instanceof ApiError ? error.messages : ['Could not load this booking.']);
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
            await hostService.updateBooking(trip.booking.bookingId, payload);
            goBack(router, '/booked');
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not save the correction.']);
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

    // No booking means nothing to correct -- a request that was declined, cancelled,
    // or is still waiting. Say so rather than rendering an empty form.
    if (!trip || !trip.booking) {
        return (
            <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
                <View style={styles.body}>
                    <BackLink onPress={() => goBack(router, '/booked')} label="Back" />
                    <ErrorList
                        errors={errors.length > 0 ? errors : ['This request has no booking to correct.']}
                    />
                </View>
            </SafeAreaView>
        );
    }

    const booking = trip.booking;

    // BookingResponse sends courses as an array; the textarea wants the lines back.
    const initialValues = {
        courses: (booking.courses ?? []).join('\n'),
        lodging: booking.lodging ?? '',
        startDate: booking.startDate ?? '',
        endDate: booking.endDate ?? '',
        totalCost: String(booking.totalCost ?? ''),
        itinerary: booking.itinerary ?? '',
    };

    return (
        <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
            <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
                <BackLink onPress={() => goBack(router, '/booked')} label="Correct Booking" />
                <Text style={[type.muted, styles.tagline]}>
                    The trip stays booked. Only these details change.
                </Text>

                <RequestSummary trip={trip} />

                <BookingForm
                    request={trip}
                    initialValues={initialValues}
                    submitLabel="Save Correction"
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
