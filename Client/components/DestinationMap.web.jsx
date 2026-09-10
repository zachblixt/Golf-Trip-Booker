import { StyleSheet, Text, View } from 'react-native';

import { spacing, type } from '../theme';

/**
 * The web build's stand-in for the map.
 *
 * react-native-maps wraps MapKit and Google Maps -- native SDKs with no browser
 * equivalent. Importing it under react-native-web crashes the bundle outright, so
 * this file exists to make sure that import never happens: Metro resolves
 * `.web.jsx` for the browser and `.native.jsx` for iOS and Android.
 *
 * The props are accepted and ignored so the two files stay interchangeable and
 * Explore does not need to know which one it got.
 */
export default function DestinationMap() {
    return (
        <View style={styles.wrap}>
            <Text style={type.heading}>The map lives on your phone</Text>
            <Text style={[type.muted, styles.text]}>
                Browsing destinations on a map needs native map support, which the browser
                does not have. Switch to List to see the same trips, or open the app on your
                phone for the map.
            </Text>
        </View>
    );
}

const styles = StyleSheet.create({
    wrap: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: spacing.lg },
    text: { marginTop: spacing.sm, textAlign: 'center', maxWidth: 420 },
});
