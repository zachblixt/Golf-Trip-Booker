import { useCallback, useState } from 'react';
import { StyleSheet, Text } from 'react-native';
import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';

import RequestForm, { EMPTY_REQUEST } from '../../components/RequestForm';
import ScreenTitle from '../../components/ScreenTitle';
import Screen from '../../components/Screen';
import TemplateBanner from '../../components/TemplateBanner';
import * as destinationService from '../../services/destinationService';
import * as requestService from '../../services/requestService';
import { ApiError } from '../../services/api';
import { formValuesFromParams, sourceFromParams } from '../../utils/tripTemplate';
import { spacing, type } from '../../theme';

/** Wireframe 02. */
export default function New() {
    const router = useRouter();

    /*
     * Arriving from Explore or from your own past trip carries the whole shape of
     * that trip in the query string. Params are strings; formValuesFromParams turns
     * them into what RequestForm holds, and deliberately leaves preferred start
     * blank -- the source trip's date has already been.
     */
    const params = useLocalSearchParams();
    const source = sourceFromParams(params);

    const [destinations, setDestinations] = useState([]);
    const [loadingDestinations, setLoadingDestinations] = useState(true);
    const [errors, setErrors] = useState([]);
    const [busy, setBusy] = useState(false);
    // Bumped after a successful submit to remount the form, clearing its fields.
    const [formKey, setFormKey] = useState(0);

    // Reloaded on focus rather than once on mount, so a destination added to the
    // database shows up without restarting the app.
    useFocusEffect(
        useCallback(() => {
            let cancelled = false;

            destinationService
                .findAll()
                .then((found) => {
                    if (!cancelled) setDestinations(found);
                })
                .catch((error) => {
                    if (!cancelled) {
                        setErrors(
                            error instanceof ApiError ? error.messages : ['Could not load destinations.']
                        );
                    }
                })
                .finally(() => {
                    if (!cancelled) setLoadingDestinations(false);
                });

            return () => {
                cancelled = true;
            };
        }, [])
    );

    async function handleSubmit(payload) {
        setErrors([]);
        setBusy(true);

        try {
            await requestService.create(payload);
            setFormKey((key) => key + 1);
            router.replace('/trips');
        } catch (error) {
            setErrors(error instanceof ApiError ? error.messages : ['Could not submit the request.']);
        } finally {
            setBusy(false);
        }
    }

    /** Strips every param, which empties the form on the remount that follows. */
    function handleClear() {
        router.replace('/new');
    }

    const prefilled = !!params.destinationId;

    return (
        <Screen scroll>
            <ScreenTitle>New Request</ScreenTitle>
            <Text style={[type.muted, styles.tagline]}>
                {source ? 'Change anything you like before sending it.' : 'Tell a host what you are after.'}
            </Text>

            <TemplateBanner source={source} onClear={handleClear} />

            {/*
              * The key carries every incoming value, so arriving from a different trip
              * remounts the form with that trip's numbers. Without it RequestForm would
              * keep the state it was born with and the second copy would show the first.
              */}
            <RequestForm
                key={`${formKey}-${params.destinationId ?? ''}-${params.players ?? ''}-${params.budget ?? ''}`}
                destinations={destinations}
                loadingDestinations={loadingDestinations}
                initialValues={prefilled ? formValuesFromParams(params) : EMPTY_REQUEST}
                source={source}
                submitLabel="Submit Request"
                onSubmit={handleSubmit}
                onClear={handleClear}
                busy={busy}
                errors={errors}
            />
        </Screen>
    );
}

const styles = StyleSheet.create({
    tagline: { marginTop: spacing.xs, marginBottom: spacing.lg },
});
