package org.golftripbooker.models;

/**
 * Registration always produces CLIENT. HOST is set by hand in the database,
 * which is what makes "a user cannot make themselves a host" true by construction.
 */
public enum Role {
    CLIENT,
    HOST;

    /** Spring Security expects authorities to be prefixed; hasRole("HOST") looks for ROLE_HOST. */
    public String authority() {
        return "ROLE_" + name();
    }
}
