package comp3011;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SubtitleTimelineTest {
    private static void assertTwoLines42(String caption) {
        String[] lines = caption.split("\\n", -1);
        assertTrue(lines.length <= 2, "At most two subtitle lines");
        for (String line : lines) assertTrue(line.length() <= 42, "Line exceeds 42 chars: " + line);
    }

    @Test void wordTimingAndLineLimits() throws Exception {
        StringBuilder json = new StringBuilder("{\"words\":[");
        for (int i = 0; i < 35; i++) {
            if (i != 0) json.append(',');
            json.append("{\"start\":").append(i * 0.2)
                .append(",\"end\":").append(i * 0.2 + 0.19)
                .append(",\"word\":\"knowledge\"}");
        }
        json.append("]}");
        SubtitleTimeline timeline = SubtitleTimeline.fromJson(json.toString());
        assertFalse(timeline.isEmpty());
        for (double seconds = 0.0; seconds < 7.0; seconds += 0.05) {
            String caption = timeline.at(seconds);
            if (!caption.isEmpty()) assertTwoLines42(caption);
        }
    }

    @Test void segmentFallbackNeverTruncatesAndKeepsLimits() throws Exception {
        String repeated = "hello ".repeat(60);
        String json = "{\"segments\":[{\"start\":0,\"end\":12,\"text\":\"" + repeated + "\"}]}";
        SubtitleTimeline timeline = SubtitleTimeline.fromJson(json);
        assertFalse(timeline.isEmpty());
        int found = 0;
        for (double s = 0; s < 12; s += .1) {
            String caption = timeline.at(s);
            if (!caption.isEmpty()) { assertTwoLines42(caption); found++; }
        }
        assertTrue(found > 0);
    }

    @Test void longUnbrokenWordDoesNotOverflow() throws Exception {
        String json = "{\"words\":[{\"start\":0,\"end\":3,\"word\":\"" + "X".repeat(110) + "\"}]}";
        SubtitleTimeline timeline = SubtitleTimeline.fromJson(json);
        for (double s = 0; s < 3; s += .1) {
            String caption = timeline.at(s);
            if (!caption.isEmpty()) assertTwoLines42(caption);
        }
    }

    @Test void sentenceBoundaryCreatesNaturalCaptionBreak() throws Exception {
        String json = "{\"words\":["
            + "{\"start\":0.0,\"end\":0.6,\"word\":\"Hello\"},"
            + "{\"start\":0.6,\"end\":1.2,\"word\":\"world.\"},"
            + "{\"start\":1.3,\"end\":1.8,\"word\":\"How\"},"
            + "{\"start\":1.8,\"end\":2.3,\"word\":\"are\"},"
            + "{\"start\":2.3,\"end\":2.8,\"word\":\"you?\"}]}";
        SubtitleTimeline timeline = SubtitleTimeline.fromJson(json);
        assertEquals("Hello world.", timeline.at(0.7));
        assertEquals("How are you?", timeline.at(1.9));
    }

    @Test void emptyResponseAndOutOfRange() throws Exception {
        SubtitleTimeline timeline = SubtitleTimeline.fromJson("{\"words\":[],\"segments\":[]}");
        assertTrue(timeline.isEmpty());
        assertEquals("", timeline.at(10));
    }
}
