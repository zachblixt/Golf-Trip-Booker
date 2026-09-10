import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useRouter } from 'expo-router';

import Button from '../../components/Button';
import ErrorList from '../../components/ErrorList';
import Field from '../../components/Field';
import Screen from '../../components/Screen';
import { useAuth } from '../../contexts/AuthContext';
import { ApiError } from '../../services/api';
import { spacing, type } from '../../theme';

/*
 * Log in and sign up are peers, so both buttons between them replace rather than
 * push. router.back() was the bug: it pops whatever is beneath this screen, and on
 * a cold start there is nothing there -- index redirects straight to /login, which
 * replaces rather than pushes, so the stack holds one screen and back() is a silent
 * no-op. replace() does not care how you arrived, which also covers a deep link to
 * /register and the guard swapping the stack out from under you.
 *
 * The cost: Android's hardware back exits the app from here instead of returning to
 * log in. That is the right behaviour for the first screen of an auth flow, and the
 * button above does the job the gesture would have.
 */
export default function Register() {
    const { register } = useAuth();
    const router = useRouter();

    const [email, setEmail] = useState('');
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [errors, setErrors] = useState([]);
    const [busy, setBusy] = useState(false);

    async function handleSignUp() {
        setErrors([]);
        setBusy(true);

        try {
            // Registers and logs in. The server decides the role -- there is nothing
            // to send here that could ask for anything but CLIENT.
            await register(email.trim(), username.trim(), password);
        } catch (error) {
            setErrors(
                error instanceof ApiError ? error.messages : ['Something went wrong. Please try again.']
            );
        } finally {
            setBusy(false);
        }
    }

    return (
        <Screen scroll>
            <View style={styles.header}>
                <Text style={type.title}>Create an account</Text>
                <Text style={[type.muted, styles.tagline]}>
                    Then tell a host where you want to play.
                </Text>
            </View>

            <ErrorList errors={errors} />

            <Field
                label="Email"
                value={email}
                onChangeText={setEmail}
                placeholder="you@example.com"
                keyboardType="email-address"
                textContentType="emailAddress"
            />

            <Field
                label="Username"
                value={username}
                onChangeText={setUsername}
                placeholder="zach"
                textContentType="username"
            />

            <Field
                label="Password"
                value={password}
                onChangeText={setPassword}
                placeholder="At least 8 characters"
                secureTextEntry
                textContentType="newPassword"
            />

            <Text style={[type.muted, styles.hint]}>
                Needs a letter, a digit, and a symbol.
            </Text>

            <View style={styles.actions}>
                <Button
                    title="Create Account"
                    onPress={handleSignUp}
                    loading={busy}
                    disabled={!email || !username || !password}
                />
                <Button
                    title="Back to log in"
                    variant="secondary"
                    onPress={() => router.replace('/login')}
                    disabled={busy}
                />
            </View>
        </Screen>
    );
}

const styles = StyleSheet.create({
    header: { marginBottom: spacing.xl },
    tagline: { marginTop: spacing.xs },
    hint: { marginTop: -spacing.sm, marginBottom: spacing.md },
    actions: { marginTop: spacing.md },
});
