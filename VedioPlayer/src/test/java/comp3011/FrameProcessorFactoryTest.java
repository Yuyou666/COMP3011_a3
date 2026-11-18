package comp3011;

import org.junit.jupiter.api.Test;

import java.util.List;

/** Run as Java Application; does not require JUnit or a video file. */
public final class FrameProcessorFactoryTest {

    @Test
    void runExistingChecks() throws Exception {
        main(new String[0]);
    }

    public static void main(String[] args) {
        List<FrameProcessor> original = List.of(
            new FrameJitterer(), new FrameFlickerer(), new FrameJitterer(),
            new FrameScratcher(), new FrameDuster(), new FrameMottler(),
            new FramePepperer(), new FrameNumberer(), new FrameBleeder(),
            new FrameBlackAndWhiter(), new FrameYellower(), new FrameVignetter()
        );
        List<FrameProcessor> first = FrameProcessorFactory.copyChain(original);
        List<FrameProcessor> second = FrameProcessorFactory.copyChain(original);
        check(first.size() == original.size(), "Chain length");
        for (int i = 0; i < original.size(); i++) {
            check(original.get(i).getClass() == first.get(i).getClass(), "Type at " + i);
            check(first.get(i) != original.get(i) && first.get(i) != second.get(i),
                  "Distinct instances at " + i);
        }
        check(((FrameJitterer)first.get(0)).instanceIndex() ==
              ((FrameJitterer)second.get(0)).instanceIndex(), "Same seed index");
        check(((FrameJitterer)first.get(0)).instanceIndex() !=
              ((FrameJitterer)first.get(2)).instanceIndex(), "Repeated jitter independent");
        System.out.println("ALL FACTORY TESTS PASSED");
    }
    private static void check(boolean result, String label) {
        if (!result) throw new AssertionError("FAIL: " + label);
        System.out.println("PASS: " + label);
    }
}
