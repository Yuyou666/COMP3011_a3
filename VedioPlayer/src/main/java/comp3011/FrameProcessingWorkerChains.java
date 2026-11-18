package comp3011;

import java.util.List;

/** One independently initialised processor chain per executor worker thread. */
public final class FrameProcessingWorkerChains {
    private final ThreadLocal<List<FrameProcessor>> chains;

    public FrameProcessingWorkerChains(List<FrameProcessor> originals, InfoVideo info) {
        List<FrameProcessor> templates = List.copyOf(originals);
        chains = ThreadLocal.withInitial(() -> {
            List<FrameProcessor> copy = FrameProcessorFactory.copyChain(templates);
            try {
                for (FrameProcessor processor : copy) {
                    processor.initialise(info);
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot initialise worker effects", ex);
            }
            return copy;
        });
    }

    public FrameProcessingJob process(FrameProcessingJob job) throws Exception {
        // The task owns the frame and closes it if anything fails, including
        // chain initialisation and an interrupted worker.
        List<FrameProcessor> local;
        try {
            local = chains.get();
        } catch (Exception | Error ex) {
            job.frame().close();
            throw ex;
        }
        return new FrameProcessingTask(job, local).call();
    }
}
