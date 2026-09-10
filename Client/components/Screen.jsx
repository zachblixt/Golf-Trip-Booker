import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, spacing } from '../theme';

/**
 * Every screen's outer shell. `scroll` is off by default because a FlatList must
 * never be nested inside a ScrollView; forms turn it on.
 */
export default function Screen({ children, scroll = false }) {
    const body = scroll ? (
        <ScrollView
            contentContainerStyle={styles.content}
            keyboardShouldPersistTaps="handled"
        >
            {children}
        </ScrollView>
    ) : (
        <View style={styles.content}>{children}</View>
    );

    return (
        <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
            <KeyboardAvoidingView
                style={styles.fill}
                // iOS pushes content up; Android's windowSoftInputMode already does.
                behavior={Platform.OS === 'ios' ? 'padding' : undefined}
            >
                {body}
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    safe: { flex: 1, backgroundColor: colors.bg },
    fill: { flex: 1 },
    content: { flexGrow: 1, padding: spacing.lg },
});
