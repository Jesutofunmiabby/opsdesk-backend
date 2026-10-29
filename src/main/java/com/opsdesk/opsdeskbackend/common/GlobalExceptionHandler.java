package com.opsdesk.opsdeskbackend.common;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.opsdesk.opsdeskbackend.ticket.TicketNotFoundException;

// Turns every exception from every controller into the contract's error
// shape: { error, message, fields }.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", e.getMessage(), e.getFields()));
    }

    // @Valid found fields that break their rules.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException e) {
        return handleValidation(ValidationException.fromFieldErrors(e.getFieldErrors()));
    }

    // The body is empty, broken JSON, an array, or not sent as JSON at all.
    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleUnreadableBody(Exception e) {
        return handleValidation(new ValidationException("The request body must be a JSON object."));
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TicketNotFoundException e) {
        return notFound(e.getMessage());
    }

    // The contract treats an id that is not a whole number (/tickets/abc) as
    // not found.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        if (e.getName().equals("id")) {
            return notFound("There is no ticket with the id \"" + e.getValue() + "\".");
        }
        String message = switch (e.getName()) {
            case "page" -> "page must be a whole number of 0 or more.";
            case "size" -> "size must be a whole number from 1 to 100.";
            default -> "Choose a status: OPEN, IN_PROGRESS, RESOLVED or CLOSED.";
        };
        return handleValidation(new ValidationException("Check the query parameters.",
                Map.of(e.getName(), message)));
    }

    // A URL that no endpoint answers.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return notFound("There is nothing at this address.");
    }

    // The contract has no code for this, so it gets its own rather than a 500.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("METHOD_NOT_ALLOWED",
                        "This address does not accept " + e.getMethod() + " requests."));
    }

    // Anything unexpected. The details go to the server log only; the client
    // never sees a stack trace or an internal message.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Something went wrong on the server."));
    }

    private static ResponseEntity<ErrorResponse> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", message));
    }
}
