package comp3011.assignment1.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment1.dto.OpenAiTranscriptionResponse;
import comp3011.assignment1.dto.TranscriptionResponse;
import comp3011.assignment1.service.GlobalStatsService;
import comp3011.assignment1.service.OpenAiTranscriptionService;

/**
 * Provides the REST endpoint for Speech-to-Text transcription.
 *
 * The controller receives an uploaded audio file, delegates the
 * transcription work to OpenAiTranscriptionService, updates the
 * global token statistics, and returns the transcribed text.
 */
@RestController
public class TranscriptionController {

    // Service responsible for communicating with the external OpenAI STT API.
    private final OpenAiTranscriptionService transcriptionService;

    // Shared service used to track cumulative input and output token usage.
    private final GlobalStatsService globalStatsService;

    /**
     * Constructor injection allows Spring to provide the required services.
     */
    public TranscriptionController(
            OpenAiTranscriptionService transcriptionService,
            GlobalStatsService globalStatsService) {

        this.transcriptionService = transcriptionService;
        this.globalStatsService = globalStatsService;
    }

    /**
     * Accepts an audio file as multipart/form-data and returns its transcription.
     *
     * @param audioFile the audio recording uploaded by the browser
     * @return the transcribed text
     */
    @PostMapping(
            value = "/api/v1/transcriptions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public TranscriptionResponse transcribe(
            @RequestPart("file") MultipartFile audioFile) {

        /*
         * Delegate the external API call to the service layer rather than
         * placing OpenAI-specific HTTP logic inside the controller.
         */
        OpenAiTranscriptionResponse result =
                transcriptionService.transcribe(audioFile);

        /*
         * If the OpenAI response includes usage information, add it to the
         * shared global statistics. The service handles thread-safe updates.
         */
        if (result.usage() != null) {
            globalStatsService.addUsage(
                    result.usage().inputTokens(),
                    result.usage().outputTokens()
            );
        }

        // Only expose the transcription text to the browser.
        return new TranscriptionResponse(result.text());
    }
}