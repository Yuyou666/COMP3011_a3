package comp3011.assignment1.controller;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.dto.ErrorResponse;
import comp3011.assignment1.dto.ShutdownResponse;
import comp3011.assignment1.dto.UptimeResponse;
import comp3011.assignment1.service.ServerLifecycleService;
import comp3011.assignment1.service.ShutdownService;

/**
 * Provides administrative REST endpoints for server uptime
 * and graceful shutdown.
 */
@RestController
public class AdminController {

    // Stores server lifecycle information such as start time
    // and whether a shutdown has already been requested.
    private final ServerLifecycleService serverLifecycleService;

    // Performs the actual graceful shutdown of the Spring application.
    private final ShutdownService shutdownService;

    /**
     * Constructor injection is used so Spring can provide the required services.
     */
    public AdminController(
            ServerLifecycleService serverLifecycleService,
            ShutdownService shutdownService) {

        this.serverLifecycleService = serverLifecycleService;
        this.shutdownService = shutdownService;
    }

    /**
     * Returns the server start time, current UTC time,
     * and total uptime in seconds.
     */
    @GetMapping("/api/v1/admin/uptime")
    public UptimeResponse getServerUptime() {

        // The server start time is recorded once when the application starts.
        Instant serverStart = serverLifecycleService.getServerStart();

        // Instant.now() provides the current timestamp in UTC.
        Instant now = Instant.now();

        // Calculate uptime with fractional-second precision.
        double uptimeSeconds =
                Duration.between(serverStart, now).toNanos()
                        / 1_000_000_000.0;

        return new UptimeResponse(
                serverStart,
                now,
                uptimeSeconds
        );
    }

    /**
     * Requests a graceful shutdown of the application.
     *
     * The first request returns HTTP 202 Accepted.
     * Any further request after shutdown has started returns HTTP 409 Conflict.
     */
    @PostMapping("/api/v1/admin/shutdown")
    public ResponseEntity<?> shutdownServer() {

        /*
         * requestShutdown() changes the shutdown state atomically.
         * If it returns false, another shutdown request has already been accepted.
         */
        if (!serverLifecycleService.requestShutdown()) {

            ErrorResponse error = new ErrorResponse(
                    Instant.now(),
                    409,
                    "Conflict",
                    "Graceful shutdown is already in progress.",
                    "/api/v1/admin/shutdown"
            );

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(error);
        }

        // Prepare the response before starting the shutdown process.
        ShutdownResponse response =
                new ShutdownResponse("Graceful shutdown requested.");

        // Starts the graceful Spring application shutdown.
        shutdownService.shutdown();

        // 202 Accepted indicates that the shutdown request has been accepted.
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(response);
    }
}