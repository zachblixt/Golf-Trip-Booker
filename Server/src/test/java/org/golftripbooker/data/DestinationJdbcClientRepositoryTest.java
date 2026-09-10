package org.golftripbooker.data;

import org.golftripbooker.models.Destination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DestinationJdbcClientRepositoryTest {

    @Autowired
    private DestinationJdbcClientRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void setup() {
        jdbcClient.sql("call set_known_good_state();").update();
    }

    @Test
    void findAllReturnsEveryDestinationInNameOrder() {
        List<Destination> actual = repository.findAll();

        assertEquals(3, actual.size());
        assertEquals(TestDataHelper.bandonDunes(), actual.get(0));
        assertEquals(TestDataHelper.pinehurst(), actual.get(1));
        assertEquals(TestDataHelper.streamsong(), actual.get(2));
    }

    @Test
    void findByIdHappyPath() {
        assertEquals(TestDataHelper.pinehurst(), repository.findById(1));
    }

    @Test
    void findByIdFailsToFind() {
        assertNull(repository.findById(999));
    }

    @Test
    void findByNameIsCaseInsensitive() {
        assertEquals(TestDataHelper.pinehurst(), repository.findByName("PINEHURST"));
        assertEquals(TestDataHelper.pinehurst(), repository.findByName("pinehurst"));
    }

    @Test
    void findByNameFailsToFind() {
        assertNull(repository.findByName("Missoula, Montana"));
    }

    /** What a host booking a request for somewhere new triggers. */
    @Test
    void create() {
        Destination created = repository.create(new Destination(0, "Missoula, Montana", "Montana",
                "Added when a host booked the first trip here.",
                new BigDecimal("46.872200"), new BigDecimal("-113.994000")));

        assertEquals(4, created.getDestinationId());
        assertEquals(created, repository.findById(4));
        assertEquals(4, repository.findAll().size());
    }

    @Test
    void everyDestinationHasAName() {
        assertTrue(repository.findAll().stream().allMatch(d -> d.getName() != null));
    }
}
