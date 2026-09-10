import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

const KEY = 'gtb.token';

/**
 * The token lives in the Keychain on iOS and EncryptedSharedPreferences on Android.
 * That is what makes "I stay logged in when I reopen the app" true -- it survives
 * the process being killed, unlike anything held in memory.
 *
 * SecureStore is a native module, so it throws on web. `expo start --web` and the
 * Vite client both fall back to localStorage, which is the best a browser offers
 * anyway. Every call is wrapped because a private window can refuse storage
 * outright, and a login failing over a storage error would be absurd.
 */

export async function saveToken(token) {
    if (Platform.OS === 'web') {
        try {
            window.localStorage.setItem(KEY, token);
        } catch {
            // Storage blocked. The session still works until the tab closes.
        }
        return;
    }
    await SecureStore.setItemAsync(KEY, token);
}

export async function getToken() {
    if (Platform.OS === 'web') {
        try {
            return window.localStorage.getItem(KEY);
        } catch {
            return null;
        }
    }
    return SecureStore.getItemAsync(KEY);
}

export async function clearToken() {
    if (Platform.OS === 'web') {
        try {
            window.localStorage.removeItem(KEY);
        } catch {
            // Nothing to do -- there is no state to leave behind.
        }
        return;
    }
    await SecureStore.deleteItemAsync(KEY);
}
