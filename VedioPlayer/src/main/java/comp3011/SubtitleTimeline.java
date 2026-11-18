package comp3011;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Immutable word-aligned captions, rendered as at most two 42-character lines. */
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
                if (token.isBlank() || token.length() > 42
                        || word.path("end").asDouble() <= word.path("start").asDouble()) continue;
                if (!batch.isEmpty()) {
                    List<JsonNode> proposed = new ArrayList<>(batch);
                    proposed.add(word);
                    String formatted = format(proposed);
                    double duration = word.path("end").asDouble() - batch.get(0).path("start").asDouble();
                    if (formatted.length() > 85 || formatted.chars().filter(c -> c == '\n').count() > 1
                            || duration > 4.0 || batch.size() >= 12) {
                        append(cues,batch,format(batch)); batch.clear();
                    }
                }
                batch.add(word);
            }
            append(cues,batch,format(batch));
        }
        if (cues.isEmpty()) {
            for (JsonNode s : root.path("segments")) {
                double start = s.path("start").asDouble();
                double end = s.path("end").asDouble();
                if (end > start) {
                    String[] tokens = s.path("text").asText("").strip().split("\\s+");
                    StringBuilder caption = new StringBuilder();
                    for (String token : tokens) {
                        if (caption.length() + token.length() + 1 > 84 && !caption.isEmpty()) break;
                        caption.append(caption.isEmpty() ? "" : " ").append(token);
                    }
                    cues.add(new Cue(start,end, wrap(caption.toString())));
                }
            }
        }
        return new SubtitleTimeline(cues);
    }
    private static String format(List<JsonNode> words) {
        StringBuilder result = new StringBuilder();
        for (JsonNode word : words) {
            String token = word.path("word").asText("").strip();
            if (token.isEmpty()) continue;
            String last = result.toString().contains("\n") ? result.substring(result.lastIndexOf("\n")+1) : result.toString();
            if (!last.isEmpty() && last.length() + token.length() + 1 > 42) result.append('\n');
            else if (!last.isEmpty()) result.append(' ');
            result.append(token);
        }
        return result.toString();
    }
    private static String wrap(String text) {
        StringBuilder result = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String last = result.toString().contains("\n") ? result.substring(result.lastIndexOf("\n")+1) : result.toString();
            if (!last.isEmpty() && last.length()+word.length()+1 > 42) result.append('\n');
            else if (!last.isEmpty()) result.append(' ');
            result.append(word);
        }
        return result.toString();
    }
    private static void append(List<Cue> cues,List<JsonNode> batch,String caption) {
        if (batch.isEmpty() || caption.isBlank()) return;
        double start=batch.get(0).path("start").asDouble();
        double end=batch.get(batch.size()-1).path("end").asDouble();
        if(end>start) cues.add(new Cue(start,end,caption));
    }
    public String at(double seconds) {
        for (Cue cue : cues) if (seconds >= cue.start && seconds < cue.end) return cue.text;
        return "";
    }
}
