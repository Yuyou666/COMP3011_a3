package comp3011;

import org.bytedeco.javacv.Frame;

/** A cloned video frame plus its playback metadata. */
public record FrameProcessingJob(Frame frame, InfoFrame info, long generation,
        long logicalTimestampUs, long targetTimeNs) {}
