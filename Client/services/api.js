import Constants from 'expo-constants';

/**
 * Where the API lives.
 *
 * `localhost` on a phone is the phone, so a hard-coded localhost would never find
 * your Mac. Rather than making you paste a LAN address into a config file every
 * time DHCP hands you a new one, this reads the address the phone already used to
 * reach the Metro bundler -- which is, by definition, your machine on this network.
 *
 * EXPO_PUBLIC_API_URL overrides it. You need that in exactly two cases: running
 * through `--tunnel` (where hostUri is an exp.direct domain, not your LAN), and
 * pointing at a deployed server.
 */
function resolveBaseUrl() {
    const override = process.env.EXPO_PUBLIC_API_URL;
    if (override) {
        return override;
    }

    const hostUri = Constants.expoConfig?.hostUri;
    const host = hostUri ? hostUri.split(':')[0] : 'localhost';
    return `http://${host}:8080/api`;
}

export const BASE_URL = resolveBaseUrl();

/**
 * Thrown for any non-2xx response. `messages` is always a string array, matching
 * what the server sends, so a screen can render errors without branching on shape.
 * Status 0 means the request never reached the server at all.
 */
export class ApiError extends Error {
    constructor(status, messages) {
        super(messages.join(' '));
        this.status = status;
        this.messages = messages;
    }
}

function messagesFor(payload, status) {
    if (Array.isArray(payload)) return payload;
    if (payload && payload.message) return [payload.message];

    switch (status) {
        case 401:
            return ['Your session expired. Please log in again.'];
        case 403:
            return ['You do not have permission to do that.'];
        case 404:
            return ['That was not found.'];
        default:
            return ['Something went wrong. Please try again.'];
    }
}

/*
 * Held in memory rather than read from SecureStore on every call. AuthContext sets
 * it on login and on restore, so a request never pays for a Keychain round trip.
 */
let authToken = null;

export function setAuthToken(token) {
    authToken = token;
}

export async function request(path, { method = 'GET', body } = {}) {
    const options = { method, headers: {} };

    if (authToken) {
        options.headers.Authorization = `Bearer ${authToken}`;
    }

    if (body !== undefined) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(`${BASE_URL}${path}`, options);
    } catch {
        /*
         * fetch rejects with a bare "Network request failed" when the host is
         * unreachable, which tells you nothing on a phone. Naming the URL turns the
         * single most common setup problem into a self-diagnosing error.
         */
        throw new ApiError(0, [
            `Could not reach the server at ${BASE_URL}.`,
            'Check that Spring is running and that your phone is on the same wifi.',
        ]);
    }

    // 204 No Content has no body to parse.
    if (response.status === 204) {
        if (!response.ok) {
            throw new ApiError(response.status, messagesFor(null, response.status));
        }
        return null;
    }

    // A 401 from Spring Security has an empty body, so response.json() would throw
    // a SyntaxError that hides the real status. Read text first.
    const text = await response.text();

    let payload = null;
    if (text) {
        try {
            payload = JSON.parse(text);
        } catch {
            // Not JSON -- an HTML error page, usually. Fall through to the
            // status-based message rather than crashing on the parse.
            payload = null;
        }
    }

    if (!response.ok) {
        throw new ApiError(response.status, messagesFor(payload, response.status));
    }

    return payload;
}
