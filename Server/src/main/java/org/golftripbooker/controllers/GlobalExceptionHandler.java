package org.golftripbooker.controllers;

import org.golftripbooker.data.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<List<String>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return new ResponseEntity<>(
                List.of("The request body could not be read. Check that it is valid JSON "
                        + "and that dates are formatted yyyy-MM-dd."),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<List<String>> handleBadPathVariable(MethodArgumentTypeMismatchException ex) {
        return new ResponseEntity<>(
                List.of("'" + ex.getValue() + "' is not a valid " + ex.getName() + "."),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<List<String>> handleDataAccess(DataAccessException ex) {
        ex.printStackTrace();
        return new ResponseEntity<>(
                List.of("Sorry, something went wrong talking to the database."),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // "Catch all" handler.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<List<String>> handleException(Exception ex) {
        ex.printStackTrace();
        return new ResponseEntity<>(
                List.of("Sorry, something unexpected went wrong."),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
