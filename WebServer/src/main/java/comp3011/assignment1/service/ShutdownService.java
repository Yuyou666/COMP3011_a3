package comp3011.assignment1.service;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Performs a graceful shutdown of the Spring Boot application.
 *
 * The Spring application context is closed instead of terminating
 * the JVM directly, allowing Spring Boot's graceful shutdown process
 * to finish active requests cleanly.
 */
@Service
public class ShutdownService {

    // Represents the currently running Spring application context.
    private final ConfigurableApplicationContext applicationContext;

    /**
     * Constructor injection allows Spring to provide the active
     * application context.
     */
    public ShutdownService(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Starts the graceful shutdown process on a separate thread.
     *
     * A separate thread is used so the current HTTP shutdown request
     * can return its response before the application context is closed.
     */
    public void shutdown() {

        // Closing the application context triggers Spring Boot's
        // configured graceful shutdown sequence.
        Thread shutdownThread =
                new Thread(() -> applicationContext.close());

        // Give the shutdown thread a meaningful name for diagnostics.
        shutdownThread.setName("graceful-shutdown-thread");

        shutdownThread.start();
    }
}