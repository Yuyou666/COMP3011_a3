package comp3011.assignment1.controller;

import comp3011.assignment1.dto.SubtitleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import comp3011.assignment1.service.SubtitleService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** A3 endpoint; keeps A1 transcription endpoints unchanged. */
@RestController
public class SubtitleController {
    private static final Logger LOG = LoggerFactory.getLogger(SubtitleController.class);
    private final SubtitleService subtitleService;

    public SubtitleController(SubtitleService subtitleService) {
        this.subtitleService = subtitleService;
    }

    @PostMapping(value = "/api/v1/subtitles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SubtitleResponse subtitles(@RequestPart("file") MultipartFile audio) {
        LOG.info("[A3 subtitle] POST /api/v1/subtitles accepted; uploadBytes={}", audio.getSize());
        return subtitleService.getSubtitles(audio);
    }
}
