package org.golftripbooker.controllers;

import org.golftripbooker.domain.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Every failure leaves as a JSON array of strings, so the client can do
 * `setErrors(payload)` without branching on the shape.
 */
public class ErrorResponse {

    public static ResponseEntity<Object> build(Result<?> result) {
        HttpStatus status = switch (result.getResultType()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;   // 404
            case FORBIDDEN -> HttpStatus.FORBIDDEN;   // 403
            default -> HttpStatus.BAD_REQUEST;        // 400
        };
        return new ResponseEntity<>(result.getErrorMessages(), status);
    }

    private ErrorResponse() {
    }
}
