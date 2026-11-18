package comp3011.assignment1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import comp3011.assignment1.controller.TranscriptionController;
import comp3011.assignment1.dto.OpenAiTranscriptionResponse;
import comp3011.assignment1.dto.TranscriptionResponse;
import comp3011.assignment1.service.GlobalStatsService;
import comp3011.assignment1.service.OpenAiTranscriptionService;

/**
 * Unit test for TranscriptionController.
 *
 * This test verifies that a successful transcription request
 * returns the expected text and also updates the global token statistics.
 */
class TranscriptionControllerTest {

    /**
     * Verifies the complete controller flow using a mocked transcription service.
     *
     * The external OpenAI API is not called. Instead, Mockito provides
     * a predefined transcription response and token usage.
     */
    @Test
    void transcriptionShouldReturnTextAndUpdateStats() {

        /*
         * Mock the external transcription service so the test does not
         * require a real API key or network connection.
         */
        OpenAiTranscriptionService transcriptionService =
                mock(OpenAiTranscriptionService.class);

        // Use the real statistics service so token updates can be verified.
        GlobalStatsService statsService =
                new GlobalStatsService();

        /*
         * Configure the mocked service to return a fixed transcription
         * and token usage whenever any audio file is provided.
         */
        when(transcriptionService.transcribe(any()))
                .thenReturn(
                        new OpenAiTranscriptionResponse(
                                "hello world",
                                new OpenAiTranscriptionResponse.Usage(12, 3)
                        )
                );

        // Construct the controller with the mocked STT service and real stats service.
        TranscriptionController controller =
                new TranscriptionController(
                        transcriptionService,
                        statsService
                );

        /*
         * Create a small fake multipart audio file that behaves like
         * a file uploaded through the HTTP transcription endpoint.
         */
        MockMultipartFile audioFile =
                new MockMultipartFile(
                        "file",
                        "test.webm",
                        "audio/webm",
                        new byte[] {1, 2, 3}
                );

        // Call the controller method directly.
        TranscriptionResponse response =
                controller.transcribe(audioFile);

        // The controller should return the transcription text from the service.
        assertEquals(
                "hello world",
                response.text()
        );

        // Token usage from the transcription response should update global statistics.
        assertEquals(
                12,
                statsService.getInputTokens()
        );

        assertEquals(
                3,
                statsService.getOutputTokens()
        );
    }
}