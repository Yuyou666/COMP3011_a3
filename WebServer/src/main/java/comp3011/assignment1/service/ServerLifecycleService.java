package comp3011.assignment1.service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

/**
 * Stores server lifecycle information for the current application process.
 *
 * This service records the server start time and tracks whether a graceful
 * shutdown has already been requested.
 */
@Service
public class ServerLifecycleService {

    // Recorded once when this service is created, representing server start time.
    private final Instant serverStart = Instant.now();

    /*
     * Tracks whether shutdown has started.
     * AtomicBoolean is used so concurrent shutdown requests cannot both succeed.
     */
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);

    /**
     * Returns the time when the current server process started.
     *
     * @return server start time in UTC
     */
    public Instant getServerStart() {
        return serverStart;
    }

    /**
     * Attempts to mark the server as shutting down.
     *
     * compareAndSet(false, true) changes the value only if shutdown has
     * not already been requested, making the operation thread-safe.
     *
     * @return true if this is the first shutdown request, otherwise false
     */
    public boolean requestShutdown() {
        return shutdownInProgress.compareAndSet(false, true);
    }

    /**
     * Checks whether a graceful shutdown is currently in progress.
     *
     * @return true if shutdown has been requested
     */
    public boolean isShutdownInProgress() {
        return shutdownInProgress.get();
    }
}