import { StyleSheet, Text, View } from 'react-native';

import { colors, radius, spacing, type } from '../theme';
import { dateRange, money, shortDate, summaryLine } from '../utils/format';

/**
 * What the client asked for, as the host sees it above the booking form. Shared by
 * Book This Trip and Correct Booking so the ceiling is quoted identically on both.
 */
export default function RequestSummary({ trip }) {
    return (
        <View style={styles.summary}>
            <View style={styles.head}>
                <Text style={type.heading}>{trip.placeLabel ?? trip.destination?.name}</Text>
                <Text style={styles.ceiling}>Ceiling {money(trip.budgetCeiling)}</Text>
            </View>

            <Text style={type.muted}>
                {summaryLine([
                    `${trip.playerCount} ${trip.playerCount === 1 ? 'player' : 'players'}`,
                    `${trip.roundsRequested} ${trip.roundsRequested === 1 ? 'round' : 'rounds'}`,
                    `${trip.nights} ${trip.nights === 1 ? 'night' : 'nights'}`,
                ])}
            </Text>

            <Text style={[type.muted, styles.from]}>
                {money(trip.budgetPerPlayer)} per player · from {trip.client?.username}
            </Text>

            {/*
              * Shown for the same reason the ceiling is: the server refuses a start date
              * outside this window, so the host needs to see the rule before they hit it.
              */}
            {trip.earliestStart ? (
                <Text style={styles.window}>
                    {trip.earliestStart === trip.latestStart
                        ? `Must start ${shortDate(trip.earliestStart)}`
                        : `Can start ${dateRange(trip.earliestStart, trip.latestStart)}`}
                </Text>
            ) : null}

            {trip.notes ? <Text style={[type.muted, styles.notes]}>“{trip.notes}”</Text> : null}

            {/*
              * The one thing about this request that the numbers do not say. Booking it
              * does more than answer one client: it creates the destination, which puts
              * the trip in Explore and on the map for everybody after them.
              */}
            {trip.destination == null && trip.requestedPlace ? (
                <Text style={styles.newSpot}>
                    Nobody has been booked here before. Booking it adds {trip.requestedPlace} to the
                    destination list for everyone.
                </Text>
            ) : null}
        </View>
    );
}

const styles = StyleSheet.create({
    summary: {
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.md,
        padding: spacing.md,
        marginBottom: spacing.lg,
    },
    head: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: spacing.xs,
    },
    ceiling: { fontSize: 13, fontWeight: '700', color: '#8A6A20' },
    from: { marginTop: spacing.xs },
    window: { marginTop: spacing.xs, fontSize: 13, fontWeight: '700', color: colors.primary },
    notes: { marginTop: spacing.sm, fontStyle: 'italic' },
    newSpot: {
        marginTop: spacing.sm,
        paddingTop: spacing.sm,
        borderTopWidth: 1,
        borderTopColor: colors.border,
        fontSize: 13,
        lineHeight: 18,
        fontWeight: '600',
        color: colors.primary,
    },
});
