import { request } from './api';

/** 201 with a UserResponse. The server always makes it a CLIENT. */
export function register(email, username, password) {
    return request('/auth/register', {
        method: 'POST',
        body: { email, username, password },
    });
}

/** 200 with { token, user }. 401 with a message array when the password is wrong. */
export function login(username, password) {
    return request('/auth/login', {
        method: 'POST',
        body: { username, password },
    });
}

/**
 * Called on launch with whatever token SecureStore held. 200 hands back a fresh
 * token so an active user is never logged out mid-week; 401 means it expired.
 */
export function refresh() {
    return request('/auth/refresh');
}

export function me() {
    return request('/auth/me');
}
