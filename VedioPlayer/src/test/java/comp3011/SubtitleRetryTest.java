package comp3011;

import static org.junit.jupiter.api.Assertions.*;
import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SubtitleRetryTest {
    @Test void recoversWhenServerStartsLater() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String text = SubtitleRetry.call(() -> {
            if (calls.incrementAndGet() < 3) throw new ConnectException("refused");
            return "recovered";
        }, SubtitleRetry::isPreConnectionFailure, new long[]{0, 0, 0});
        assertEquals("recovered", text);
        assertEquals(3, calls.get());
    }

    @Test void stopsAfterRetryBudget() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(ConnectException.class, () -> SubtitleRetry.call(() -> {
            calls.incrementAndGet();
            throw new ConnectException("refused");
        }, SubtitleRetry::isPreConnectionFailure, new long[]{0, 0}));
        assertEquals(3, calls.get());
    }

    @Test void doesNotRetryAmbiguousTimeoutOrHttpFailure() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(HttpTimeoutException.class, () -> SubtitleRetry.call(() -> {
            calls.incrementAndGet();
            throw new HttpTimeoutException("response timeout");
        }, SubtitleRetry::isPreConnectionFailure, new long[]{0, 0}));
        assertEquals(1, calls.get());
        assertTrue(SubtitleRetry.isPreConnectionFailure(new HttpConnectTimeoutException("connect")));
        assertFalse(SubtitleRetry.isPreConnectionFailure(new IllegalStateException("HTTP 500")));
    }
}
