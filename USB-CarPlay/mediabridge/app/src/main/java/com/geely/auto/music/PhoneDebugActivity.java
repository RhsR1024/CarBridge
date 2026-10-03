package com.geely.auto.music;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mediabridge.app.BuildConfig;

import java.util.Locale;

/** Phone-only rendering of the metadata and callbacks normally consumed by the vehicle UI. */
@SuppressLint("SetTextI18n")
public final class PhoneDebugActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refresh(); }
    };
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 250L);
        }
    };

    private ImageView artwork;
    private TextView title;
    private TextView artist;
    private TextView currentLyric;
    private TextView progressText;
    private TextView favoriteState;
    private TextView diagnostics;
    private TextView lyricContent;
    private TextView queueStatus;
    private LinearLayout queueItems;
    private TextView browserStatus;
    private LinearLayout browserItems;
    private TextView scenarioReport;
    private Button previous;
    private Button playPause;
    private Button next;
    private Button favorite;
    private Button unfavorite;
    private Button startScenario;
    private Button cancelScenario;
    private SeekBar progress;
    private boolean tracking;
    private String displayedArtwork = "";
    private String displayedQueue = "";
    private DebugScenarioRunner scenarioRunner;
    private DebugMediaBrowserClient browserClient;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.DEBUG) {
            finish();
            return;
        }
        setTitle("MediaBridge 手机调试台");
        setContentView(buildContent());
        scenarioRunner = new DebugScenarioRunner(handler, (report, running) -> {
            scenarioReport.setText(report);
            startScenario.setEnabled(!running && PhoneDebugController.isAvailable());
            cancelScenario.setEnabled(running);
        });
        browserClient = new DebugMediaBrowserClient(this, handler, this::showBrowserResult);
        SettingsRepository settings = new SettingsRepository(this);
        if (!settings.isPhoneDebugEnabled()) {
            Toast.makeText(this, "请先在 MediaBridge 首页开启手机调试模式", Toast.LENGTH_LONG).show();
        }
        MediaListenerService.requestRefresh(this);
        try {
            startForegroundService(BridgeEvents.serviceIntent(this, BridgeEvents.ACTION_START));
        } catch (RuntimeException error) {
            DiagnosticsLog.e("Unable to start phone debug bridge", error);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BridgeEvents.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(stateReceiver, filter);
        refresh();
        handler.post(ticker);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(ticker);
        try { unregisterReceiver(stateReceiver); } catch (IllegalArgumentException ignored) {}
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (scenarioRunner != null) scenarioRunner.cancel();
        if (browserClient != null) browserClient.close();
        super.onDestroy();
    }

    private View buildContent() {
        int background = Color.parseColor("#10131A");
        int card = Color.parseColor("#1C2230");
        int text = Color.parseColor("#F5F7FC");
        int muted = Color.parseColor("#AEB7C8");
        int accent = Color.parseColor("#4D8DFF");
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);
        LinearLayout root = column();
        root.setPadding(dp(20), dp(20), dp(20), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView heading = text("手机调试台", 28, text, true);
        root.addView(heading);
        TextView help = text("这里模拟原车媒体界面。按钮会经过与车机相同的 LegacyMusicClient 控制链。", 14, muted, false);
        root.addView(help, marginTop(6));

        LinearLayout nowPlaying = card(card);
        artwork = new ImageView(this);
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artwork.setImageResource(android.R.drawable.ic_media_play);
        nowPlaying.addView(artwork, new LinearLayout.LayoutParams(-1, dp(280)));
        title = text("等待播放器", 25, text, true);
        artist = text("请在目标播放器中开始播放", 17, muted, false);
        nowPlaying.addView(title, marginTop(18));
        nowPlaying.addView(artist, marginTop(5));
        currentLyric = text("暂无同步歌词", 21, Color.parseColor("#8CB5FF"), true);
        currentLyric.setGravity(Gravity.CENTER);
        currentLyric.setMinHeight(dp(70));
        nowPlaying.addView(currentLyric, marginTop(20));

        progress = new SeekBar(this);
        progress.setMax(1);
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (fromUser) progressText.setText(formatTime(value) + " / " + formatTime(seekBar.getMax()));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { tracking = true; }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                tracking = false;
                if (!PhoneDebugController.seekTo(seekBar.getProgress())) feedback("当前播放器未接受进度请求");
            }
        });
        nowPlaying.addView(progress, marginTop(8));
        progressText = text("00:00 / 00:00", 13, muted, false);
        progressText.setGravity(Gravity.END);
        nowPlaying.addView(progressText);

        LinearLayout playbackControls = row();
        previous = button("上一首", v -> runControl(PhoneDebugController.previous(), "上一首"));
        playPause = button("播放", v -> runControl(PhoneDebugController.togglePlayback(), "播放/暂停"));
        next = button("下一首", v -> runControl(PhoneDebugController.next(), "下一首"));
        playbackControls.addView(previous, weighted());
        playbackControls.addView(playPause, weightedWithLeftMargin());
        playbackControls.addView(next, weightedWithLeftMargin());
        nowPlaying.addView(playbackControls, marginTop(16));

        favoriteState = text("收藏：未知", 15, muted, false);
        nowPlaying.addView(favoriteState, marginTop(18));
        LinearLayout favoriteControls = row();
        favorite = button("收藏", v -> runControl(PhoneDebugController.favorite(true), "收藏"));
        unfavorite = button("取消收藏", v -> runControl(PhoneDebugController.favorite(false), "取消收藏"));
        favoriteControls.addView(favorite, weighted());
        favoriteControls.addView(unfavorite, weightedWithLeftMargin());
        nowPlaying.addView(favoriteControls, marginTop(8));
        root.addView(nowPlaying, marginTop(18));

        LinearLayout testCard = card(card);
        testCard.addView(text("自动验证", 19, text, true));
        testCard.addView(text("依次验证暂停、播放、上下曲、Seek 和收藏回传。测试会短暂改变当前播放状态、曲目和收藏，结束时尽力恢复收藏与原暂停状态。",
                13, muted, false), marginTop(8));
        scenarioReport = text("尚未运行自动测试", 13, muted, false);
        scenarioReport.setTextIsSelectable(true);
        testCard.addView(scenarioReport, marginTop(10));
        startScenario = button("开始核心自动测试", v -> confirmScenario());
        cancelScenario = button("取消", v -> { if (scenarioRunner != null) scenarioRunner.cancel(); });
        cancelScenario.setEnabled(false);
        LinearLayout scenarioActions = row();
        scenarioActions.addView(startScenario, weighted());
        scenarioActions.addView(cancelScenario, weightedWithLeftMargin());
        testCard.addView(scenarioActions, marginTop(10));
        LinearLayout reportActions = row();
        reportActions.addView(button("复制报告", v -> copyScenarioReport()), weighted());
        reportActions.addView(button("导出诊断", v -> exportScenarioDiagnostics()), weightedWithLeftMargin());
        testCard.addView(reportActions, marginTop(8));
        root.addView(testCard, marginTop(14));

        LinearLayout details = card(card);
        details.addView(text("诊断信息", 19, text, true));
        diagnostics = text("等待 MediaSession", 13, muted, false);
        diagnostics.setTextIsSelectable(true);
        details.addView(diagnostics, marginTop(10));
        root.addView(details, marginTop(14));

        LinearLayout lyricsCard = card(card);
        lyricsCard.addView(text("完整歌词（调试）", 19, text, true));
        lyricContent = text("暂无歌词内容", 14, muted, false);
        lyricContent.setTextIsSelectable(true);
        lyricsCard.addView(lyricContent, marginTop(10));
        root.addView(lyricsCard, marginTop(14));

        LinearLayout listCard = card(card);
        listCard.addView(text("播放列表与推荐", 19, text, true));
        queueStatus = text("等待播放器提供标准 MediaSession Queue", 14, muted, false);
        listCard.addView(queueStatus, marginTop(8));
        queueItems = column();
        listCard.addView(queueItems, marginTop(8));
        listCard.addView(text("这里只展示播放器真实提供的 Queue；推荐内容和 MediaBrowser 浏览结果不会混入当前播放队列。",
                12, muted, false), marginTop(10));
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#394154"));
        LinearLayout.LayoutParams dividerParams = marginTop(14);
        dividerParams.height = dp(1);
        listCard.addView(divider, dividerParams);
        listCard.addView(text("公开 MediaBrowser 根目录", 17, text, true), marginTop(14));
        browserStatus = text("尚未探测；只会连接当前播放器公开且已授权的标准浏览服务", 13, muted, false);
        listCard.addView(browserStatus, marginTop(6));
        listCard.addView(button("探测当前播放器浏览目录", v -> probeBrowser()), marginTop(8));
        browserItems = column();
        listCard.addView(browserItems, marginTop(8));
        root.addView(listCard, marginTop(14));
        return scroll;
    }

    private void refresh() {
        if (title == null) return;
        PlayerSnapshot snapshot = BridgeStateStore.getSnapshot();
        boolean available = PhoneDebugController.isAvailable() && snapshot != null;
        previous.setEnabled(available);
        playPause.setEnabled(available);
        next.setEnabled(available);
        favorite.setEnabled(available);
        unfavorite.setEnabled(available);
        progress.setEnabled(available);
        if (startScenario != null && (scenarioRunner == null || !scenarioRunner.isRunning()))
            startScenario.setEnabled(available);
        if (snapshot == null) {
            title.setText("等待播放器");
            artist.setText("请确认通知访问权限，并在目标播放器中开始播放");
            currentLyric.setText("暂无同步歌词");
            favoriteState.setText("收藏：未知");
            diagnostics.setText("输入：" + BridgeStateStore.getInputState()
                    + "\n调试后端：" + BridgeStateStore.getOutputState());
            playPause.setText("播放");
            if (!tracking) { progress.setMax(1); progress.setProgress(0); progressText.setText("00:00 / 00:00"); }
            setArtwork("");
            refreshQueue(null, null);
            return;
        }

        title.setText(emptyAs(snapshot.title, "未命名曲目"));
        artist.setText(emptyAs(snapshot.artist, "未知歌手") + "  ·  " + emptyAs(snapshot.album, "未知专辑"));
        String line = PhoneDebugController.currentLyric();
        currentLyric.setText(line.isEmpty() ? "暂无同步歌词" : line);
        playPause.setText(snapshot.isPlaying() ? "暂停" : "播放");
        favoriteState.setText("收藏：" + favoriteLabel(snapshot.favoriteState)
                + (snapshot.favoritePending ? "（等待播放器确认）" : "")
                + "\n" + emptyAs(snapshot.favoriteMessage, "播放器未提供补充信息"));

        if (!tracking) {
            int duration = safeInt(Math.max(1L, snapshot.durationMs));
            int position = Math.min(duration, safeInt(snapshot.currentPosition()));
            progress.setMax(duration);
            progress.setProgress(position);
            progressText.setText(formatTime(position) + " / " + formatTime(duration));
        }
        setArtwork(snapshot.artworkUri);
        diagnostics.setText("播放器：" + snapshot.appLabel
                + "\n包名：" + snapshot.packageName
                + "\nSession：" + emptyAs(snapshot.sessionId, "—")
                + "\nTrack：" + emptyAs(snapshot.trackId, "—")
                + "\n状态：" + snapshot.status + "  速度：" + snapshot.playbackSpeed
                + "\n收藏可写：" + snapshot.favoriteWriteSupported
                + "\n歌词来源：" + BridgeStateStore.getLyricsState()
                + "\n封面来源：" + BridgeStateStore.getArtworkState()
                + "\n最后控制：" + BridgeStateStore.getLastControl()
                + "\n调试后端：" + BridgeStateStore.getOutputState()
                + "  generation=" + BridgeStateStore.getGeneration());
        String content = PhoneDebugController.lyricContent();
        if (content.length() > 8000) content = content.substring(0, 8000) + "\n…（已截断）";
        lyricContent.setText(content.isEmpty() ? "暂无歌词内容" : content);
        refreshQueue(snapshot, BridgeStateStore.getQueueSnapshot());
    }

    private void refreshQueue(PlayerSnapshot player, QueueSnapshot queue) {
        if (queueStatus == null || queueItems == null) return;
        String signature = queueSignature(player, queue);
        if (signature.equals(displayedQueue)) return;
        displayedQueue = signature;
        queueItems.removeAllViews();
        if (player == null || queue == null || !queue.matches(player)) {
            queueStatus.setText("等待播放器提供标准 MediaSession Queue");
            return;
        }
        if (!queue.provided) {
            queueStatus.setText("当前播放器未提供标准 Queue；不会合成或猜测播放列表");
            return;
        }
        String heading = queue.title.isEmpty() ? "标准播放队列" : queue.title;
        queueStatus.setText(heading + " · " + queue.items.size() + " 项");
        if (queue.items.isEmpty()) {
            queueItems.addView(text("播放器返回了空队列", 13, Color.parseColor("#AEB7C8"), false));
            return;
        }
        int shown = Math.min(100, queue.items.size());
        for (int index = 0; index < shown; index++) {
            QueueSnapshot.Entry item = queue.items.get(index);
            boolean active = item.queueId == queue.activeQueueId;
            String itemTitle = emptyAs(item.title, emptyAs(item.mediaId, "队列项 " + (index + 1)));
            String subtitle = emptyAs(item.subtitle, item.description);
            Button entry = button((active ? "▶ " : "") + (index + 1) + ". " + itemTitle
                    + (subtitle.isEmpty() ? "" : "\n" + subtitle),
                    v -> runControl(PhoneDebugController.playQueueItem(item.queueId), "播放队列项"));
            entry.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            entry.setEnabled(item.queueId != android.media.session.MediaSession.QueueItem.UNKNOWN_ID);
            queueItems.addView(entry, marginTop(index == 0 ? 0 : 6));
        }
        if (queue.items.size() > shown)
            queueItems.addView(text("其余 " + (queue.items.size() - shown) + " 项未展开", 12,
                    Color.parseColor("#AEB7C8"), false), marginTop(8));
    }

    private static String queueSignature(PlayerSnapshot player, QueueSnapshot queue) {
        if (player == null || queue == null) return "none";
        StringBuilder value = new StringBuilder(queue.packageName).append('|').append(queue.sessionId)
                .append('|').append(queue.generation).append('|').append(queue.provided)
                .append('|').append(queue.activeQueueId).append('|').append(queue.title);
        for (QueueSnapshot.Entry item : queue.items)
            value.append('\n').append(item.queueId).append('|').append(item.mediaId)
                    .append('|').append(item.title).append('|').append(item.subtitle);
        return value.toString();
    }

    private void probeBrowser() {
        PlayerSnapshot snapshot = BridgeStateStore.getSnapshot();
        if (snapshot == null) { feedback("当前没有播放器"); return; }
        browserStatus.setText("正在查找并连接 " + snapshot.packageName + " 的公开浏览服务…");
        browserItems.removeAllViews();
        PhoneDebugController.approveBrowserSnapshot(null);
        browserClient.probe(snapshot.packageName);
    }

    private void showBrowserResult(BrowserSnapshot browser) {
        PlayerSnapshot player = BridgeStateStore.getSnapshot();
        if (player == null || !browser.packageName.equals(player.packageName)) {
            PhoneDebugController.approveBrowserSnapshot(null);
            browserStatus.setText("浏览结果已过期：播放器已经切换");
            browserItems.removeAllViews();
            return;
        }
        browserItems.removeAllViews();
        if (!browser.error.isEmpty()) {
            PhoneDebugController.approveBrowserSnapshot(null);
            browserStatus.setText(browser.error);
            return;
        }
        PhoneDebugController.approveBrowserSnapshot(browser);
        browserStatus.setText("服务：" + browser.component + "\n根目录：" + browser.rootId
                + "\n条目：" + browser.items.size());
        int shown = Math.min(100, browser.items.size());
        for (int index = 0; index < shown; index++) {
            BrowserSnapshot.Entry item = browser.items.get(index);
            String flags = item.playable ? "可播放" : item.browsable ? "目录" : "只读";
            String label = (index + 1) + ". " + emptyAs(item.title, emptyAs(item.mediaId, "未命名条目"))
                    + (item.subtitle.isEmpty() ? "" : "\n" + item.subtitle) + "  [" + flags + "]";
            Button entry = button(label, v -> runControl(
                    item.playable && PhoneDebugController.playMediaId(item.mediaId), "按 Media ID 播放"));
            entry.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            entry.setEnabled(item.playable && !item.mediaId.isEmpty());
            browserItems.addView(entry, marginTop(index == 0 ? 0 : 6));
        }
        if (browser.items.isEmpty())
            browserItems.addView(text("播放器返回了空的浏览根目录", 13,
                    Color.parseColor("#AEB7C8"), false));
        else if (browser.items.size() > shown)
            browserItems.addView(text("其余 " + (browser.items.size() - shown) + " 项未展开", 12,
                    Color.parseColor("#AEB7C8"), false), marginTop(8));
    }

    private void setArtwork(String uri) {
        String value = uri == null ? "" : uri;
        if (value.equals(displayedArtwork)) return;
        displayedArtwork = value;
        if (value.isEmpty()) {
            artwork.setImageResource(android.R.drawable.ic_media_play);
            return;
        }
        try {
            artwork.setImageURI(null);
            artwork.setImageURI(Uri.parse(value));
            if (artwork.getDrawable() == null) artwork.setImageResource(android.R.drawable.ic_media_play);
        } catch (RuntimeException error) {
            artwork.setImageResource(android.R.drawable.ic_media_play);
            DiagnosticsLog.e("Unable to render debug artwork", error);
        }
    }

    private void runControl(boolean accepted, String label) {
        if (!accepted) feedback(label + "未发送：调试后端或播放器尚未就绪");
    }

    private void confirmScenario() {
        if (!PhoneDebugController.isAvailable()) {
            feedback("播放器尚未就绪");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("开始自动验证？")
                .setMessage("测试会执行暂停、播放、上下曲、进度跳转，并在收藏状态可确认时测试收藏后恢复。请先选择一首允许操作的歌曲。")
                .setPositiveButton("开始", (dialog, which) -> scenarioRunner.start())
                .setNegativeButton("取消", null)
                .show();
    }

    private void copyScenarioReport() {
        String report = scenarioRunner == null ? scenarioReport.getText().toString() : scenarioRunner.report();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) { feedback("无法访问剪贴板"); return; }
        clipboard.setPrimaryClip(ClipData.newPlainText("MediaBridge 手机测试报告", report));
        feedback("测试报告已复制");
    }

    private void exportScenarioDiagnostics() {
        String report = scenarioRunner == null ? scenarioReport.getText().toString() : scenarioRunner.report();
        DiagnosticsLog.i("PHONE_TEST REPORT\n" + report);
        startScenario.setEnabled(false);
        new Thread(() -> {
            String path = DiagnosticsLog.exportToDownloads(getApplicationContext(), true);
            handler.post(() -> {
                if (scenarioRunner == null || !scenarioRunner.isRunning())
                    startScenario.setEnabled(PhoneDebugController.isAvailable());
                feedback(path == null ? "诊断导出失败" : "诊断已保存到 " + path);
            });
        }, "PhoneDebug-Export").start();
    }

    private void feedback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private LinearLayout card(int color) {
        LinearLayout layout = column();
        layout.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(16));
        layout.setBackground(background);
        return layout;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(dp(2), 1f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout.LayoutParams marginTop(int value) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(value);
        return params;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, -2, 1f);
    }

    private LinearLayout.LayoutParams weightedWithLeftMargin() {
        LinearLayout.LayoutParams params = weighted();
        params.leftMargin = dp(8);
        return params;
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private static String emptyAs(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static String favoriteLabel(FavoriteState state) {
        if (state == FavoriteState.FAVORITED) return "已收藏";
        if (state == FavoriteState.NOT_FAVORITED) return "未收藏";
        return "未知";
    }

    private static int safeInt(long value) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value));
    }

    private static String formatTime(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format(Locale.US, "%02d:%02d", seconds / 60L, seconds % 60L);
    }
}
