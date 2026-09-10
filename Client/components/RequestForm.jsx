import { useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';

import Button from './Button';
import ErrorList from './ErrorList';
import Field from './Field';
import DestinationPicker from './DestinationPicker';
import useGeocode from '../hooks/useGeocode';
import { colors, spacing, type } from '../theme';
import { isValidIsoDate, money } from '../utils/format';

export const EMPTY_REQUEST = {
    destinationId: null,
    /** Set instead of destinationId when the trip is for somewhere we have not booked. */
    requestedPlace: '',
    budgetPerPlayer: '',
    playerCount: '',
    roundsRequested: '',
    nights: '',
    earliestStart: '',
    latestStart: '',
    notes: '',
};

/**
 * The trip request form, shared by New Request and Edit Request.
 *
 * Extracted rather than copied: the two screens differ only in where the values
 * start and which endpoint they call. A duplicated form would have drifted the
 * first time a validation rule changed on one and not the other.
 *
 * `errors` are the server's; local checks live here and take precedence while they
 * exist, so a missing field never gets buried under a stale API message.
 */
export default function RequestForm({
    destinations,
    loadingDestinations = false,
    initialValues = EMPTY_REQUEST,
    submitLabel,
    onSubmit,
    busy = false,
    errors = [],
    // { players, price } from a trip being copied. Drives the party-size warning below.
    source = null,
    /*
     * Passed by New Request only. Edit Request deliberately has no clear button:
     * emptying an existing request and saving it is not a thing anyone wants, and
     * "start over" there means backing out of the screen.
     *
     * The parent's handler runs as well as the local reset, because arriving from a
     * template puts the values in the URL -- wiping state without stripping the
     * params would leave the banner up and refill the form on the next render.
     */
    onClear = null,
    children,
}) {
    const [form, setForm] = useState(initialValues);
    const [localErrors, setLocalErrors] = useState([]);
    const [confirmingClear, setConfirmingClear] = useState(false);

    /*
     * Callers build initialValues by hand, and one built before this field existed
     * would leave it undefined. Normalising once here means nothing downstream has
     * to keep asking whether the key is there.
     */
    const place = (form.requestedPlace ?? '').trim();

    /*
     * Coordinates for a place typed by hand, derived rather than stored -- keeping
     * them out of form state means there is no way for the name and the pin to drift
     * apart. Idle whenever a real destination is picked, since requestedPlace is
     * empty then.
     */
    const geo = useGeocode(place);

    function set(key, value) {
        setForm((current) => ({ ...current, [key]: value }));
    }

    function handleClear() {
        setForm(EMPTY_REQUEST);
        setLocalErrors([]);
        setConfirmingClear(false);
        if (onClear) {
            onClear();
        }
    }

    /** Picking one clears the other, so only ever one of them reaches the server. */
    function selectDestination(destinationId) {
        setForm((current) => ({ ...current, destinationId, requestedPlace: '' }));
    }

    function changePlace(text) {
        setForm((current) => ({ ...current, requestedPlace: text, destinationId: null }));
    }

    /*
     * Only what the form itself can know -- a missing field, a date that is not a
     * date. Whether the numbers make sense is the server's call, and its messages
     * come back through the same list.
     */
    function localProblems() {
        const problems = [];
        // Same wording the server uses, so the message does not change depending on
        // which of the two checks happened to catch it.
        if (!form.destinationId && !place) {
            problems.push('Pick a destination, or tell us where you want to go.');
        }
        if (!form.budgetPerPlayer) problems.push('Enter a budget per player.');
        if (!form.playerCount) problems.push('Enter how many players.');
        if (!form.roundsRequested) problems.push('Enter how many rounds.');
        if (!form.nights) problems.push('Enter how many nights.');
        // Both ends or neither, matching the server. Checked here too so the most
        // likely mistake gets an answer without a round trip.
        if (Boolean(form.earliestStart) !== Boolean(form.latestStart)) {
            problems.push('Give both ends of the date range, or leave both empty.');
        }
        if (form.earliestStart && !isValidIsoDate(form.earliestStart)) {
            problems.push('Earliest start must be a date, formatted YYYY-MM-DD.');
        }
        if (form.latestStart && !isValidIsoDate(form.latestStart)) {
            problems.push('Latest start must be a date, formatted YYYY-MM-DD.');
        }
        return problems;
    }

    function handleSubmit() {
        const problems = localProblems();
        setLocalErrors(problems);
        if (problems.length > 0) {
            return;
        }

        const usingPlace = !form.destinationId && place.length > 0;

        onSubmit({
            // destinationId is an int server-side, so 0 -- not null -- is how we say
            // "not this one". Exactly one of these two is ever populated.
            destinationId: usingPlace ? 0 : form.destinationId,
            requestedPlace: usingPlace ? place : null,
            // Whatever the phone's geocoder managed. Null is fine: the place still
            // reaches the host, it just cannot be mapped until someone fills these in.
            requestedLatitude: usingPlace ? (geo.coords?.latitude ?? null) : null,
            requestedLongitude: usingPlace ? (geo.coords?.longitude ?? null) : null,
            budgetPerPlayer: Number(form.budgetPerPlayer),
            playerCount: Number(form.playerCount),
            roundsRequested: Number(form.roundsRequested),
            nights: Number(form.nights),
            // Nullable server-side, so an empty field means "no preference" rather
            // than an empty string it would fail to read as a date.
            earliestStart: form.earliestStart || null,
            latestStart: form.latestStart || null,
            notes: form.notes || null,
        });
    }

    /*
     * Nothing typed yet means nothing to clear, and a button that does nothing is
     * worse than no button. Compared against EMPTY_REQUEST rather than a hand-written
     * list of fields, so a field added later is covered without touching this.
     */
    const isBlank = (value) => value === null || value === undefined || value === '';
    const hasContent = Object.keys(EMPTY_REQUEST).some(
        (key) =>
            !(isBlank(form[key]) && isBlank(EMPTY_REQUEST[key])) && form[key] !== EMPTY_REQUEST[key]
    );

    const ceiling =
        form.budgetPerPlayer && form.playerCount
            ? Number(form.budgetPerPlayer) * Number(form.playerCount)
            : null;

    /*
     * Green fees are per person, but lodging is not -- a condo split eight ways costs
     * far less a head than the same condo split four ways. Copying an eight-player
     * trip into a four-player request at the same per-player budget quietly
     * understates it, so say so the moment the count changes.
     */
    const partySize = Number(form.playerCount);
    const sizeShift =
        source?.players && partySize && partySize !== source.players
            ? partySize < source.players
                ? `That trip was ${source.players} players sharing. With ${partySize}, lodging costs more each \u2014 budget above ${money(source.price)}.`
                : `That trip was ${source.players} players. With ${partySize} sharing, lodging should cost less each.`
            : null;

    return (
        <View>
            <ErrorList errors={localErrors.length > 0 ? localErrors : errors} />

            {loadingDestinations ? (
                <ActivityIndicator color={colors.primary} style={styles.loading} />
            ) : (
                <DestinationPicker
                    destinations={destinations}
                    selectedValue={form.destinationId}
                    onSelect={selectDestination}
                    place={form.requestedPlace ?? ''}
                    onPlaceChange={changePlace}
                    geoStatus={geo.status}
                />
            )}

            <Field
                label="Budget per player"
                value={form.budgetPerPlayer}
                onChangeText={(text) => set('budgetPerPlayer', text.replace(/[^0-9.]/g, ''))}
                placeholder="1200"
                keyboardType="numeric"
            />

            <View style={styles.row}>
                <View style={styles.half}>
                    <Field
                        label="Players"
                        value={form.playerCount}
                        onChangeText={(text) => set('playerCount', text.replace(/[^0-9]/g, ''))}
                        placeholder="4"
                        keyboardType="number-pad"
                    />
                </View>
                <View style={styles.half}>
                    <Field
                        label="Rounds"
                        value={form.roundsRequested}
                        onChangeText={(text) => set('roundsRequested', text.replace(/[^0-9]/g, ''))}
                        placeholder="3"
                        keyboardType="number-pad"
                    />
                </View>
            </View>

            <View style={styles.row}>
                <View style={styles.half}>
                    <Field
                        label="Nights"
                        value={form.nights}
                        onChangeText={(text) => set('nights', text.replace(/[^0-9]/g, ''))}
                        placeholder="4"
                        keyboardType="number-pad"
                    />
                </View>
                {/* Keeps Nights half-width now that the dates have a row of their own. */}
                <View style={styles.half} />
            </View>

            <View style={styles.row}>
                <View style={styles.half}>
                    <Field
                        label="Earliest start"
                        value={form.earliestStart}
                        onChangeText={(text) => set('earliestStart', text)}
                        placeholder="YYYY-MM-DD"
                    />
                </View>
                <View style={styles.half}>
                    <Field
                        label="Latest start"
                        value={form.latestStart}
                        onChangeText={(text) => set('latestStart', text)}
                        placeholder="YYYY-MM-DD"
                    />
                </View>
            </View>

            <Text style={[type.muted, styles.hint]}>
                Dates are optional. Leave both blank if you can travel any time, or put the same
                date in both if it has to be that day.
            </Text>

            <Field
                label="Notes"
                value={form.notes}
                onChangeText={(text) => set('notes', text)}
                placeholder="Two rounds a day if the budget allows"
                multiline
            />

            {sizeShift ? <Text style={[type.muted, styles.shift]}>{sizeShift}</Text> : null}

            {/* The same figure the host's booking form shows as its ceiling. */}
            {ceiling ? (
                <View style={styles.ceiling}>
                    <Text style={type.muted}>Total budget</Text>
                    <Text style={styles.ceilingValue}>{money(ceiling)}</Text>
                </View>
            ) : null}

            <Button title={submitLabel} onPress={handleSubmit} loading={busy} />

            {/*
              * Under Submit, not beside it: the primary action should be the one your
              * thumb finds first, and a destructive action sharing a row with it is
              * how people clear a form they meant to send.
              *
              * Two taps, in place, rather than Alert.alert -- which does not render
              * under react-native-web, so the same confirm would silently do nothing
              * in a browser. Same pattern the cancel-request flow uses.
              */}
            {onClear && hasContent ? (
                confirmingClear ? (
                    <>
                        <Button title="Yes, clear it" variant="danger" onPress={handleClear} disabled={busy} />
                        <Button
                            title="Keep what I typed"
                            variant="secondary"
                            onPress={() => setConfirmingClear(false)}
                            disabled={busy}
                        />
                    </>
                ) : (
                    <Button
                        title="Clear Form"
                        variant="secondary"
                        onPress={() => setConfirmingClear(true)}
                        disabled={busy}
                    />
                )
            ) : null}

            {children}
        </View>
    );
}

const styles = StyleSheet.create({
    loading: { marginVertical: spacing.lg },
    row: { flexDirection: 'row', gap: spacing.md },
    half: { flex: 1 },
    hint: { marginTop: -spacing.sm, marginBottom: spacing.md },
    shift: { marginBottom: spacing.md, lineHeight: 19, color: colors.money },
    ceiling: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: spacing.md,
    },
    ceilingValue: { fontSize: 18, fontWeight: '700', color: colors.text },
});
