package comp3011.assignment1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment1.dto.OpenAiTranscriptionResponse;
import comp3011.assignment1.service.OpenAiTranscriptionService;

/**
 * Integration test for concurrent transcription requests.
 *
 * A real Spring Boot HTTP server is started on a random local port,
 * while the external OpenAI service is mocked. This allows concurrency
 * behaviour to be tested without making real API calls.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrencyTest {

    // Port automatically assigned to the Spring Boot test server.
    @LocalServerPort
    private int port;

    /*
     * Replace the real OpenAiTranscriptionService inside the Spring context
     * with a Mockito mock so no real network requests or API key are required.
     */
    @MockitoBean
    private OpenAiTranscriptionService transcriptionService;

    /**
     * Verifies that the application can handle more than 200 simultaneous
     * blocking HTTP transcription requests within a reasonable time.
     */
    @Test
    void shouldHandleMoreThan200ConcurrentBlockingHttpRequests()
            throws Exception {

        int requestCount = 250;

        /*
         * Simulate a blocking cloud Speech-to-Text request.
         *
         * Each mocked request waits for one second before returning a result.
         * If the server processed these requests sequentially, 250 requests
         * would take roughly 250 seconds.
         */
        when(transcriptionService.transcribe(any(MultipartFile.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(1000);

                    return new OpenAiTranscriptionResponse(
                            "test transcription",
                            new OpenAiTranscriptionResponse.Usage(10, 5)
                    );
                });

        // HTTP client used to send requests to the real local test server.
        RestClient client = RestClient.create(
                "http://localhost:" + port
        );

        /*
         * Store each HTTP request as a task so all requests can later
         * be submitted concurrently.
         */
        List<Callable<Integer>> requests = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {

            requests.add(() -> {

                MultipartBodyBuilder body =
                        new MultipartBodyBuilder();

                /*
                 * Create a small in-memory fake audio file.
                 * The actual audio contents are irrelevant because
                 * the transcription service is mocked.
                 */
                ByteArrayResource audio =
                        new ByteArrayResource(
                                "fake audio".getBytes(StandardCharsets.UTF_8)
                        ) {
                            @Override
                            public String getFilename() {
                                return "test.webm";
                            }
                        };

                // The field name "file" matches @RequestPart("file")
                // in TranscriptionController.
                body.part("file", audio);

                // Send one multipart POST request and return its HTTP status code.
                return client.post()
                        .uri("/api/v1/transcriptions")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(body.build())
                        .retrieve()
                        .toEntity(String.class)
                        .getStatusCode()
                        .value();
            });
        }

        // Record the start time so total concurrent execution time can be measured.
        Instant start = Instant.now();

        /*
         * Use one lightweight virtual thread for each client request.
         * invokeAll submits the entire collection of request tasks and
         * waits until they have all completed.
         */
        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<Integer>> results =
                    executor.invokeAll(requests);

            // Every transcription request should complete successfully.
            for (Future<Integer> result : results) {
                assertEquals(200, result.get());
            }
        }

        long elapsedMilliseconds =
                Duration.between(start, Instant.now()).toMillis();

        // Print the measured time to make the concurrency result visible.
        System.out.println(
                requestCount
                + " concurrent blocking HTTP requests completed in "
                + elapsedMilliseconds
                + " ms"
        );

        /*
         * Since every simulated cloud request blocks for one second,
         * completing 250 requests in under eight seconds demonstrates
         * that requests are being handled concurrently rather than sequentially.
         */
        assertTrue(
                elapsedMilliseconds < 8000,
                "Concurrent requests took too long: "
                        + elapsedMilliseconds + " ms"
        );
    }
}