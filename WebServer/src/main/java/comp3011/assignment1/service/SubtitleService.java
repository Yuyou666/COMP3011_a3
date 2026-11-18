package comp3011.assignment1.service;

import comp3011.assignment1.dto.SubtitleResponse;
import comp3011.assignment1.dto.WhisperVerboseResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;

/** Whisper transcription with single-flight, content-based caching. */
@Service
public class SubtitleService {
    private static final Logger LOG = LoggerFactory.getLogger(SubtitleService.class);
    private final RestClient client;
    private final SubtitleUsageService usage;
    private final String apiKey;
    private final ConcurrentHashMap<String, CompletableFuture<SubtitleResponse>> cache =
            new ConcurrentHashMap<>();

    @Autowired
    public SubtitleService(RestClient.Builder builder, SubtitleUsageService usage) {
        this(builder.baseUrl("https://api.openai.com").build(),
                usage, System.getenv("OPENAI_API_KEY"));
    }

    // Package-private constructor for tests without a network call or real API key.
    SubtitleService(RestClient client, SubtitleUsageService usage, String apiKey) {
        this.client = client;
        this.usage = usage;
        this.apiKey = apiKey;
        LOG.info("[A3 subtitle] Service ready; OPENAI_API_KEY configured={}",
                apiKey != null && !apiKey.isBlank());
    }

    public SubtitleResponse getSubtitles(MultipartFile audio) {
        if (audio.isEmpty()) {
            throw new IllegalArgumentException("Audio file must not be empty");
        }
        final byte[] bytes;
        try {
            bytes = audio.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read uploaded audio", e);
        }
        String key = digest(bytes);
        LOG.info("[A3 subtitle] Audio received; bytes={}, filenameExtension={}",
                bytes.length, safeExtension(audio.getOriginalFilename()));
        // One in-progress future per audio file: concurrent requests share the same transcription.
        CompletableFuture<SubtitleResponse> own = new CompletableFuture<>();
        CompletableFuture<SubtitleResponse> current = cache.putIfAbsent(key, own);
        if (current != null) {
            try {
                return current.join();
            } catch (CompletionException e) {
                throw new IllegalStateException("Subtitle transcription failed", e.getCause());
            }
        }
        try {
            SubtitleResponse result = requestWhisper(bytes, audio.getOriginalFilename());
            own.complete(result);
            return result;
        } catch (RuntimeException e) {
            own.completeExceptionally(e);
            cache.remove(key, own); // Never permanently cache transient failures.
            throw e;
        }
    }

    private SubtitleResponse requestWhisper(byte[] bytes, String filename) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is not configured");
        }
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("model", "whisper-1");
        body.part("response_format", "verbose_json");
        body.part("timestamp_granularities[]", "word");
        body.part("timestamp_granularities[]", "segment");
        String uploadName = filename != null && !filename.isBlank() ? filename : "audio.wav";
        body.part("file", new ByteArrayResource(bytes) {
            @Override public String getFilename() { return uploadName; }
        }).filename(uploadName);

        LOG.info("[A3 subtitle] Sending audio to Whisper; bytes={}", bytes.length);
        WhisperVerboseResponse result = client.post()
                .uri("/v1/audio/transcriptions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body.build())
                .retrieve()
                .body(WhisperVerboseResponse.class);
        if (result == null) {
            throw new IllegalStateException("Whisper returned an empty response");
        }
        List<SubtitleResponse.Segment> segments = result.segments() == null ? List.of()
                : result.segments().stream()
                        .filter(s -> s.end() > s.start())
                        // Suppress likely silence/hallucination using the assignment thresholds.
                        .filter(s -> s.noSpeechProb() == null || s.noSpeechProb() <= 0.6)
                        .filter(s -> s.avgLogProb() == null || s.avgLogProb() >= -1.0)
                        .map(s -> new SubtitleResponse.Segment(s.start(), s.end(), s.text()))
                        .toList();
        List<SubtitleResponse.Word> words = result.words() == null ? List.of()
                : result.words().stream()
                        .filter(w -> w.end() > w.start())
                        // Keep word timings only when their segment survives filtering.
                        .filter(w -> segments.stream().anyMatch(s ->
                                w.start() >= s.start() && w.start() < s.end()))
                        .map(w -> new SubtitleResponse.Word(w.start(), w.end(), w.word()))
                        .toList();
        double duration = result.duration() == null ? 0.0 : result.duration();
        usage.addSeconds(duration);
        LOG.info("[A3 subtitle] Usage increased by {} seconds; total={}",
                duration, usage.totalSeconds());
        return new SubtitleResponse(segments, words);
    }

    private static String safeExtension(String filename) {
        if (filename == null) return "unknown";
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return "none";
        String ext = filename.substring(dot + 1).replaceAll("[^A-Za-z0-9]", "");
        return ext.substring(0, Math.min(10, ext.length()));
    }

    private static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
