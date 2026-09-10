package org.golftripbooker.domain;

import org.golftripbooker.data.UserRepository;
import org.golftripbooker.models.Role;
import org.golftripbooker.models.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public class UserService implements UserDetailsService {

    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    /**
     * Used by the login endpoint's AuthenticationManager. The authority comes from
     * the database row, so a token can only ever carry the role the row says.
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = repository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole().authority())
                .build();
    }

    public User findByUsername(String username) {
        return repository.findByUsername(username);
    }

    /**
     * Resolves the caller from the JWT-populated Principal. Returns null when nobody
     * is authenticated, which every service treats as FORBIDDEN.
     *
     * This is looked up fresh rather than trusted from the token so that a role
     * changed in the database takes effect on the next request, not on the next login.
     */
    public User currentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return repository.findByUsername(principal.getName());
    }

    /**
     * Registration. Note there is no role parameter anywhere in this method --
     * that is the whole of "a user cannot make themselves a host" at this layer,
     * and UserJdbcClientRepository.create hard-codes CLIENT as the second layer.
     */
    public Result<User> create(String email, String username, String password) {
        Result<User> result = new Result<>();

        validateEmail(result, email);
        validateUsername(result, username);
        validatePassword(result, password);

        if (!result.isSuccess()) {
            return result;
        }

        if (repository.findByUsername(username) != null) {
            result.addErrorMessage("That username is taken.", ResultType.INVALID);
        }

        if (repository.findByEmail(email) != null) {
            result.addErrorMessage("That email is already registered.", ResultType.INVALID);
        }

        if (!result.isSuccess()) {
            return result;
        }

        User toCreate = new User(0, email.trim(), username.trim(), encoder.encode(password), Role.CLIENT);
        result.setPayload(repository.create(toCreate));
        return result;
    }

    private void validateEmail(Result<User> result, String email) {
        if (email == null || email.isBlank()) {
            result.addErrorMessage("Email is required.", ResultType.INVALID);
        } else if (email.length() > MAX_EMAIL_LENGTH) {
            result.addErrorMessage("Email must be %s characters or fewer.",
                    ResultType.INVALID, MAX_EMAIL_LENGTH);
        } else if (!email.contains("@") || email.startsWith("@") || email.endsWith("@")) {
            result.addErrorMessage("Email must look like an email address.", ResultType.INVALID);
        }
    }

    private void validateUsername(Result<User> result, String username) {
        if (username == null || username.isBlank()) {
            result.addErrorMessage("Username is required.", ResultType.INVALID);
        } else if (username.length() > MAX_USERNAME_LENGTH) {
            result.addErrorMessage("Username must be %s characters or fewer.",
                    ResultType.INVALID, MAX_USERNAME_LENGTH);
        }
    }

    private void validatePassword(Result<User> result, String password) {
        if (password == null || password.isBlank()) {
            result.addErrorMessage("Password is required.", ResultType.INVALID);
            return;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            result.addErrorMessage("Password must be at least %s characters.",
                    ResultType.INVALID, MIN_PASSWORD_LENGTH);
            return;
        }

        int digits = 0;
        int letters = 0;
        int others = 0;
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                digits++;
            } else if (Character.isLetter(c)) {
                letters++;
            } else if (!Character.isWhitespace(c)) {
                others++;
            }
        }

        if (digits == 0 || letters == 0 || others == 0) {
            result.addErrorMessage(
                    "Password must contain a digit, a letter, and a symbol.", ResultType.INVALID);
        }
    }
}
