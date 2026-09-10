package org.golftripbooker.controllers;

import org.golftripbooker.domain.TripRequestService;
import org.golftripbooker.dtos.BookedTripResponse;
import org.golftripbooker.models.TripRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Post-MVP Explore: what other groups booked, and for how much.
 *
 * Given its own path rather than /api/request/booked so that "booked" can never be
 * mistaken for a request id, and so the anonymised shape lives somewhere obvious.
 */
@RestController
@RequestMapping("/api/explore")
public class ExploreController {

    private final TripRequestService service;

    public ExploreController(TripRequestService service) {
        this.service = service;
    }

    /** Both filters are optional. No parameters at all means "any budget, anywhere". */
    @GetMapping
    public List<BookedTripResponse> findBooked(
            @RequestParam(required = false) BigDecimal maxCostPerPlayer,
            @RequestParam(required = false) Integer destinationId) {

        List<TripRequest> booked = service.findBooked(maxCostPerPlayer, destinationId);
        return BookedTripResponse.fromAll(booked, service.findBookingsFor(booked));
    }
}
