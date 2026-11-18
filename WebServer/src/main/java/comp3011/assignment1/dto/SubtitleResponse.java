package comp3011.assignment1.dto;

import java.util.List;

/** Server-owned format for the A3 player; timings are in seconds. */
public record SubtitleResponse(List<Segment> segments, List<Word> words) {
    public record Segment(double start, double end, String text) { }
    public record Word(double start, double end, String word) { }
}
