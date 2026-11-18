package comp3011.assignment1.dto;

/**
 * Response object returned to the browser after a successful
 * Speech-to-Text transcription request.
 *
 * Only the transcribed text is exposed to the client.
 *
 * @param text the transcription generated from the uploaded audio
 */
public record TranscriptionResponse(
        String text
) {
}