package org.golftripbooker.models;

/**
 * Why a client turned a proposal down.
 *
 * Structured so the host's queue can be filtered and the counter sheet can ask the right
 * follow-up, with a free-text note beside it carrying what five values cannot. PRICE is
 * only one of them on purpose: the flow this replaced assumed cost was the single thing
 * that could be wrong with a trip, and it was wrong about that.
 */
public enum CounterReason {
    PRICE, DATES, COURSES, LODGING, OTHER
}
