package org.golftripbooker.controllers;

import org.golftripbooker.domain.Result;
import org.golftripbooker.domain.UserService;
import org.golftripbooker.dtos.AuthResponse;
import org.golftripbooker.dtos.LoginForm;
import org.golftripbooker.dtos.RegisterForm;
import org.golftripbooker.dtos.UserResponse;
import org.golftripbooker.models.User;
import org.golftripbooker.security.JwtConverter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService service;
    private final AuthenticationManager authenticationManager;
    private final JwtConverter jwtConverter;

    public AuthController(UserService service,
                          AuthenticationManager authenticationManager,
                          JwtConverter jwtConverter) {
        this.service = service;
        this.authenticationManager = authenticationManager;
        this.jwtConverter = jwtConverter;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterForm form) {
        Result<User> result = service.create(form.getEmail(), form.getUsername(), form.getPassword());

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        // Never return the User itself -- it carries the password hash.
        return new ResponseEntity<>(UserResponse.from(result.getPayload()), HttpStatus.CREATED);
    }

    /**
     * One deliberately vague message for both a wrong username and a wrong password.
     * Telling someone which half they got right is telling them a username exists.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginForm form) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(form.getUsername(), form.getPassword()));
        } catch (AuthenticationException ex) {
            return new ResponseEntity<>(
                    List.of("Invalid username or password."), HttpStatus.UNAUTHORIZED);
        }

        User user = service.findByUsername(form.getUsername());
        return new ResponseEntity<>(
                AuthResponse.of(jwtConverter.makeToken(user), user), HttpStatus.OK);
    }

    /**
     * The app calls this on launch with whatever token expo-secure-store held. A 401
     * means the token expired and the user has to log in again; a 200 hands back a
     * fresh token so an active user is never logged out mid-week.
     */
    @GetMapping("/refresh")
    public ResponseEntity<?> refresh(Principal principal) {
        User user = service.currentUser(principal);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(
                AuthResponse.of(jwtConverter.makeToken(user), user), HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        User user = service.currentUser(principal);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(UserResponse.from(user), HttpStatus.OK);
    }

    /*
     * There is no /logout. Nothing is stored server side, so logging out is the
     * client deleting its token. Revocation before expiry would need a deny list,
     * which is not worth it for a 12-hour token.
     */
}
