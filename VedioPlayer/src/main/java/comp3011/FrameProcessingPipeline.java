package comp3011;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/** Bounded parallel execution with strictly ordered, non-blocking delivery. */
public final class FrameProcessingPipeline<T> implements AutoCloseable {
    private final ExecutorService executor;
    private final Deque<Entry<T>> pending = new ArrayDeque<>();
    private final int capacity;
    private boolean closed;

    public FrameProcessingPipeline(int workers, int capacity) {
        if (workers < 1 || capacity < workers) {
            throw new IllegalArgumentException("Require workers >= 1 and capacity >= workers");
        }
        executor = Executors.newFixedThreadPool(workers);
        this.capacity = capacity;
    }

    /** The one-argument form remains available for existing unit tests. */
    public boolean submit(Callable<T> task) {
        return submit(task, () -> {});
    }

    /**
     * disposeResult releases a successfully processed result that is never
     * transferred to the caller (for example because Stop/Seek cancelled it).
     * The caller retains ownership when submission returns false.
     * All pipeline control operations must run on one coordinating thread.
     */
    public boolean submit(Callable<T> task, Runnable disposeResult) {
        if (closed) {
            throw new IllegalStateException("Pipeline is closed");
        }
        if (isFull()) {
            return false;
        }
        Entry<T> entry = new Entry<>(task, disposeResult);
        try {
            entry.future = executor.submit((Callable<T>) entry::execute);
            pending.addLast(entry);
            return true;
        } catch (RejectedExecutionException ex) {
            return false;
        }
    }

    public T takeNext() throws InterruptedException, ExecutionException {
        if (closed) {
            throw new IllegalStateException("Pipeline is closed");
        }
        Entry<T> entry = pending.pollFirst();
        if (entry == null) {
            throw new IllegalStateException("No pending tasks");
        }
        T result = entry.future.get();
        entry.transfer();
        return result;
    }

    public T pollNext() throws InterruptedException, ExecutionException {
        if (closed) {
            throw new IllegalStateException("Pipeline is closed");
        }
        Entry<T> entry = pending.peekFirst();
        if (entry == null || !entry.future.isDone()) {
            return null;
        }
        pending.removeFirst();
        T result = entry.future.get();
        entry.transfer();
        return result;
    }

    public int pendingCount() {
        return pending.size();
    }

    public boolean isFull() {
        return pending.size() >= capacity;
    }

    /** Cancel without waiting on the JavaFX thread. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (Entry<T> entry : pending) {
            entry.discard();
            entry.future.cancel(true);
        }
        pending.clear();
        executor.shutdownNow();
    }

    private static final class Entry<T> {
        private enum State { NEW, RUNNING, COMPLETED, TRANSFERRED, DISPOSED }
        private final Callable<T> action;
        private final Runnable disposer;
        private State state = State.NEW;
        private boolean discardRequested;
        private Future<T> future;

        Entry(Callable<T> action, Runnable disposer) {
            this.action = action;
            this.disposer = disposer;
        }

        T execute() throws Exception {
            synchronized (this) {
                if (state == State.DISPOSED) {
                    throw new CancellationException("Frame discarded before execution");
                }
                state = State.RUNNING;
            }
            try {
                T result = action.call();
                synchronized (this) {
                    if (discardRequested) {
                        state = State.DISPOSED;
                        disposer.run();
                    } else {
                        state = State.COMPLETED;
                    }
                }
                return result;
            } catch (Exception | Error ex) {
                // The frame-processing callable is responsible for releasing
                // its frame when it throws. Avoid closing the same frame twice.
                synchronized (this) {
                    state = State.DISPOSED;
                }
                throw ex;
            }
        }

        synchronized void transfer() {
            if (state != State.COMPLETED) {
                throw new IllegalStateException("Frame result was not completed");
            }
            state = State.TRANSFERRED;
        }

        synchronized void discard() {
            if (state == State.NEW || state == State.COMPLETED) {
                state = State.DISPOSED;
                disposer.run();
            } else if (state == State.RUNNING) {
                discardRequested = true;
            }
        }
    }
}
