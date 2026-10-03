package com.geely.auto.music;

import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;

import com.mediabridge.app.BuildConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Opt-in black-box checks against the real player session used by the phone debug harness. */
final class DebugScenarioRunner {
    enum Status { PASS, FAIL, UNSUPPORTED, TIMEOUT, CANCELLED }
    enum Kind { PAUSE, PLAY, NEXT, PREVIOUS, SEEK, FAVORITE_CHANGE, FAVORITE_RESTORE,
        RESTORE_POSITION, RESTORE_PAUSE }

    interface Listener { void onReportChanged(String report, boolean running); }

    private static final long POLL_MS = 200L;
    private static final long CONTROL_TIMEOUT_MS = 5000L;
    private static final long FAVORITE_TIMEOUT_MS = 4000L;

    private final Handler handler;
    private final Listener listener;
    private final List<Result> results = new ArrayList<>();
    private final List<Kind> plan = new ArrayList<>();
    private boolean running;
    private int stepIndex;
    private long stepStartedAt;
    private PlayerSnapshot before;
    private long seekTarget;
    private Boolean favoriteRestore;
    private boolean initiallyPlaying;
    private PlayerSnapshot initialSnapshot;
    private long initialPosition;
    private long initialGeneration;
    private String initialPackage;
    private String initialSession;

    private final Runnable poll = new Runnable() {
        @Override public void run() { pollCurrentStep(); }
    };

    DebugScenarioRunner(Handler handler, Listener listener) {
        this.handler = handler;
        this.listener = listener;
    }

    boolean isRunning() { return running; }

    void start() {
        if (running) return;
        PlayerSnapshot snapshot = BridgeStateStore.getSnapshot();
        if (!PhoneDebugController.isAvailable() || snapshot == null) {
            results.clear();
            results.add(new Result(null, Status.FAIL, 0L, "调试后端或播放器尚未就绪"));
            notifyListener(false);
            return;
        }
        results.clear();
        plan.clear();
        plan.add(Kind.PAUSE);
        plan.add(Kind.PLAY);
        plan.add(Kind.NEXT);
        plan.add(Kind.PREVIOUS);
        plan.add(Kind.SEEK);
        plan.add(Kind.FAVORITE_CHANGE);
        plan.add(Kind.FAVORITE_RESTORE);
        plan.add(Kind.RESTORE_POSITION);
        initiallyPlaying = snapshot.isPlaying();
        if (!initiallyPlaying) plan.add(Kind.RESTORE_PAUSE);
        initialGeneration = BridgeStateStore.getGeneration();
        initialSnapshot = snapshot;
        initialPosition = snapshot.currentPosition();
        initialPackage = value(snapshot.packageName);
        initialSession = value(snapshot.sessionId);
        favoriteRestore = null;
        stepIndex = 0;
        running = true;
        DiagnosticsLog.i("PHONE_TEST start package=" + initialPackage + " session=" + initialSession);
        notifyListener(true);
        startCurrentStep();
    }

    void cancel() {
        if (!running) return;
        handler.removeCallbacks(poll);
        PlayerSnapshot current = BridgeStateStore.getSnapshot();
        if (favoriteRestore != null && current != null && !current.favoritePending
                && current.favoriteState != FavoriteState.UNKNOWN
                && (current.favoriteState == FavoriteState.FAVORITED) != favoriteRestore)
            PhoneDebugController.favorite(favoriteRestore);
        if (!initiallyPlaying) PhoneDebugController.pause();
        results.add(new Result(stepIndex < plan.size() ? plan.get(stepIndex) : null,
                Status.CANCELLED, 0L, "用户取消"));
        running = false;
        DiagnosticsLog.i("PHONE_TEST cancelled");
        notifyListener(false);
    }

    private void startCurrentStep() {
        handler.removeCallbacks(poll);
        if (!running) return;
        if (stepIndex >= plan.size()) {
            running = false;
            DiagnosticsLog.i("PHONE_TEST complete " + summary());
            notifyListener(false);
            return;
        }
        Kind kind = plan.get(stepIndex);
        before = BridgeStateStore.getSnapshot();
        if (!identityValid(before)) {
            abort(kind, Status.FAIL, "播放器、Session 或连接代次已变化");
            return;
        }
        stepStartedAt = SystemClock.elapsedRealtime();
        boolean accepted;
        switch (kind) {
            case PAUSE:
            case RESTORE_PAUSE:
                accepted = PhoneDebugController.pause();
                break;
            case PLAY:
                accepted = PhoneDebugController.play();
                break;
            case NEXT:
                accepted = PhoneDebugController.next();
                break;
            case PREVIOUS:
                accepted = PhoneDebugController.previous();
                break;
            case SEEK:
                if (before.durationMs < 10000L) {
                    finish(kind, Status.UNSUPPORTED, "当前曲目没有可验证的有效时长");
                    return;
                }
                seekTarget = chooseSeekTarget(before);
                accepted = PhoneDebugController.seekTo(seekTarget);
                break;
            case RESTORE_POSITION:
                if (!sameTrack(initialSnapshot, before) || before.durationMs <= 0L) {
                    finish(kind, Status.UNSUPPORTED, "上下曲后未确认回到原曲，未强行恢复进度");
                    return;
                }
                seekTarget = Math.min(Math.max(0L, initialPosition), before.durationMs);
                accepted = PhoneDebugController.seekTo(seekTarget);
                break;
            case FAVORITE_CHANGE:
                if (before.favoriteState != FavoriteState.FAVORITED
                        && before.favoriteState != FavoriteState.NOT_FAVORITED) {
                    finish(kind, Status.UNSUPPORTED, "播放器未提供可确认的收藏状态");
                    return;
                }
                favoriteRestore = before.favoriteState == FavoriteState.FAVORITED;
                accepted = PhoneDebugController.favorite(!favoriteRestore);
                break;
            case FAVORITE_RESTORE:
                if (favoriteRestore == null) {
                    finish(kind, Status.UNSUPPORTED, "前一步未改变收藏状态，无需恢复");
                    return;
                }
                accepted = PhoneDebugController.favorite(favoriteRestore);
                break;
            default:
                accepted = false;
        }
        DiagnosticsLog.i("PHONE_TEST dispatch step=" + kind + " accepted=" + accepted);
        if (!accepted) {
            finish(kind, Status.UNSUPPORTED, "控制请求未被调试后端接受");
            return;
        }
        handler.postDelayed(poll, POLL_MS);
    }

    private void pollCurrentStep() {
        if (!running || stepIndex >= plan.size()) return;
        Kind kind = plan.get(stepIndex);
        PlayerSnapshot current = BridgeStateStore.getSnapshot();
        if (!identityValid(current)) {
            abort(kind, Status.FAIL, "播放器、Session 或连接代次已变化");
            return;
        }
        if (satisfied(kind, before, current, seekTarget, favoriteRestore)) {
            finish(kind, Status.PASS, evidence(kind, current));
            return;
        }
        long timeout = kind == Kind.FAVORITE_CHANGE || kind == Kind.FAVORITE_RESTORE
                ? FAVORITE_TIMEOUT_MS : CONTROL_TIMEOUT_MS;
        if (SystemClock.elapsedRealtime() - stepStartedAt >= timeout) {
            String control = BridgeStateStore.getLastControl();
            Status status = control != null && control.contains("未声明支持")
                    ? Status.UNSUPPORTED : Status.TIMEOUT;
            finish(kind, status, "未收到预期状态；最后控制=" + value(control));
            return;
        }
        handler.postDelayed(poll, POLL_MS);
    }

    private void finish(Kind kind, Status status, String detail) {
        long elapsed = stepStartedAt == 0L ? 0L : Math.max(0L, SystemClock.elapsedRealtime() - stepStartedAt);
        results.add(new Result(kind, status, elapsed, detail));
        DiagnosticsLog.i("PHONE_TEST result step=" + kind + " status=" + status
                + " elapsedMs=" + elapsed + " detail=" + detail);
        stepIndex++;
        notifyListener(true);
        handler.postDelayed(this::startCurrentStep, 300L);
    }

    private void abort(Kind kind, Status status, String detail) {
        long elapsed = stepStartedAt == 0L ? 0L : Math.max(0L, SystemClock.elapsedRealtime() - stepStartedAt);
        results.add(new Result(kind, status, elapsed, detail));
        running = false;
        DiagnosticsLog.w("PHONE_TEST aborted step=" + kind + " status=" + status + " detail=" + detail);
        notifyListener(false);
    }

    private boolean identityValid(PlayerSnapshot snapshot) {
        return snapshot != null && initialGeneration == BridgeStateStore.getGeneration()
                && initialPackage.equals(value(snapshot.packageName))
                && initialSession.equals(value(snapshot.sessionId));
    }

    static boolean satisfied(Kind kind, PlayerSnapshot before, PlayerSnapshot current,
                             long seekTarget, Boolean favoriteRestore) {
        if (current == null) return false;
        switch (kind) {
            case PAUSE:
            case RESTORE_PAUSE:
                return current.status == PlaybackStatus.PAUSED;
            case PLAY:
                return current.isPlaying();
            case NEXT:
            case PREVIOUS:
                return before != null && trackChanged(before, current);
            case SEEK:
            case RESTORE_POSITION:
                if (before == null || !value(before.trackId).equals(value(current.trackId))) return false;
                long tolerance = Math.max(1500L, Math.max(0L, current.durationMs) / 50L);
                return Math.abs(current.currentPosition() - seekTarget) <= tolerance;
            case FAVORITE_CHANGE:
                if (favoriteRestore == null || current.favoritePending) return false;
                return (current.favoriteState == FavoriteState.FAVORITED) != favoriteRestore;
            case FAVORITE_RESTORE:
                if (favoriteRestore == null || current.favoritePending) return false;
                return (current.favoriteState == FavoriteState.FAVORITED) == favoriteRestore;
            default:
                return false;
        }
    }

    private static long chooseSeekTarget(PlayerSnapshot snapshot) {
        long first = Math.max(2000L, snapshot.durationMs / 4L);
        long second = Math.max(2000L, snapshot.durationMs * 3L / 4L);
        long selected = Math.abs(snapshot.currentPosition() - first) >= 5000L ? first : second;
        return Math.min(Math.max(0L, snapshot.durationMs - 2000L), selected);
    }

    private static boolean sameTrack(PlayerSnapshot first, PlayerSnapshot second) {
        return first != null && second != null && !trackChanged(first, second);
    }

    private static boolean trackChanged(PlayerSnapshot first, PlayerSnapshot second) {
        return first == null || second == null
                || !value(first.trackId).equals(value(second.trackId))
                || !value(first.title).equals(value(second.title))
                || !value(first.artist).equals(value(second.artist))
                || !value(first.album).equals(value(second.album));
    }

    private static String evidence(Kind kind, PlayerSnapshot current) {
        if (kind == Kind.NEXT || kind == Kind.PREVIOUS) return "track=" + value(current.trackId);
        if (kind == Kind.SEEK || kind == Kind.RESTORE_POSITION)
            return "position=" + current.currentPosition();
        if (kind == Kind.FAVORITE_CHANGE || kind == Kind.FAVORITE_RESTORE)
            return "favorite=" + current.favoriteState;
        return "state=" + current.status;
    }

    String report() {
        StringBuilder report = new StringBuilder();
        report.append("MediaBridge 手机自动测试\n")
                .append("版本：").append(BuildConfig.VERSION_NAME).append('\n')
                .append("设备：").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append(" / Android ").append(Build.VERSION.SDK_INT).append('\n')
                .append("播放器：").append(value(initialPackage)).append('\n')
                .append("Session：").append(value(initialSession)).append('\n')
                .append("Generation：").append(initialGeneration).append('\n')
                .append("说明：上下曲通过下一首/上一首尝试回原曲；确认回原曲后恢复原进度。\n");
        for (Result result : results) report.append(result.line()).append('\n');
        if (running && stepIndex < plan.size()) report.append("… 正在执行：").append(label(plan.get(stepIndex))).append('\n');
        report.append(summary());
        return report.toString();
    }

    private String summary() {
        int passed = 0, failed = 0, unsupported = 0;
        for (Result result : results) {
            if (result.status == Status.PASS) passed++;
            else if (result.status == Status.UNSUPPORTED) unsupported++;
            else failed++;
        }
        return String.format(Locale.US, "汇总：通过 %d，失败/超时 %d，不支持 %d", passed, failed, unsupported);
    }

    private void notifyListener(boolean stillRunning) {
        listener.onReportChanged(report(), stillRunning && running);
    }

    private static String label(Kind kind) {
        if (kind == null) return "初始化";
        switch (kind) {
            case PAUSE: return "暂停";
            case PLAY: return "播放";
            case NEXT: return "下一首";
            case PREVIOUS: return "上一首";
            case SEEK: return "跳转进度";
            case FAVORITE_CHANGE: return "改变收藏";
            case FAVORITE_RESTORE: return "恢复收藏";
            case RESTORE_POSITION: return "恢复原进度";
            case RESTORE_PAUSE: return "恢复暂停状态";
            default: return kind.name();
        }
    }

    private static String value(String value) { return value == null ? "" : value; }

    private static final class Result {
        final Kind kind;
        final Status status;
        final long elapsedMs;
        final String detail;

        Result(Kind kind, Status status, long elapsedMs, String detail) {
            this.kind = kind;
            this.status = status;
            this.elapsedMs = elapsedMs;
            this.detail = detail;
        }

        String line() {
            return "[" + status + "] " + label(kind) + " · " + elapsedMs + "ms · " + detail;
        }
    }
}
