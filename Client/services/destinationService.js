import { request } from './api';

/** Seeded server-side, so this is the only list a client may pick from. */
export function findAll() {
    return request('/destination');
}

export function findById(destinationId) {
    return request(`/destination/${destinationId}`);
}
