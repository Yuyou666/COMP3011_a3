package comp3011;

import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/** Bounded background retry for errors known to occur before a request reaches the server. */
final class SubtitleRetry {
    private SubtitleRetry() {}

    // Five delays and six total attempts; gives a temporarily restarting server
    // about 19 seconds to become reachable without blocking the JavaFX thread.
    private static final long[] DELAYS_MS = {1000, 2000, 3000, 5000, 8000};

    static <T> T call(Callable<T> action, Predicate<Exception> mayRetry) throws Exception {
        return call(action, mayRetry, DELAYS_MS);
    }

    // Package-private deterministic test seam: zero-delay retries need no real network.
    static <T> T call(Callable<T> action, Predicate<Exception> mayRetry, long[] delaysMs)
            throws Exception {
        for (int attempt = 0; ; attempt++) {
            try {
                return action.call();
            } catch (Exception failure) {
                if (failure instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw failure;
                }
                if (Thread.currentThread().isInterrupted() ||
                        attempt >= delaysMs.length || !mayRetry.test(failure)) {
                    throw failure;
                }
                Thread.sleep(delaysMs[attempt]);
            }
        }
    }

    /** Do NOT retry a response error or an ambiguous read/request timeout. */
    static boolean isPreConnectionFailure(Exception error) {
        // HttpConnectTimeoutException extends HttpTimeoutException; this subtype
        // signifies the HTTP connection itself was never established.
        return error instanceof ConnectException || error instanceof HttpConnectTimeoutException;
    }
}
