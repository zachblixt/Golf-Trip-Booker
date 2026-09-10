/**
 * Pulled off the wireframes. One place for colour so a screen never invents its own.
 */
export const colors = {
    bg: '#F4F5F7',
    card: '#FFFFFF',
    border: '#E3E4E8',
    text: '#1B1D21',
    muted: '#8A8D96',
    primary: '#33465E',
    primaryText: '#FFFFFF',
    danger: '#A6412F',
    money: '#2E7D4F',
};

/** Status pills on My Trips and the host queue. Keyed by the API's RequestStatus. */
export const statusStyles = {
    PENDING: { bg: '#FAF0D8', fg: '#8A6A20', label: 'Pending' },
    // Blue rather than another warm tone: this is the one status that needs the client
    // to do something, so it should not read as another shade of waiting.
    PROPOSED: { bg: '#E4ECF8', fg: '#2F4F86', label: 'Needs your OK' },
    BOOKED: { bg: '#DFF3E3', fg: '#2F6B3A', label: 'Booked' },
    DECLINED: { bg: '#FBE4E2', fg: '#A6412F', label: 'Declined' },
    CANCELLED: { bg: '#ECECEF', fg: '#6B6E77', label: 'Cancelled' },
};

/**
 * Who you are signed in as. Same shape as statusStyles so the two badges in this
 * app are built the same way, and the colours live here rather than in a screen.
 *
 * Keyed by the API's Role. Host is the green used for a booked trip -- host is the
 * role that books things, so the association is worth reusing rather than inventing
 * a fourth accent colour.
 */
export const roleStyles = {
    CLIENT: { bg: '#E6ECF5', fg: '#33465E', label: 'CLIENT', icon: 'person-outline' },
    HOST: { bg: '#DFF3E3', fg: '#2F6B3A', label: 'HOST', icon: 'briefcase-outline' },
};

export const spacing = {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
};

export const radius = {
    sm: 6,
    md: 10,
    pill: 999,
};

export const type = {
    title: { fontSize: 26, fontWeight: '700', color: colors.text },
    heading: { fontSize: 18, fontWeight: '700', color: colors.text },
    body: { fontSize: 15, color: colors.text },
    muted: { fontSize: 13, color: colors.muted },
    label: {
        fontSize: 11,
        fontWeight: '600',
        color: colors.muted,
        letterSpacing: 0.8,
    },
};
