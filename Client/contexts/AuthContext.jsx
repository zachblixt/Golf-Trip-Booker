import { createContext, useCallback, useContext, useEffect, useState } from 'react';

import * as authService from '../services/authService';
import { setAuthToken } from '../services/api';
import { clearToken, getToken, saveToken } from '../storage/tokenStorage';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [initializing, setInitializing] = useState(true);

    /*
     * On launch: take the stored token, ask the server whether it is still good, and
     * take a fresh one back. The server is the source of truth, not the token --
     * a token decodes happily long after the account behind it changed.
     */
    useEffect(() => {
        let cancelled = false;

        (async () => {
            try {
                const stored = await getToken();
                if (!stored) {
                    return;
                }

                setAuthToken(stored);
                const session = await authService.refresh();
                if (cancelled) {
                    return;
                }

                setAuthToken(session.token);
                await saveToken(session.token);
                setUser(session.user);
            } catch {
                // Expired, tampered with, or the server is down. Either way we are
                // logged out, and the login screen is the honest thing to show.
                setAuthToken(null);
                await clearToken();
            } finally {
                if (!cancelled) {
                    setInitializing(false);
                }
            }
        })();

        return () => {
            cancelled = true;
        };
    }, []);

    const login = useCallback(async (username, password) => {
        const session = await authService.login(username, password);
        setAuthToken(session.token);
        await saveToken(session.token);
        setUser(session.user);
        return session.user;
    }, []);

    /** Registers, then logs in, so a new account lands inside the app rather than back at a form. */
    const register = useCallback(
        async (email, username, password) => {
            await authService.register(email, username, password);
            return login(username, password);
        },
        [login]
    );

    /*
     * Nothing is stored server side, so logging out is deleting the token. There is
     * no endpoint to call and nothing to invalidate.
     */
    const logout = useCallback(async () => {
        setAuthToken(null);
        setUser(null);
        await clearToken();
    }, []);

    return (
        <AuthContext.Provider
            value={{ user, initializing, isHost: user?.role === 'HOST', login, register, logout }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === null) {
        throw new Error('useAuth must be used inside an AuthProvider');
    }
    return context;
}
