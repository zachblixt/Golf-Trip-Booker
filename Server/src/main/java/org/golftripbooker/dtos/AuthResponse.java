package org.golftripbooker.dtos;

import org.golftripbooker.models.User;

/**
 * What login and refresh return. The client stores the token in expo-secure-store
 * (or localStorage on the web build) and puts the user straight into state, so it
 * does not need a second request to find out who it is.
 */
public class AuthResponse {

    private final String token;
    private final UserResponse user;

    public static AuthResponse of(String token, User user) {
        return new AuthResponse(token, UserResponse.from(user));
    }

    public AuthResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public UserResponse getUser() {
        return user;
    }
}
