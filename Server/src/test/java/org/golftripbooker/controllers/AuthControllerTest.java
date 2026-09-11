package org.golftripbooker.controllers;

import org.golftripbooker.domain.UserService;
import org.golftripbooker.security.JwtConverter;
import org.golftripbooker.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The first tests over the HTTP layer. A @WebMvcTest slice loads the controller
 * and GlobalExceptionHandler but no database, so these run without a MySQL
 * anywhere, unlike the repository tests.
 *
 * The @Import is not optional. SecurityConfig is a plain @Configuration, and a
 * @WebMvcTest slice does not pick those up -- without it the slice quietly falls
 * back to Spring Boot's DEFAULT security, which has CSRF enabled and authenticates
 * every route, so every request here returned 403 before reaching the controller.
 * A security test that forgets this is testing Spring Boot's defaults rather than
 * the configuration it is supposed to be checking.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtConverter jwtConverter;

    private static final String LOGIN_BODY = """
            {"username":"zach","password":"1234"}
            """;

    @Test
    void aWrongPasswordIsAnUnauthorized() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$[0]").value("Invalid username or password."));
    }

    /**
     * The regression this class was written for.
     *
     * Spring Security wraps any failure inside the UserDetailsService -- an
     * unreachable database being the common one -- in
     * InternalAuthenticationServiceException, which extends AuthenticationException
     * by inheritance while saying nothing at all about the caller's credentials.
     * Catching the parent type therefore answers "your password is wrong" when the
     * truth is "the database is down".
     *
     * That cost about twenty minutes of debugging on 2026-09-11, where three
     * unrelated causes -- a wrong password, a shell eating a $ in an unquoted one,
     * and an unset variable -- all produced an identical 401. It would mislead
     * anyone reading production logs far longer than that.
     */
    @Test
    void anUnreachableDatabaseIsAServerErrorNotAnUnauthorized() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new InternalAuthenticationServiceException(
                        "Could not open JDBC connection"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isInternalServerError());
    }
}
