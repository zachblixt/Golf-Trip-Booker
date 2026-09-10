package org.golftripbooker.controllers;

import org.golftripbooker.domain.Result;
import org.golftripbooker.dtos.CounterForm;
import org.golftripbooker.domain.TripRequestService;
import org.golftripbooker.domain.UserService;
import org.golftripbooker.dtos.TripRequestForm;
import org.golftripbooker.dtos.TripRequestResponse;
import org.golftripbooker.models.TripRequest;
import org.golftripbooker.models.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * The client half of the API. Every route reads the caller from the Principal --
 * there is no endpoint here that takes a user id.
 */
@RestController
@RequestMapping("/api/request")
public class TripRequestController {

    private final TripRequestService service;
    private final UserService userService;

    public TripRequestController(TripRequestService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    /** My Trips. Scoped to the caller by construction, not by a query parameter. */
    @GetMapping
    public List<TripRequestResponse> findMine(Principal principal) {
        User actingUser = userService.currentUser(principal);
        List<TripRequest> requests = service.findForClient(actingUser);
        return TripRequestResponse.forClient(requests, service.findBookingsFor(requests));
    }

    /** Trip Detail. 403 rather than 404 when it is someone else's -- see the service. */
    @GetMapping("/{requestId}")
    public ResponseEntity<?> findById(@PathVariable int requestId, Principal principal) {
        Result<TripRequest> result = service.findById(requestId, userService.currentUser(principal));

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        TripRequest request = result.getPayload();
        return new ResponseEntity<>(
                TripRequestResponse.forClient(request, service.findBooking(request),
                        service.countProposals(request.getRequestId())),
                HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TripRequestForm form, Principal principal) {
        Result<TripRequest> result =
                service.create(form.toModel(0), userService.currentUser(principal));

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        return new ResponseEntity<>(
                TripRequestResponse.forClient(result.getPayload(), null), HttpStatus.CREATED);
    }

    /** Post-MVP: edit a request that is still pending. */
    @PutMapping("/{requestId}")
    public ResponseEntity<?> update(@PathVariable int requestId,
                                    @RequestBody TripRequestForm form,
                                    Principal principal) {

        Result<TripRequest> result =
                service.update(form.toModel(requestId), userService.currentUser(principal));

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204
    }

    /**
     * Accept what the host arranged. 204. The proposal becomes the booking and the
     * request is BOOKED -- this route is the only way any request ever reaches that
     * status, which is the whole shape of the product in one sentence.
     *
     * It is also the only place a client changes a status a host set, so the ownership
     * check in the service matters more here than anywhere else in the API.
     */
    @PostMapping("/{requestId}/accept")
    public ResponseEntity<?> acceptProposal(@PathVariable int requestId, Principal principal) {
        Result<Void> result = service.acceptProposal(requestId, userService.currentUser(principal));

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204
    }

    /**
     * Turn the proposal down, with a reason and a note. 204, and the request goes back to
     * PENDING rather than to declined. The body is required: a refusal that says nothing
     * sends the host back to the queue no better informed than when they started.
     */
    @PostMapping("/{requestId}/counter")
    public ResponseEntity<?> counterProposal(@PathVariable int requestId,
                                             @RequestBody CounterForm form,
                                             Principal principal) {

        Result<Void> result = service.counterProposal(requestId, form.getReason(), form.getNote(),
                userService.currentUser(principal));

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204
    }

    /**
     * Cancel. Modelled as a DELETE because that is what it means to the client, but
     * the row survives with status CANCELLED -- the host's record of what was asked
     * for should not vanish.
     */
    @DeleteMapping("/{requestId}")
    public ResponseEntity<?> cancel(@PathVariable int requestId, Principal principal) {
        Result<Void> result = service.cancel(requestId, userService.currentUser(principal));

        if (!result.isSuccess()) {
            return ErrorResponse.build(result);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204
    }
}
