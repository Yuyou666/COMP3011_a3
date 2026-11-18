package comp3011.assignment1.dto;

import java.time.Instant;

/**
 * Standard error response returned by the REST API.
 *
 * Using a record makes the response immutable and provides
 * a concise data structure for serialising error information to JSON.
 *
 * @param timestamp time when the error occurred
 * @param status HTTP status code
 * @param error short description of the HTTP error
 * @param message user-facing explanation of the error
 * @param path request path where the error occurred
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}