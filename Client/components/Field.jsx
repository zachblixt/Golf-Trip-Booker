import { StyleSheet, Text, TextInput, View } from 'react-native';

import { colors, radius, spacing, type } from '../theme';

/**
 * A labelled text input.
 *
 * autoCapitalize and autoCorrect default to off. On a phone the keyboard would
 * otherwise capitalise the first letter of a username and quietly turn a valid
 * login into a 401 nobody can explain.
 */
export default function Field({
    label,
    value,
    onChangeText,
    placeholder,
    secureTextEntry = false,
    keyboardType = 'default',
    autoCapitalize = 'none',
    multiline = false,
    ...rest
}) {
    return (
        <View style={styles.wrap}>
            <Text style={styles.label}>{label.toUpperCase()}</Text>
            <TextInput
                style={[styles.input, multiline && styles.multiline]}
                value={value}
                onChangeText={onChangeText}
                placeholder={placeholder}
                placeholderTextColor={colors.muted}
                secureTextEntry={secureTextEntry}
                keyboardType={keyboardType}
                autoCapitalize={autoCapitalize}
                autoCorrect={false}
                multiline={multiline}
                {...rest}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    wrap: { marginBottom: spacing.md },
    label: { ...type.label, marginBottom: spacing.xs },
    input: {
        backgroundColor: colors.card,
        borderWidth: 1,
        borderColor: colors.border,
        borderRadius: radius.sm,
        paddingHorizontal: spacing.md,
        paddingVertical: spacing.sm + 4,
        fontSize: 15,
        color: colors.text,
    },
    multiline: { minHeight: 88, textAlignVertical: 'top' },
});
