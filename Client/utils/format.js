/** $1,200 — no cents, because every figure in this app is a whole-dollar budget. */
export function money(value) {
    const number = Number(value);
    if (Number.isNaN(number)) {
        return '$0';
    }
    return `$${number.toLocaleString('en-US', { maximumFractionDigits: 0 })}`;
}

/** $1,162.50 — cents kept, because a per-player split rarely lands on a dollar. */
export function moneyExact(value) {
    const number = Number(value);
    if (Number.isNaN(number)) {
        return '$0.00';
    }
    return `$${number.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/**
 * "2026-10-12" -> "Oct 12". Split rather than `new Date(string)`, which reads a
 * bare date as UTC midnight and then renders it in local time -- west of Greenwich
 * that shows the day before, which is exactly the kind of bug nobody looks for.
 */
export function shortDate(isoDate) {
    if (!isoDate) {
        return '';
    }
    const [year, month, day] = isoDate.slice(0, 10).split('-').map(Number);
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
        'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    return `${months[month - 1]} ${day}`;
}

/** "Jun 2026". Used to date a trip somebody is copying. */
export function monthYear(isoDate) {
    if (!isoDate) {
        return '';
    }
    const [year, month] = isoDate.slice(0, 10).split('-').map(Number);
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
        'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    return `${months[month - 1]} ${year}`;
}

/** "Oct 12 – Oct 16" for a booked stay. */
export function dateRange(start, end) {
    if (!start || !end) {
        return '';
    }
    return `${shortDate(start)} – ${shortDate(end)}`;
}

/** "3 rounds · 4 nights", skipping any part that is missing. */
export function summaryLine(parts) {
    return parts.filter(Boolean).join(' · ');
}

/** True for a well-formed yyyy-MM-dd that is also a real calendar date. */
export function isValidIsoDate(text) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(text)) {
        return false;
    }
    const [year, month, day] = text.split('-').map(Number);
    if (month < 1 || month > 12 || day < 1) {
        return false;
    }
    // Day 0 of the next month is the last day of this one.
    return day <= new Date(year, month, 0).getDate();
}
