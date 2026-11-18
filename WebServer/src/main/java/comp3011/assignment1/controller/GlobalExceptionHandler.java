package comp3011.assignment1.controller;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import comp3011.assignment1.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Provides centralised exception handling for all REST controllers.
 *
 * This keeps error handling out of individual controllers and ensures
 * that unexpected server errors use a consistent JSON response format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles any exception that is not handled elsewhere in the application.
     *
     * @param exception the exception that occurred
     * @param request the HTTP request that caused the exception
     * @return a standard HTTP 500 error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request) {

        // Server-side diagnostics only; never include the exception in HTTP responses.
        LOGGER.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), exception);

        /*
         * Do not expose internal exception details to the client.
         * The response contains a generic message while still identifying
         * the HTTP status and request path.
         */
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                500,
                "Internal Server Error",
                "An unexpected server error occurred.",
                request.getRequestURI()
        );

        // Return the standard error object with HTTP 500 status.
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}