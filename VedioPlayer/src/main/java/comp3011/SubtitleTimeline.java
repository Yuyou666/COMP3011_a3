package comp3011;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Immutable word-aligned captions; every cue contains at most two 42-character lines. */
public final class SubtitleTimeline {
    private record Cue(double start, double end, String text) {}
    private final List<Cue> cues;
    private SubtitleTimeline(List<Cue> cues) { this.cues = List.copyOf(cues); }
    public static SubtitleTimeline empty() { return new SubtitleTimeline(List.of()); }
    public boolean isEmpty() { return cues.isEmpty(); }

    public static SubtitleTimeline fromJson(String json) throws Exception {
        JsonNode root = new ObjectMapper().readTree(json);
        if (root == null || !root.isObject()) throw new IllegalArgumentException("Invalid subtitle response");
        List<Cue> cues = new ArrayList<>();
        JsonNode words = root.path("words");
        if (words.isArray() && !words.isEmpty()) {
            List<JsonNode> batch = new ArrayList<>();
            for (JsonNode word : words) {
                String token = word.path("word").asText("").strip();
                double start = word.path("start").asDouble();
                double end = word.path("end").asDouble();
                if (token.isBlank() || end <= start) continue;
                // Rare >42-char words are broken into safe lines rather than overflowing.
                if (token.length() > 42) {
                    flush(cues, batch);
                    batch.clear();
                    addTextCues(cues, start, end, token);
                    continue;
                }
                if (!batch.isEmpty()) {
                    List<JsonNode> proposed = new ArrayList<>(batch);
                    proposed.add(word);
                    double duration = end - batch.get(0).path("start").asDouble();
                    // Start a new caption at a natural sentence boundary when possible.
                    String lastToken = batch.get(batch.size() - 1).path("word").asText("").strip();
                    boolean sentenceEnd = lastToken.matches(".*[.!?…][\"']?");
                    boolean naturalBreak = sentenceEnd &&
                            (batch.get(batch.size() - 1).path("end").asDouble()
                                - batch.get(0).path("start").asDouble()) >= 1.0;
                    if (naturalBreak || linesForWords(proposed).size() > 2
                            || duration > 7.0 || batch.size() >= 18) {
                        flush(cues, batch);
                        batch.clear();
                    }
                }
                batch.add(word);
            }
            flush(cues, batch);
        }
        if (cues.isEmpty()) {
            for (JsonNode segment : root.path("segments")) {
                double start = segment.path("start").asDouble();
                double end = segment.path("end").asDouble();
                if (end > start) addTextCues(cues, start, end, segment.path("text").asText(""));
            }
        }
        return new SubtitleTimeline(cues);
    }

    private static void flush(List<Cue> cues, List<JsonNode> batch) {
        if (batch.isEmpty()) return;
        double start = batch.get(0).path("start").asDouble();
        double end = batch.get(batch.size() - 1).path("end").asDouble();
        if (end > start) cues.add(new Cue(start, end, String.join("\n", linesForWords(batch))));
    }

    private static List<String> linesForWords(List<JsonNode> words) {
        List<String> tokens = new ArrayList<>();
        for (JsonNode word : words) tokens.add(word.path("word").asText("").strip());
        return wrapTokens(tokens);
    }

    private static List<String> wrapTokens(List<String> tokens) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) continue;
            // Character wrapping also supports languages without ASCII spaces.
            while (token.length() > 42) {
                if (!line.isEmpty()) { lines.add(line.toString()); line.setLength(0); }
                lines.add(token.substring(0, 42));
                token = token.substring(42);
            }
            if (token.isEmpty()) continue;
            if (!line.isEmpty() && line.length() + 1 + token.length() > 42) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(token);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    // Fallback to segment text; split into consecutive cues rather than silently truncating.
    private static void addTextCues(List<Cue> cues, double start, double end, String text) {
        List<String> lines = wrapTokens(List.of(text.strip().split("\\s+")));
        if (lines.isEmpty()) return;
        int cueCount = (lines.size() + 1) / 2;
        for (int i = 0; i < cueCount; i++) {
            int from = i * 2;
            String caption = String.join("\n", lines.subList(from, Math.min(from + 2, lines.size())));
            double cueStart = start + (end - start) * i / cueCount;
            double cueEnd = start + (end - start) * (i + 1) / cueCount;
            cues.add(new Cue(cueStart, cueEnd, caption));
        }
    }

    public String at(double seconds) {
        for (Cue cue : cues) if (seconds >= cue.start && seconds < cue.end) return cue.text;
        return "";
    }
}
