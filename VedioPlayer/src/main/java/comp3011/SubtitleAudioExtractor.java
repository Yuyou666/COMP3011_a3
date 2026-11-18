package comp3011;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

/**
 * Extracts audio using the FFmpeg libraries already bundled through JavaCV.
 * No external ffmpeg.exe or installation on the grading computer is required.
 * This method must only be called on a background thread.
 */
final class SubtitleAudioExtractor {
    private SubtitleAudioExtractor() { }

    static void extractMp3(File video, Path destination) throws Exception {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(video)) {
            grabber.start();
            if (!grabber.hasAudio() || grabber.getSampleRate() <= 0) {
                throw new IllegalStateException("Video has no usable audio track");
            }
            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(destination.toFile(), 0, 0, 1)) {
                recorder.setFormat("mp3");
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP3);
                recorder.setSampleRate(16_000);
                recorder.setAudioChannels(1);
                recorder.setAudioBitrate(64_000);
                recorder.start();
                int frames = 0;
                Frame audio;
                while ((audio = grabber.grabSamples()) != null) {
                    if (audio.samples != null && audio.samples.length > 0) {
                        recorder.recordSamples(grabber.getSampleRate(),
                                grabber.getAudioChannels(), audio.samples);
                        frames++;
                    }
                }
                if (frames == 0) {
                    throw new IllegalStateException("No decodable audio samples in video");
                }
            }
        }
        if (!Files.isRegularFile(destination) || Files.size(destination) == 0) {
            throw new IllegalStateException("Audio extraction produced an empty file");
        }
    }
}
