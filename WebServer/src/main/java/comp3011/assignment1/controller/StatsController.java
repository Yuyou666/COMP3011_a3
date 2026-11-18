package comp3011.assignment1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.dto.GlobalStatsResponse;
import comp3011.assignment1.service.GlobalStatsService;

/**
 * Provides the REST endpoint used to retrieve global
 * Speech-to-Text token usage statistics.
 */
@RestController
public class StatsController {

    // Shared service that stores cumulative input and output token counts.
    private final GlobalStatsService globalStatsService;

    /**
     * Constructor injection allows Spring to provide the shared statistics service.
     */
    public StatsController(GlobalStatsService globalStatsService) {
        this.globalStatsService = globalStatsService;
    }

    /**
     * Returns the cumulative token usage since the current server process started.
     *
     * @return the current input and output token totals
     */
    @GetMapping("/api/v1/global/stats")
    public GlobalStatsResponse getGlobalStats() {

        /*
         * The controller does not store statistics itself.
         * It reads the current values from GlobalStatsService and
         * packages them into a REST response object.
         */
        return new GlobalStatsResponse(
                globalStatsService.getInputTokens(),
                globalStatsService.getOutputTokens()
        );
    }
}