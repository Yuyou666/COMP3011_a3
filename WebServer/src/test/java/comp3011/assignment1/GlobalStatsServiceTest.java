package comp3011.assignment1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import comp3011.assignment1.service.GlobalStatsService;

/**
 * Unit tests for GlobalStatsService.
 *
 * These tests verify both normal token accumulation and
 * thread-safe updates when many threads modify the counters concurrently.
 */
class GlobalStatsServiceTest {

    /**
     * Verifies that token usage from multiple transcription requests
     * is accumulated correctly.
     */
    @Test
    void tokenUsageShouldAccumulateCorrectly() {

        GlobalStatsService service =
                new GlobalStatsService();

        // Simulate token usage from two completed transcription requests.
        service.addUsage(100, 20);
        service.addUsage(50, 10);

        // Input tokens should be added together.
        assertEquals(
                150,
                service.getInputTokens()
        );

        // Output tokens should also be added together.
        assertEquals(
                30,
                service.getOutputTokens()
        );
    }

    /**
     * Verifies that token counters remain correct when many threads
     * update the statistics at the same time.
     *
     * This specifically tests the AtomicLong-based implementation
     * used by GlobalStatsService.
     */
    @Test
    void tokenUsageShouldBeThreadSafe() throws InterruptedException {

        GlobalStatsService service =
                new GlobalStatsService();

        int threadCount = 250;

        // Store all worker threads so they can be started and joined.
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {

            /*
             * Each thread represents one concurrent update
             * to the shared global statistics.
             */
            threads[i] = new Thread(() -> {
                service.addUsage(10, 5);
            });

            threads[i].start();
        }

        /*
         * Wait for every thread to finish before checking the totals.
         * Without join(), assertions could run before all updates complete.
         */
        for (Thread thread : threads) {
            thread.join();
        }

        // 250 threads × 10 input tokens = 2500.
        assertEquals(
                2500,
                service.getInputTokens()
        );

        // 250 threads × 5 output tokens = 1250.
        assertEquals(
                1250,
                service.getOutputTokens()
        );
    }
}