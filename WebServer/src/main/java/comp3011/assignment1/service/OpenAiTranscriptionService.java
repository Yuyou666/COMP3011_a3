package comp3011.assignment1.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment1.dto.OpenAiTranscriptionResponse;

/**
 * Handles communication with the OpenAI Speech-to-Text API.
 *
 * This service receives an audio file from the controller, builds a
 * multipart HTTP request, sends it to OpenAI, and converts the JSON
 * response into an OpenAiTranscriptionResponse object.
 */
@Service
public class OpenAiTranscriptionService {

    // API key is read from the operating system environment rather than
    // being hard-coded in the source code.
    private final String apiKey;

    // HTTP client used to communicate with the OpenAI API.
    private final RestClient restClient;

    /**
     * Creates the transcription service and configures the OpenAI base URL.
     *
     * Spring injects the RestClient.Builder through constructor injection.
     */
    public OpenAiTranscriptionService(
            RestClient.Builder restClientBuilder) {

        this.apiKey = System.getenv("OPENAI_API_KEY");

        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com")
                .build();
    }

    /**
     * Sends an uploaded audio file to the OpenAI transcription API.
     *
     * @param audioFile audio received from the browser
     * @return the transcription text and token usage returned by OpenAI
     */
    public OpenAiTranscriptionResponse transcribe(MultipartFile audioFile) {

        // Fail early if the required API key is unavailable.
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY environment variable is not configured.");
        }

        // OpenAI expects the model and audio file as multipart/form-data.
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();

        bodyBuilder.part("model", "gpt-4o-mini-transcribe");

        /*
         * Attach the uploaded audio file.
         * If no original filename is available, use a WebM filename as a fallback.
         */
        bodyBuilder
                .part("file", audioFile.getResource())
                .filename(
                        audioFile.getOriginalFilename() != null
                                ? audioFile.getOriginalFilename()
                                : "recording.webm"
                );

        /*
         * Send the authenticated POST request to OpenAI and deserialize
         * the JSON response into OpenAiTranscriptionResponse.
         */
        return restClient.post()
                .uri("/v1/audio/transcriptions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(bodyBuilder.build())
                .retrieve()
                .body(OpenAiTranscriptionResponse.class);
    }
}