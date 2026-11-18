package comp3011.assignment1.dto;

/**
 * Response object containing the cumulative Speech-to-Text
 * token usage for the current server process.
 *
 * @param inputTokens total number of input tokens used
 * @param outputTokens total number of output tokens generated
 */
public record GlobalStatsResponse(
        long inputTokens,
        long outputTokens
) {
}