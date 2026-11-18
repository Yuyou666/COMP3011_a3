package comp3011.assignment1.controller;

import comp3011.assignment1.dto.SubtitleUsageResponse;
import comp3011.assignment1.service.SubtitleUsageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Returns cumulative Whisper audio duration, in seconds, since server startup. */
@RestController
public class UsageController {
    private final SubtitleUsageService usage;

    public UsageController(SubtitleUsageService usage) {
        this.usage = usage;
    }

    @GetMapping("/api/v1/global/usage")
    public SubtitleUsageResponse usage() {
        return new SubtitleUsageResponse(usage.totalSeconds());
    }
}
