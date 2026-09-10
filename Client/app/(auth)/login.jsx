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

/** Wireframe 01. */
export default function Login() {
    const { login } = useAuth();
    const router = useRouter();

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [errors, setErrors] = useState([]);
    const [busy, setBusy] = useState(false);

    async function handleLogIn() {
        setErrors([]);
        setBusy(true);

        try {
            await login(username.trim(), password);
            // No navigation here. The guard in the root layout flips the moment
            // `user` is set, which swaps the whole stack. Pushing a route as well
            // would race that swap.
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
                <Text style={type.title}>Golf Trip Booker</Text>
                <Text style={[type.muted, styles.tagline]}>Request a golf trip. A host books it.</Text>
            </View>

            <ErrorList errors={errors} />

            <Field
                label="Username"
                value={username}
                onChangeText={setUsername}
                placeholder="username"
                textContentType="username"
            />

            <Field
                label="Password"
                value={password}
                onChangeText={setPassword}
                placeholder="••••••••"
                secureTextEntry
                textContentType="password"
                onSubmitEditing={handleLogIn}
                returnKeyType="go"
            />

            <View style={styles.actions}>
                <Button
                    title="Log In"
                    onPress={handleLogIn}
                    loading={busy}
                    disabled={!username || !password}
                />
                <Button
                    title="Create an account"
                    variant="secondary"
                    onPress={() => router.replace('/register')}
                    disabled={busy}
                />
            </View>
        </Screen>
    );
}

const styles = StyleSheet.create({
    header: { marginBottom: spacing.xl },
    tagline: { marginTop: spacing.xs },
    actions: { marginTop: spacing.md },
});
