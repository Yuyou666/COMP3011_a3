package comp3011;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates independent effect chains while preserving CLI ordering, duplicates,
 * and deterministic instance indexes assigned to seeded effects.
 *
 * The returned processors must be initialised with the same InfoVideo as
 * the original chain before any frame is submitted to their worker.
 */
public final class FrameProcessorFactory {
    private FrameProcessorFactory() { }

    public static List<FrameProcessor> copyChain(List<FrameProcessor> originals) {
        List<FrameProcessor> copies = new ArrayList<>(originals.size());
        for (FrameProcessor processor : originals) {
            copies.add(copy(processor));
        }
        return List.copyOf(copies);
    }

    private static FrameProcessor copy(FrameProcessor processor) {
        if (processor instanceof FrameScratcher p) return new FrameScratcher(p.instanceIndex());
        if (processor instanceof FrameFlickerer p) return new FrameFlickerer(p.instanceIndex());
        if (processor instanceof FrameDuster p) return new FrameDuster(p.instanceIndex());
        if (processor instanceof FrameJitterer p) return new FrameJitterer(p.instanceIndex());
        if (processor instanceof FrameMottler p) return new FrameMottler(p.instanceIndex());
        if (processor instanceof FramePepperer p) return new FramePepperer(p.instanceIndex());
        if (processor instanceof FrameNumberer) return new FrameNumberer();
        if (processor instanceof FrameBlackAndWhiter) return new FrameBlackAndWhiter();
        if (processor instanceof FrameYellower) return new FrameYellower();
        if (processor instanceof FrameVignetter) return new FrameVignetter();
        if (processor instanceof FrameBleeder) return new FrameBleeder();
        throw new IllegalArgumentException("Unsupported processor: " + processor.getClass());
    }
}
