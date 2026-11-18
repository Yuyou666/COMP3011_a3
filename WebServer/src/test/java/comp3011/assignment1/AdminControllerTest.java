package comp3011.assignment1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import comp3011.assignment1.controller.AdminController;
import comp3011.assignment1.dto.UptimeResponse;
import comp3011.assignment1.service.ServerLifecycleService;
import comp3011.assignment1.service.ShutdownService;

/**
 * Unit tests for AdminController.
 *
 * These tests verify the uptime response and the behaviour of
 * repeated graceful shutdown requests.
 */
class AdminControllerTest {

    /**
     * Verifies that the uptime endpoint returns valid timestamps
     * and a non-negative uptime value.
     */
    @Test
    void uptimeShouldReturnValidValues() {

        // Use a real lifecycle service because its behaviour is simple
        // and does not depend on external systems.
        ServerLifecycleService lifecycleService =
                new ServerLifecycleService();

        /*
         * ShutdownService is mocked because this test does not need
         * to actually close the Spring application context.
         */
        ShutdownService shutdownService =
                mock(ShutdownService.class);

        // Construct the controller directly with its dependencies.
        AdminController controller =
                new AdminController(lifecycleService, shutdownService);

        // Call the controller method being tested.
        UptimeResponse response =
                controller.getServerUptime();

        // Both timestamps should always be present.
        assertNotNull(response.utcServerStart());
        assertNotNull(response.utcNow());

        // Current time must not be earlier than the recorded server start time.
        assertTrue(
                !response.utcNow().isBefore(response.utcServerStart())
        );

        // Uptime should never be negative.
        assertTrue(
                response.serverUptimeSeconds() >= 0
        );
    }

    /**
     * Verifies that only the first shutdown request is accepted.
     *
     * The first request should return HTTP 202 Accepted, while
     * a second request should return HTTP 409 Conflict.
     */
    @Test
    void secondShutdownRequestShouldReturnConflict() {

        ServerLifecycleService lifecycleService =
                new ServerLifecycleService();

        // Mock the shutdown service so the test does not stop the application.
        ShutdownService shutdownService =
                mock(ShutdownService.class);

        AdminController controller =
                new AdminController(lifecycleService, shutdownService);

        // Send two shutdown requests to the same controller instance.
        var firstResponse = controller.shutdownServer();
        var secondResponse = controller.shutdownServer();

        // The first request should be accepted.
        assertEquals(
                202,
                firstResponse.getStatusCode().value()
        );

        // A repeated request should be rejected because shutdown is already marked.
        assertEquals(
                409,
                secondResponse.getStatusCode().value()
        );
    }
}