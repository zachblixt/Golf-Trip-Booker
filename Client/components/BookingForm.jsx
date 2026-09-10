import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import Button from './Button';
import ErrorList from './ErrorList';
import Field from './Field';
import { colors, spacing, type } from '../theme';
import { isValidIsoDate, money } from '../utils/format';

export const EMPTY_BOOKING = {
    courses: '',
    lodging: '',
    startDate: '',
    endDate: '',
    totalCost: '',
    itinerary: '',
};

/** Mirrors Booking.courseList() on the server: blank lines are not courses. */
export function courseLines(text) {
    return text
        .split('\n')
        .map((line) => line.trim())
        .filter(Boolean);
}

/**
 * The booking form, shared by Send Proposal and Correct Booking.
 *
 * The two live hints -- courses against rounds, total against the ceiling -- are
 * advisory. The courses hint mirrors a rule the server enforces; the budget hint no
 * longer mirrors anything, because going over budget stopped being an error. It is
 * there so the host knows what the client is about to see.
 */
export default function BookingForm({
    request,
    initialValues = EMPTY_BOOKING,
    submitLabel,
    onSubmit,
    busy = false,
    errors = [],
    children,
}) {
    const [form, setForm] = useState(initialValues);
    const [localErrors, setLocalErrors] = useState([]);

    function set(key, value) {
        setForm((current) => ({ ...current, [key]: value }));
    }

    function localProblems() {
        const problems = [];
        if (courseLines(form.courses).length === 0) {
            problems.push('List at least one course, one per line.');
        }
        if (!form.startDate) problems.push('Enter a start date.');
        else if (!isValidIsoDate(form.startDate)) problems.push('Start date must be a date, formatted YYYY-MM-DD.');
        if (!form.endDate) problems.push('Enter an end date.');
        else if (!isValidIsoDate(form.endDate)) problems.push('End date must be a date, formatted YYYY-MM-DD.');
        if (!form.totalCost) problems.push('Enter the total cost.');
        return problems;
    }

    function handleSubmit() {
        const problems = localProblems();
        setLocalErrors(problems);
        if (problems.length > 0) {
            return;
        }

        const payload = {
            courses: form.courses,
            lodging: form.lodging || null,
            startDate: form.startDate,
            endDate: form.endDate,
            totalCost: Number(form.totalCost),
            itinerary: form.itinerary || null,
        };

        onSubmit(payload);
    }

    const ceiling = Number(request.budgetCeiling);
    const entered = Number(form.totalCost);
    const overBy = form.totalCost && !Number.isNaN(entered) ? entered - ceiling : null;
    const courseCount = courseLines(form.courses).length;

    return (
        <View>
            <ErrorList errors={localErrors.length > 0 ? localErrors : errors} />

            <Field
                label="Courses — one per line"
                value={form.courses}
                onChangeText={(text) => set('courses', text)}
                placeholder={'Bandon Trails\nPacific Dunes\nOld Macdonald'}
                multiline
            />
            <Text
                style={[type.muted, styles.hint, courseCount > request.roundsRequested && styles.over]}
            >
                {courseCount} of {request.roundsRequested} rounds
                {courseCount > request.roundsRequested ? ' — too many' : ''}
            </Text>

            <Field
                label="Staying"
                value={form.lodging}
                onChangeText={(text) => set('lodging', text)}
                placeholder="Lily Pond"
            />

            <View style={styles.row}>
                <View style={styles.half}>
                    <Field
                        label="Start"
                        value={form.startDate}
                        onChangeText={(text) => set('startDate', text)}
                        placeholder="YYYY-MM-DD"
                    />
                </View>
                <View style={styles.half}>
                    <Field
                        label="End"
                        value={form.endDate}
                        onChangeText={(text) => set('endDate', text)}
                        placeholder="YYYY-MM-DD"
                    />
                </View>
            </View>

            <Field
                label="Total cost"
                value={form.totalCost}
                onChangeText={(text) => set('totalCost', text.replace(/[^0-9.]/g, ''))}
                placeholder="3880"
                keyboardType="numeric"
            />
            {overBy !== null ? (
                <Text
                    style={[
                        type.muted,
                        styles.hint,
                        overBy > 0 && styles.over,
                        overBy <= 0 && styles.under,
                    ]}
                >
                    {overBy > 0
                        ? `${money(overBy)} over budget — they will see that when they review it`
                        : `${money(Math.abs(overBy))} under budget`}
                </Text>
            ) : null}

            <Field
                label="Itinerary"
                value={form.itinerary}
                onChangeText={(text) => set('itinerary', text)}
                placeholder="One round a day, walking."
                multiline
            />

            <View style={styles.actions}>
                <Button
                    title={submitLabel}
                    onPress={handleSubmit}
                    loading={busy}
                />
                {children}
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    row: { flexDirection: 'row', gap: spacing.md },
    half: { flex: 1 },
    hint: { marginTop: -spacing.sm, marginBottom: spacing.md },
    over: { color: colors.danger, fontWeight: '600' },
    under: { color: colors.money },
    actions: { marginTop: spacing.md },
});
