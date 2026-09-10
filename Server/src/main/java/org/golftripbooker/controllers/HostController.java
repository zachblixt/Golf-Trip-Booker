package org.golftripbooker.controllers;

import org.golftripbooker.domain.BookingService;
import org.golftripbooker.domain.TripRequestService;
import org.golftripbooker.domain.UserService;
import org.golftripbooker.domain.Result;
import org.golftripbooker.dtos.BookingForm;
import org.golftripbooker.dtos.BookingResponse;
import org.golftripbooker.dtos.DeclineForm;
import org.golftripbooker.dtos.WithdrawForm;
import org.golftripbooker.dtos.TripRequestResponse;
import org.golftripbooker.models.Booking;
import org.golftripbooker.models.RequestStatus;
import org.golftripbooker.models.TripRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Everything only a host may do, gathered under one path prefix so
 * SecurityConfig can gate the whole branch with a single hasRole("HOST") rule.
 * BookingService then checks the role again on every call.
 */
@RestController
@RequestMapping("/api/host")
public class HostController {

    private final BookingService service;
    private final TripRequestService requestService;
    private final UserService userService;

    public HostController(BookingService service,
                          TripRequestService requestService,
                          UserService userService) {
        this.service = service;
        this.requestService = requestService;
        this.userService = userService;
    }

    /**
     * The queue, oldest first, each card showing who asked.
     *
     * Defaults to PENDING so the existing call keeps working untouched. `?status=BOOKED`
     * is what the host's Booked tab reads, and those responses carry the booking -- which
     * is how a correction finds its booking id.
     */
    @GetMapping("/request")
    public List<TripRequestResponse> findByStatus(
            @RequestParam(required = false, defaultValue = "PENDING") RequestStatus status) {

        List<TripRequest> found = service.findByStatus(status);
        return TripRequestResponse.forHost(found, requestService.findBookingsFor(found));
    }

    /** The request behind the booking form. */
    @GetMapping("/request/{requestId}")
    public ResponseEntity<?> findRequest(@PathVariable int requestId, Principal principal) {
        Result<TripRequest> result =
                requestService.findById(requestId, userService.currentUser(principal));

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        // Status-aware: a request under proposal carries that proposal, so the host sees
        // what they sent rather than an empty booking block.
        return new ResponseEntity<>(
                TripRequestResponse.forHost(result.getPayload(),
                        requestService.findBooking(result.getPayload()),
                        requestService.countProposals(requestId)),
                HttpStatus.OK);
    }

    /**
     * Send Proposal. 201, and the request moves to PROPOSED to wait on the client.
     *
     * There is one endpoint here rather than two. The earlier API had book and offer as
     * separate routes, split on whether the total cleared the client's budget -- which
     * made price the only thing a client got to refuse.
     */
    @PostMapping("/request/{requestId}/proposal")
    public ResponseEntity<?> propose(@PathVariable int requestId,
                                     @RequestBody BookingForm form,
                                     Principal principal) {

        Result<Booking> result =
                service.propose(form.toModel(requestId, 0), userService.currentUser(principal));

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        return new ResponseEntity<>(BookingResponse.from(result.getPayload()), HttpStatus.CREATED);
    }

    /**
     * Pull a proposal back, with a note. 204, and the request returns to the queue.
     *
     * A POST rather than the DELETE this used to be. Nothing is deleted -- the proposal
     * stays as history -- and the note has to travel in a body, which a DELETE is a poor
     * place to put. The verb follows what actually happens.
     */
    @PostMapping("/request/{requestId}/withdrawal")
    public ResponseEntity<?> withdrawProposal(@PathVariable int requestId,
                                              @RequestBody WithdrawForm form,
                                              Principal principal) {

        Result<Void> result = service.withdrawProposal(requestId, form.getNote(),
                userService.currentUser(principal));

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204
    }

    /**
     * Everything ever proposed on a request, newest first, live and dead alike. This is
     * what lets the host read a request as a conversation rather than as a bare row that
     * keeps reappearing in the queue for reasons nobody wrote down.
     */
    @GetMapping("/request/{requestId}/proposals")
    public ResponseEntity<?> proposals(@PathVariable int requestId, Principal principal) {
        Result<List<Booking>> result =
                service.findProposalHistory(requestId, userService.currentUser(principal));

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        return new ResponseEntity<>(
                result.getPayload().stream().map(BookingResponse::from).toList(), HttpStatus.OK);
    }

    /** Decline with a reason. */
    @PostMapping("/request/{requestId}/decline")
    public ResponseEntity<?> decline(@PathVariable int requestId,
                                     @RequestBody DeclineForm form,
                                     Principal principal) {

        Result<Void> result =
                service.decline(requestId, form.getReason(), userService.currentUser(principal));

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204
    }

    /** Post-MVP: fix a booking after the fact. The request stays BOOKED. */
    @PutMapping("/booking/{bookingId}")
    public ResponseEntity<?> updateBooking(@PathVariable int bookingId,
                                           @RequestBody BookingForm form,
                                           Principal principal) {

        // requestId is ignored here -- BookingService pins it to the existing row so a
        // correction cannot move a booking onto a different request.
        Result<Booking> result =
                service.update(form.toModel(0, bookingId), userService.currentUser(principal));

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204
    }
}
