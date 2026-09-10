package org.golftripbooker.dtos;

import org.golftripbooker.models.Role;
import org.golftripbooker.models.User;

import java.util.Objects;

/**
 * A User safe to send to the app. Never carries the password hash.
 */
public class UserResponse {

    private final int userId;
    private final String username;
    private final String email;
    private final Role role;

    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(user.getUserId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public UserResponse(int userId, String username, String email, Role role) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserResponse that = (UserResponse) o;
        return userId == that.userId
                && Objects.equals(username, that.username)
                && Objects.equals(email, that.email)
                && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username, email, role);
    }
}
