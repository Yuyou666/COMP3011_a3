package comp3011.assignment1.dto;

/**
 * Response object returned when a graceful shutdown request
 * has been successfully accepted by the server.
 *
 * @param message confirmation message describing the shutdown request
 */
public record ShutdownResponse(
        String message
) {
}