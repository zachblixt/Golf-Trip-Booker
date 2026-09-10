package org.golftripbooker.domain;

import org.golftripbooker.data.TestDataHelper;
import org.golftripbooker.data.UserRepository;
import org.golftripbooker.models.Role;
import org.golftripbooker.models.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class UserServiceTest {

    private static final String GOOD_PASSWORD = "P@ssw0rd!";

    @Autowired
    UserService service;

    @MockitoBean
    UserRepository repository;

    @Test
    void createHappyPath() {
        when(repository.create(any(User.class))).thenReturn(TestDataHelper.userAfterCreate());

        Result<User> actual = service.create("c@c.com", "clientC", GOOD_PASSWORD);

        assertTrue(actual.isSuccess());
        assertEquals(TestDataHelper.userAfterCreate(), actual.getPayload());
    }

    @Test
    void createFailsWhenEmailIsBlank() {
        Result<User> actual = service.create("  ", "clientC", GOOD_PASSWORD);

        assertEquals(ResultType.INVALID, actual.getResultType());
        assertTrue(actual.getErrorMessages().contains("Email is required."));
        verify(repository, never()).create(any(User.class));
    }

    @Test
    void createFailsWhenEmailIsNotAnEmail() {
        Result<User> actual = service.create("not-an-email", "clientC", GOOD_PASSWORD);

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).create(any(User.class));
    }

    @Test
    void createFailsWhenUsernameIsBlank() {
        Result<User> actual = service.create("c@c.com", "", GOOD_PASSWORD);

        assertEquals(ResultType.INVALID, actual.getResultType());
        assertTrue(actual.getErrorMessages().contains("Username is required."));
        verify(repository, never()).create(any(User.class));
    }

    @Test
    void createFailsWhenPasswordIsTooShort() {
        Result<User> actual = service.create("c@c.com", "clientC", "P@ss1");

        assertEquals(ResultType.INVALID, actual.getResultType());
        verify(repository, never()).create(any(User.class));
    }

    @Test
    void createFailsWhenPasswordHasNoSymbol() {
        Result<User> actual = service.create("c@c.com", "clientC", "password1");

        assertEquals(ResultType.INVALID, actual.getResultType());
        assertTrue(actual.getErrorMessages()
                .contains("Password must contain a digit, a letter, and a symbol."));
        verify(repository, never()).create(any(User.class));
    }

    @Test
    void createFailsWhenUsernameIsTaken() {
        when(repository.findByUsername("clientA")).thenReturn(TestDataHelper.clientAWithPassword());

        Result<User> actual = service.create("c@c.com", "clientA", GOOD_PASSWORD);

        assertEquals(ResultType.INVALID, actual.getResultType());
        assertTrue(actual.getErrorMessages().contains("That username is taken."));
        verify(repository, never()).create(any(User.class));
    }

    @Test
    void createFailsWhenEmailIsAlreadyRegistered() {
        when(repository.findByEmail("a@a.com")).thenReturn(TestDataHelper.clientAWithPassword());

        Result<User> actual = service.create("a@a.com", "clientC", GOOD_PASSWORD);

        assertEquals(ResultType.INVALID, actual.getResultType());
        assertTrue(actual.getErrorMessages().contains("That email is already registered."));
        verify(repository, never()).create(any(User.class));
    }

    @Test
    void createHashesThePasswordBeforeSaving() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(repository.create(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.create("c@c.com", "clientC", GOOD_PASSWORD);

        verify(repository).create(captor.capture());
        String stored = captor.getValue().getPassword();

        assertNotEquals(GOOD_PASSWORD, stored);
        assertTrue(stored.startsWith("{bcrypt}"));
    }

    /**
     * "As a user, I can't make myself a host."
     *
     * There is no role parameter to pass, so the only thing left to prove at this
     * layer is that the User handed to the repository is a CLIENT. The repository
     * test proves the insert would ignore it even if it were not.
     */
    @Test
    void createAlwaysBuildsAClient() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(repository.create(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.create("c@c.com", "clientC", GOOD_PASSWORD);

        verify(repository).create(captor.capture());
        assertEquals(Role.CLIENT, captor.getValue().getRole());
    }

    @Test
    void currentUserIsNullWhenNobodyIsLoggedIn() {
        assertNull(service.currentUser(null));
        verify(repository, never()).findByUsername(anyString());
    }
}
