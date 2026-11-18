package comp3011.assignment1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the parts of the OpenAI transcription API response
 * that are required by this application.
 *
 * Unknown JSON fields are ignored so the application is not tightly
 * coupled to every field returned by the external API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiTranscriptionResponse(
        String text,
        Usage usage
) {

    /**
     * Represents token usage information returned by the OpenAI API.
     *
     * JsonProperty is used because the API returns snake_case field names,
     * while the Java code uses camelCase naming.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @JsonProperty("input_tokens") long inputTokens,
            @JsonProperty("output_tokens") long outputTokens
    ) {
    }
}