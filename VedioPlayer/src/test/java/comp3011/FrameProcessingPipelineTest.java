
package comp3011;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class FrameProcessingPipelineTest {

    @Test
    void runExistingChecks() throws Exception {
        main(new String[0]);
    }


    public static void main(String[] args) throws Exception {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();

        try (FrameProcessingPipeline<Integer> pipeline =
                 new FrameProcessingPipeline<>(3, 3)) {

            int[] delays = {180, 30, 60};

            for (int i = 0; i < delays.length; i++) {
                final int frameNumber = i;
                final int delay = delays[i];

                boolean submitted = pipeline.submit(() -> {
                    int count = active.incrementAndGet();
                    peak.accumulateAndGet(count, Math::max);

                    try {
                        Thread.sleep(delay);
                        return frameNumber;
                    } finally {
                        active.decrementAndGet();
                    }
                });

                check(submitted, "Task submitted: " + i);
            }

            check(pipeline.isFull(), "Capacity limit");
            check(!pipeline.submit(() -> 99),
                  "Full queue rejects new tasks");

            for (int i = 0; i < delays.length; i++) {
                int result = pipeline.takeNext();
                check(result == i, "Ordered result: " + i);
            }

            check(peak.get() > 1, "Concurrent execution");
            check(pipeline.pendingCount() == 0, "Queue drained");
        }

        testNonBlockingPoll();
        System.out.println("ALL PIPELINE TESTS PASSED");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("FAILED: " + message);
        }
        System.out.println("PASS: " + message);
    }
    
    private static void testNonBlockingPoll() throws Exception {
        try (FrameProcessingPipeline<Integer> pipeline =
                new FrameProcessingPipeline<>(2, 2)) {

            java.util.concurrent.CountDownLatch release =
                new java.util.concurrent.CountDownLatch(1);

            pipeline.submit(() -> {
                release.await();
                return 10;
            });

            pipeline.submit(() -> 20);

            try {
                check(pipeline.pollNext() == null,
                      "Poll does not block on unfinished frame");
            } finally {
                release.countDown();
            }

            check(pipeline.takeNext() == 10,
                  "First result remains ordered");
            check(pipeline.takeNext() == 20,
                  "Second result remains ordered");
        }
    }
}
