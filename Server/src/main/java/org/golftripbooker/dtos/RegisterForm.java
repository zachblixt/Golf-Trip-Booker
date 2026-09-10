package org.golftripbooker.dtos;

/**
 * Registration input.
 *
 * There is deliberately no `role` field. "As a user, I can't make myself a host"
 * is enforced three times over: this object has nowhere to put a role, UserService
 * never takes one, and UserJdbcClientRepository writes the literal 'CLIENT'.
 */
public class RegisterForm {

    private String email;
    private String username;
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
