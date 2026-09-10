package org.golftripbooker.controllers;

import org.golftripbooker.domain.DestinationService;
import org.golftripbooker.dtos.DestinationResponse;
import org.golftripbooker.models.Destination;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/destination")
public class DestinationController {

    private final DestinationService service;

    public DestinationController(DestinationService service) {
        this.service = service;
    }

    /** Feeds the destination picker on the New Request screen. */
    @GetMapping
    public List<DestinationResponse> findAll() {
        return DestinationResponse.fromAll(service.findAll());
    }

    @GetMapping("/{destinationId}")
    public ResponseEntity<Object> findById(@PathVariable int destinationId) {
        Destination destination = service.findById(destinationId);
        if (destination == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(DestinationResponse.from(destination), HttpStatus.OK);
    }
}
