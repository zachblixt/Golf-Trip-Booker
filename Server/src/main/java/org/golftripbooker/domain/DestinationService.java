package org.golftripbooker.domain;

import org.golftripbooker.data.DestinationRepository;
import org.golftripbooker.models.Destination;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only on purpose. There is no create/update/delete here because the
 * destination list is seed data, not something a user gets to change.
 */
@Service
public class DestinationService {

    private final DestinationRepository repository;

    public DestinationService(DestinationRepository repository) {
        this.repository = repository;
    }

    public List<Destination> findAll() {
        return repository.findAll();
    }

    public Destination findById(int destinationId) {
        return repository.findById(destinationId);
    }
}
