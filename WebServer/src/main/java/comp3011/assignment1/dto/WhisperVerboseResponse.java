package comp3011.assignment1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Only the fields needed from Whisper's verbose_json response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WhisperVerboseResponse(
        Double duration,
        List<Segment> segments,
        List<Word> words) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(double start, double end, String text,
            @JsonProperty("no_speech_prob") Double noSpeechProb,
            @JsonProperty("avg_logprob") Double avgLogProb) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Word(double start, double end, String word) { }
}
