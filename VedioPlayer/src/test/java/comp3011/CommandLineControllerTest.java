
package comp3011;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Standalone regression tests for command-line parsing.
 * Run as a Java Application without requiring JUnit.
 */
public class CommandLineControllerTest {

    @Test
    void runExistingChecks() throws Exception {
        main(new String[0]);
    }


    private static int passed = 0;

    public static void main(String[] args) throws IOException {
        File video = Files.createTempFile("comp3011-cli-", ".mp4").toFile();

        try {
            String path = video.getAbsolutePath();

            // Test all eleven frame-effect short options and their expected processors.
            checkProcessors("Short options",
                new String[] {"-nrfwyvdjmbp", path},
                "FrameNumberer", "FrameScratcher", "FrameFlickerer",
                "FrameBlackAndWhiter", "FrameYellower",
                "FrameVignetter", "FrameDuster", "FrameJitterer",
                "FrameMottler", "FrameBleeder", "FramePepperer");

            // Long options must produce the same ordered sequence.
            checkProcessors("Long options",
                new String[] {
                    "--number-frames", "--scratch-frames",
                    "--flicker-frames", "--black-and-white",
                    "--yellow-frames", "--vignette-frames",
                    "--dust-frames", "--jitter-frames",
                    "--mottle-frames", "--bleed-frames",
                    "--pepper-frames", path
                },
                "FrameNumberer", "FrameScratcher", "FrameFlickerer",
                "FrameBlackAndWhiter", "FrameYellower",
                "FrameVignetter", "FrameDuster", "FrameJitterer",
                "FrameMottler", "FrameBleeder", "FramePepperer");

            // Repeated processors must remain in their original positions.
            checkProcessors("Repeated effects",
                new String[] {"-nrrnf", path},
                "FrameNumberer", "FrameScratcher",
                "FrameScratcher", "FrameNumberer",
                "FrameFlickerer");

            checkProcessors("Mixed options",
                new String[] {
                    "-jf", "--dust-frames", "-d",
                    "--number-frames", path
                },
                "FrameJitterer", "FrameFlickerer",
                "FrameDuster", "FrameDuster", "FrameNumberer");

            CommandLineController repeated =
                new CommandLineController(new String[] {"-rr", path});

            List<FrameProcessor> processors =
                repeated.getFrameProcessors();

            check("Repeated instances are distinct",
                processors.size() == 2
                && processors.get(0) != processors.get(1));

            CommandLineController original =
                new CommandLineController(new String[] {
                    "-ax1", path
                });

            check("Original options remain supported",
                original.shouldLaunchApplication()
                && original.isAudioRequested()
                && original.isMaximiseRequested()
                && Integer.valueOf(1).equals(original.getDisplayId()));

            CommandLineController shortSubtitles =
                new CommandLineController(new String[] {"-s", path});
            check("Subtitle short option enabled",
                shortSubtitles.shouldLaunchApplication()
                && shortSubtitles.isSubtitleRequested()
                && shortSubtitles.getFrameProcessors().isEmpty());

            CommandLineController longSubtitles =
                new CommandLineController(new String[] {"--subtitle-frames", path});
            check("Subtitle long option enabled",
                longSubtitles.shouldLaunchApplication()
                && longSubtitles.isSubtitleRequested());

            CommandLineController both =
                new CommandLineController(new String[] {"-srn", path});
            check("Subtitle option stacks with effects",
                both.shouldLaunchApplication()
                && both.isSubtitleRequested()
                && both.getFrameProcessors().size() == 2
                && both.getFrameProcessors().get(0) instanceof FrameScratcher
                && both.getFrameProcessors().get(1) instanceof FrameNumberer);

            check("Scratch long option preserved",
                new CommandLineController(new String[] {"--scratch-frames", path})
                    .getFrameProcessors().get(0) instanceof FrameScratcher);

            check("Unknown option rejected",
                !new CommandLineController(
                    new String[] {"-z", path}
                ).shouldLaunchApplication());

            check("Conflicting monitors rejected",
                !new CommandLineController(
                    new String[] {"-12", path}
                ).shouldLaunchApplication());

            check("Multiple video paths rejected",
                !new CommandLineController(
                    new String[] {path, path}
                ).shouldLaunchApplication());

            check("Missing video rejected",
                !new CommandLineController(
                    new String[] {"-n"}
                ).shouldLaunchApplication());

            CommandLineController help =
                new CommandLineController(new String[] {"--help"});

            check("Help accepted without a file",
                help.getErrorMessage() == null
                && !help.shouldLaunchApplication());

            System.out.println();
            System.out.println("ALL TESTS PASSED: " + passed);

        } finally {
            Files.deleteIfExists(video.toPath());
        }
    }

    private static void checkProcessors(
        String name, String[] args, String... expected) {

        CommandLineController cli = new CommandLineController(args);
        List<FrameProcessor> actual = cli.getFrameProcessors();

        boolean valid = cli.shouldLaunchApplication()
            && actual.size() == expected.length;

        if (valid) {
            for (int i = 0; i < expected.length; i++) {
                if (!actual.get(i).getClass()
                    .getSimpleName().equals(expected[i])) {
                    valid = false;
                    break;
                }
            }
        }

        check(name, valid);
    }

    private static void check(String name, boolean condition) {
        if (!condition) {
            throw new AssertionError("FAIL: " + name);
        }
        passed++;
        System.out.println("PASS: " + name);
    }
}
