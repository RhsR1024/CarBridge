package com.geely.auto.music;

import android.os.Handler;
import android.os.SystemClock;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import static com.geely.auto.music.SourceDirectoryRegistrar.*;

/** Maintains only the directory; deliberately has no playback/focus API. One instance per connection. */
final class SourceDirectoryMonitor {
    private static final long TIMEOUT_MS = 10000L;
    private static final long[] VERIFY_DELAYS = {15000L, 45000L, 240000L, 300000L};
    private static final long[] RETRY_DELAYS = {15000L, 30000L, 60000L, 120000L, 300000L};
    interface Operation { Result check(BooleanSupplier cancelled) throws Exception; }
    interface Listener { void onResult(Result result, String diagnostic); }

    private final Handler main;
    private final Executor worker;
    private final Operation operation;
    private final Listener listener;
    private final Runnable check = this::checkNow;
    private final Runnable deadline = this::onTimeout;
    private volatile boolean stopped;
    private volatile Job inFlight;
    private boolean started, confirmedOnce;
    private int verification, failures, attempts;
    private String reason = "registration";

    SourceDirectoryMonitor(Handler main, Executor worker, Operation operation, Listener listener) {
        this.main = main; this.worker = worker; this.operation = operation; this.listener = listener;
    }

    // start and all scheduling/result handling run on the main thread. stop may invalidate from Binder/IO.
    void start() {
        if (started || stopped) return;
        started = true;
        checkNow();
    }

    void stop() {
        stopped = true;
        main.removeCallbacks(check);
        main.removeCallbacks(deadline);
        Job job = inFlight;
        if (job != null) cancel(job);
    }

    private void checkNow() {
        if (stopped) return;
        if (inFlight != null) {
            // A synchronous Binder call cannot be forcibly ended. Never overlap it with another writer.
            report(new Result(Status.WAITING, "来源目录检查仍在等待车机接口返回"), inFlight, retryDelay());
            return;
        }
        Job job = new Job(++attempts, reason);
        inFlight = job;
        main.postDelayed(deadline, TIMEOUT_MS);
        try {
            worker.execute(job);
        } catch (RejectedExecutionException busy) {
            main.removeCallbacks(deadline);
            cancel(job);
            inFlight = null;
            report(new Result(Status.BUSY, "来源目录登记繁忙，等待重试"), job, retryDelay());
        }
    }

    private void onTimeout() {
        Job job = inFlight;
        if (stopped || job == null) return;
        cancel(job);
        // Cancel queued work immediately; running work retains the slot until it really returns.
        if (job.phase.get() == 2) inFlight = null;
        report(new Result(Status.TIMEOUT, "来源目录检查超时；等待重试，客户端保持连接"), job, retryDelay());
    }

    private void cancel(Job job) {
        job.cancelled = true;
        if (job.phase.compareAndSet(0, 2) && worker instanceof ThreadPoolExecutor)
            ((ThreadPoolExecutor) worker).remove(job);
    }

    private void completed(Job job, Result result) {
        if (stopped || inFlight != job) return;
        inFlight = null;
        main.removeCallbacks(deadline);
        // Timeout already arranged a retry. A late success must not replace its diagnostic state.
        if (job.cancelled) return;
        long delay;
        if (result.confirmed()) {
            failures = 0;
            if (result.changed()) verification = 0;
            delay = VERIFY_DELAYS[verification];
            verification = Math.min(verification + 1, VERIFY_DELAYS.length - 1);
        } else {
            verification = 0;
            delay = retryDelay();
        }
        report(result, job, delay);
    }

    private long retryDelay() {
        verification = 0;
        long delay = RETRY_DELAYS[failures];
        failures = Math.min(failures + 1, RETRY_DELAYS.length - 1);
        return delay;
    }

    private void report(Result result, Job job, long delay) {
        if (stopped) return;
        boolean missing = confirmedOnce && result.ownBefore == 0;
        listener.onResult(result, "event=check reason=" + job.reason + " attempt=" + job.attempt
                + " result=" + result.status + " previouslyConfirmed=" + confirmedOnce
                + " missingAfterConfirmation=" + missing
                + " elapsedMs=" + (SystemClock.elapsedRealtime() - job.startedAt)
                + " nextCheckMs=" + delay + " " + result.detail);
        if (result.confirmed()) confirmedOnce = true;
        reason = result.confirmed() ? (delay < 300000L ? "delayed_verify" : "periodic_verify") : "retry";
        main.removeCallbacks(check);
        if (!stopped) main.postDelayed(check, delay);
    }

    private final class Job implements Runnable {
        final int attempt;
        final String reason;
        final long startedAt = SystemClock.elapsedRealtime();
        // 0 queued, 1 running, 2 actually returned or cancelled before starting (not Future.isDone).
        final AtomicInteger phase = new AtomicInteger();
        volatile boolean cancelled;
        Job(int attempt, String reason) { this.attempt = attempt; this.reason = reason; }
        @Override public void run() {
            if (!phase.compareAndSet(0, 1)) return;
            Result result;
            try {
                result = stopped || cancelled ? new Result(Status.CANCELLED, "来源目录登记已取消")
                        : operation.check(() -> stopped || cancelled);
            } catch (Exception | LinkageError error) {
                result = new Result(Status.FAILED, "来源目录登记失败：" + error.getClass().getSimpleName());
            } finally {
                phase.set(2);
            }
            Result completedResult = result;
            main.post(() -> completed(this, completedResult));
        }
    }
}
