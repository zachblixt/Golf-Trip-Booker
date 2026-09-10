package org.golftripbooker.data;

import org.golftripbooker.models.Role;
import org.golftripbooker.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class UserJdbcClientRepositoryTest {

    @Autowired
    private UserJdbcClientRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void setup() {
        jdbcClient.sql("call set_known_good_state();").update();
    }

    @Test
    void findByIdHappyPath() {
        assertEquals(TestDataHelper.clientAWithPassword(), repository.findById(1));
    }

    @Test
    void findByIdFailsToFind() {
        assertNull(repository.findById(999));
    }

    @Test
    void findByUsernameHappyPath() {
        assertEquals(TestDataHelper.clientAWithPassword(), repository.findByUsername("clientA"));
    }

    @Test
    void findByUsernameFailsToFind() {
        assertNull(repository.findByUsername("nobody"));
    }

    @Test
    void findByEmailHappyPath() {
        assertEquals(TestDataHelper.clientAWithPassword(), repository.findByEmail("a@a.com"));
    }

    @Test
    void findByEmailFailsToFind() {
        assertNull(repository.findByEmail("does@not.exist"));
    }

    @Test
    void findsTheSeededHostWithTheHostRole() {
        User host = repository.findByUsername("hostH");

        assertNotNull(host);
        assertEquals(Role.HOST, host.getRole());
    }

    @Test
    void create() {
        User toCreate = TestDataHelper.userToCreate();
        assertNull(repository.findByEmail(toCreate.getEmail()));

        User actual = repository.create(toCreate);

        assertEquals(TestDataHelper.userAfterCreate(), actual);
        assertNotNull(repository.findByEmail("c@c.com"));
    }

    /**
     * "As a user, I can't make myself a host." The insert writes the literal 'CLIENT'
     * rather than reading the role off the argument, so even a User handed to this
     * method already claiming HOST lands in the table as a CLIENT.
     */
    @Test
    void createIgnoresARoleOfHost() {
        User sneaky = TestDataHelper.userToCreate();
        sneaky.setRole(Role.HOST);

        User created = repository.create(sneaky);

        assertEquals(Role.CLIENT, created.getRole());
        assertEquals(Role.CLIENT, repository.findById(created.getUserId()).getRole());
    }
}
