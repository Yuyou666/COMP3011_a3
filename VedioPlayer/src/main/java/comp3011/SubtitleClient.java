package comp3011;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Background-only adapter: extract audio with FFmpeg and call the local A1 API. */
public final class SubtitleClient {
    private SubtitleClient() {}
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private static final URI ENDPOINT = URI.create("http://localhost:8080/api/v1/subtitles");

    public static SubtitleTimeline fetch(File video) throws Exception {
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
                    .POST(HttpRequest.BodyPublishers.ofByteArrays(java.util.List.of(before, audio, after)))
                    .build();
            HttpResponse<String> response = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                String body = response.body() == null ? "" : response.body().strip();
                if (body.length() > 160) body = body.substring(0, 160) + "...";
                throw new IllegalStateException("Subtitle server HTTP " + response.statusCode()
                        + (body.isBlank() ? "" : " - " + body));
            }
            return SubtitleTimeline.fromJson(response.body());
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
