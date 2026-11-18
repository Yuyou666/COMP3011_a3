package comp3011;

import java.util.List;
import java.util.concurrent.Callable;
import org.bytedeco.javacv.Frame;

/** Applies effects to one owned frame in the user-specified order. */
public final class FrameProcessingTask implements Callable<FrameProcessingJob> {
    private final FrameProcessingJob job;
    private final List<FrameProcessor> processors;

    public FrameProcessingTask(FrameProcessingJob job, List<FrameProcessor> processors) {
        this.job = job;
        this.processors = List.copyOf(processors);
    }

    @Override
    public FrameProcessingJob call() throws Exception {
        Frame frame = job.frame();
        try {
            for (FrameProcessor processor : processors) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Frame task cancelled");
                }
                processor.process(frame, job.info());
            }
            return job;
        } catch (Exception | Error ex) {
            frame.close();
            throw ex;
        }
    }
}
