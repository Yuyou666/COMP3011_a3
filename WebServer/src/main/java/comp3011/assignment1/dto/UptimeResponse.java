package comp3011.assignment1.dto;

import java.time.Instant;

/**
 * Response object containing information about the current
 * server uptime.
 *
 * Instant is used for the timestamps because it represents
 * an absolute point in time in UTC.
 *
 * @param utcServerStart the UTC time when the server started
 * @param utcNow the current UTC time when the request is processed
 * @param serverUptimeSeconds total server uptime in seconds
 */
public record UptimeResponse(
        Instant utcServerStart,
        Instant utcNow,
        double serverUptimeSeconds
) {
}