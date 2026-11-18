package comp3011;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Background-only adapter. Caption JSON is cached locally across player/server restarts. */
public final class SubtitleClient {
    private SubtitleClient() {}
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private static final URI ENDPOINT = URI.create("http://localhost:8080/api/v1/subtitles");
    private static final Path CACHE_DIRECTORY = Path.of(
            System.getProperty("user.home"), ".comp3011", "subtitle-cache");

    public static SubtitleTimeline fetch(File video) throws Exception {
        if (!video.isFile()) throw new IllegalArgumentException("Video file not found");
        // Hash the video content on the background caller thread, not the JavaFX thread.
        // File-content identity means a renamed/moved identical file uses the same captions.
        Path cacheFile = CACHE_DIRECTORY.resolve(videoHash(video) + ".json");
        try {
            if (Files.isRegularFile(cacheFile)) {
                String cachedJson = Files.readString(cacheFile, StandardCharsets.UTF_8);
                return SubtitleTimeline.fromJson(cachedJson);
            }
        } catch (Exception corruptOrUnreadableCache) {
            // A broken cache must never prevent fetching a fresh transcript.
        }

        Path temp = Files.createTempFile("comp3011-a3-audio-", ".mp3");
        try {
            Process process;
            try {
                process = new ProcessBuilder("ffmpeg", "-nostdin", "-y", "-loglevel", "error",
                        "-i", video.getAbsolutePath(), "-vn", "-ac", "1", "-ar", "16000",
                        "-codec:a", "libmp3lame", temp.toString())
                        .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            } catch (java.io.IOException e) {
                throw new IllegalStateException("FFmpeg unavailable (check PATH)", e);
            }
            if (!process.waitFor(90, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Audio extraction timed out");
            }
            if (process.exitValue() != 0 || Files.size(temp) == 0) {
                throw new IllegalStateException("Could not extract audio from video");
            }
            byte[] audio = Files.readAllBytes(temp);
            String boundary = "----A3" + UUID.randomUUID().toString().replace("-", "");
            byte[] before = ("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"audio.mp3\"\r\n"
                    + "Content-Type: audio/mpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            byte[] after = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(ENDPOINT)
                    .timeout(Duration.ofMinutes(3))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArrays(List.of(before, audio, after)))
                    .build();
            // Only retry failures before a connection is established. An ambiguous HTTP
            // timeout might mean Whisper was already called; do not automatically resend it.
            HttpResponse<String> response = SubtitleRetry.call(
                    () -> HTTP.send(req, HttpResponse.BodyHandlers.ofString()),
                    SubtitleRetry::isPreConnectionFailure);
            if (response.statusCode() != 200) {
                String body = response.body() == null ? "" : response.body().strip();
                if (body.length() > 160) body = body.substring(0, 160) + "...";
                throw new IllegalStateException("Subtitle server HTTP " + response.statusCode()
                        + (body.isBlank() ? "" : " - " + body));
            }
            String json = response.body();
            SubtitleTimeline parsed = SubtitleTimeline.fromJson(json); // Validate before caching.
            try {
                Files.createDirectories(CACHE_DIRECTORY);
                Path staging = Files.createTempFile(CACHE_DIRECTORY, "subtitle-", ".tmp");
                try {
                    Files.writeString(staging, json, StandardCharsets.UTF_8);
                    Files.move(staging, cacheFile, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(staging);
                }
            } catch (Exception cannotCache) {
                // Display is more important than disk caching (e.g. read-only home directory).
            }
            return parsed;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static String videoHash(File video) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(video.toPath())) {
            byte[] block = new byte[64 * 1024];
            int count;
            while ((count = in.read(block)) != -1) digest.update(block, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
