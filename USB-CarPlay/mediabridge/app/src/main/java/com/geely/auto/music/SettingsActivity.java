package com.geely.auto.music;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.Manifest;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Compact settings/status UI. The screen never tries to grant notification access silently. */
@SuppressLint("SetTextI18n")
public class SettingsActivity extends Activity {
    private static final int REQUEST_WRITE_DOWNLOADS = 92;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshStatus();
        }
    };
    private SettingsRepository settings;
    private TextView playerView, accessBanner;
    private NotificationAccessStatus lastAccess = NotificationAccessStatus.UNKNOWN;
    private static final java.util.concurrent.ThreadPoolExecutor UI_WORK = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 30, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.ArrayBlockingQueue<>(8),
            task -> { Thread thread = new Thread(task, "MediaBridge-UI-IO"); thread.setDaemon(true); return thread; });
    private TextView playerListsView;
    private TextView[] statusValues;
    private TextView feedbackView;
    private TextView tuneView;
    private TextView collectionHint;
    private LinearLayout favoriteIgnoredListView;
    private String lastFavoriteIgnoredSignature = "";
    private LinearLayout ignoredListView;
    private String lastIgnoredSignature = "";
    private Switch bridgeSwitch;
    private Switch collectionSwitch;
    private Switch lyricsSwitch;
    private Button tuneMinus;
    private Button tunePlus;
    private Button pinPlayer;
    private Button ignorePlayer;
    private Button openPlayer;
    private Button retryLyrics;
    private int pageColor;
    private int cardColor;
    private int heroColor;
    private int textColor;
    private int mutedColor;
    private int blueColor;
    private int buttonColor;
    private int lineColor;
    private boolean nightMode;
    private int bodySp;
    private boolean wideLayout;
    private boolean notificationAccessPromptShown;
    private final Runnable statusPoll = new Runnable() {
        @Override
        public void run() {
            refreshStatus();
            handler.postDelayed(this, 2000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EntryDiagnostics.record(this, "create", getIntent());
        settings = new SettingsRepository(this);
        setContentView(buildContent());
        boolean runtimePermissionRequested = requestNotificationPermissionIfNeeded();
        ensureServiceRunning();
        if (!runtimePermissionRequested) handler.post(this::maybePromptNotificationAccess);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EntryDiagnostics.record(this, "resume", getIntent());
        registerStateReceiver();
        refreshStatus();
        handler.post(statusPoll);
    }

    @Override
    protected void onPause() {
        EntryDiagnostics.record(this, "pause", getIntent());
        handler.removeCallbacks(statusPoll);
        try {
            unregisterReceiver(stateReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        EntryDiagnostics.record(this, "new_intent", intent);
    }

    private View buildContent() {
        prepareColors();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{nightMode ? Color.parseColor("#3A3F5C") : Color.parseColor("#ECEEF3"),
                        nightMode ? Color.parseColor("#2B2F47") : Color.parseColor("#E0E3EC")}));
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame);
        LinearLayout root = column();
        root.setPadding(dp(50), dp(24), dp(50), dp(44));
        frame.addView(root, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        TextView title = label("MediaBridge", bodySp + 13, textColor, true);
        LinearLayout.LayoutParams titleParams = fullWidth();
        titleParams.bottomMargin = dp(19);
        root.addView(title, titleParams);

        LinearLayout hero = card(heroColor);
        GradientDrawable heroBackground = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{heroColor, cardColor});
        heroBackground.setCornerRadius(dp(13));
        hero.setBackground(heroBackground);
        LinearLayout heroRow = horizontal();
        TextView dot = label("●", bodySp + 1, settings.isBridgeEnabled() ? Color.rgb(74, 222, 128) : mutedColor, true);
        heroRow.addView(dot);
        LinearLayout heroLabels = column();
        heroLabels.setPadding(dp(16), 0, dp(12), 0);
        heroLabels.addView(label("启用媒体桥接", bodySp + 5, textColor, true));
        heroLabels.addView(label("默认开启；关闭后记住选择，重启车辆不会自动改变", bodySp - 3, mutedColor, false));
        heroRow.addView(heroLabels, new LinearLayout.LayoutParams(0, -2, 1));
        bridgeSwitch = blueSwitch(settings.isBridgeEnabled(), "启用媒体桥接");
        bridgeSwitch.setOnCheckedChangeListener((button, enabled) -> {
            settings.setBridgeEnabled(enabled);
            dot.setTextColor(enabled ? Color.rgb(74, 222, 128) : mutedColor);
            if (enabled) ensureServiceRunning();
            else stopService(new Intent(this, UniversalBridgeService.class));
            refreshStatus();
        });
        heroRow.addView(bridgeSwitch);
        hero.addView(heroRow);
        addCard(root, hero);

        LinearLayout accessCard = card(cardColor);
        accessBanner = label("正在检查通知访问", bodySp - 2, textColor, true);
        accessCard.addView(accessBanner);
        Button permissionButton = action("通知访问设置", false, v -> openNotificationSettings());
        Button recheckPermission = action("重新检查", true, v -> {
            refreshStatus();
            MediaListenerService.requestRefresh(this);
        });
        accessCard.addView(actionRow(permissionButton, recheckPermission), spacedFullWidth(dp(12)));
        addCard(root, accessCard);

        if (com.mediabridge.app.BuildConfig.DEBUG) {
            addSection(root, "手机调试");
            LinearLayout debugCard = card(Color.parseColor(nightMode ? "#354C72" : "#E8F1FF"));
            Switch phoneDebug = blueSwitch(settings.isPhoneDebugEnabled(), "手机调试模式");
            debugCard.addView(settingRow("手机调试模式",
                    "使用手机界面替代 ECARX 车机媒体中心；正式版没有此入口。切换后会重建输出通道。",
                    phoneDebug));
            Button openDebug = action("打开调试播放界面", false, v -> {
                if (!settings.isPhoneDebugEnabled()) {
                    showFeedback("请先开启手机调试模式");
                    return;
                }
                startActivity(new Intent(this, PhoneDebugActivity.class));
            });
            debugCard.addView(openDebug, spacedFullWidth(dp(12)));
            phoneDebug.setOnCheckedChangeListener((button, enabled) -> {
                settings.setPhoneDebugEnabled(enabled);
                if (settings.isBridgeEnabled()) applySettingsImmediately();
                showFeedback(enabled ? "手机调试模式已开启" : "手机调试模式已关闭，将使用车机通道");
            });
            addCard(root, debugCard);
        }

        addSection(root, "连接设置");
        LinearLayout reconnectCard = card(cardColor);
        reconnectCard.addView(label("重新连接媒体通道", bodySp + 1, textColor, true));
        reconnectCard.addView(label("车机桥接、手机调试异常或断链后手动触发重连", bodySp - 3, mutedColor, false));
        Button reconnect = action("重连", false, v -> { ensureServiceRunning(true); refreshStatus(); });
        reconnectCard.addView(reconnect, spacedFullWidth(dp(12)));
        addCard(root, reconnectCard);

        LinearLayout resumeCard = card(cardColor);
        Switch resumeLast = blueSwitch(settings.isResumeLastPlayerEnabled(), "切回时续播");
        resumeCard.addView(settingRow("切回时续播", "在车机选择 MediaBridge 时尝试继续上次播放器。开机和重连不会自动播放；播放器已退出时只打开应用。", resumeLast));
        resumeLast.setOnCheckedChangeListener((button, enabled) -> settings.setResumeLastPlayerEnabled(enabled));
        resumeCard.addView(label("车机“进入应用”打开当前或上次播放器；从应用列表打开 MediaBridge 仍进入设置。", bodySp - 3, mutedColor, false));
        addCard(root, resumeCard);

        addSection(root, "封面显示");
        LinearLayout artworkCard = card(cardColor);
        Switch roundArtwork = blueSwitch(settings.isRoundArtworkEnabled(), "智能封面适配");
        artworkCard.addView(settingRow("智能封面适配",
                "默认开启；只处理已识别的异常封面来源，其他播放器保持原图。酷狗无完整封面时会先在线补全，再缩入圆形安全区；关闭后全部使用播放器原图。",
                roundArtwork));
        roundArtwork.setOnCheckedChangeListener((button, enabled) -> {
            settings.setRoundArtworkEnabled(enabled);
            ArtworkRepository.invalidateAll();
            MediaListenerService.requestRefresh(this);
            applySettingsImmediately();
        });
        addCard(root, artworkCard);

        addSection(root, "显示收藏");
        LinearLayout favoriteCard = card(cardColor);
        collectionSwitch = blueSwitch(settings.isShowCollectionEnabled(), "显示收藏");
        favoriteCard.addView(settingRow("显示收藏", "默认向所有播放器显示原车收藏红心；勾选下方应用可单独隐藏。点击后以播放器回传为准。", collectionSwitch));
        collectionHint = label("正在确认播放器收藏能力", bodySp - 3, mutedColor, false);
        collectionHint.setPadding(0, dp(10), 0, 0);
        favoriteCard.addView(collectionHint);
        collectionSwitch.setOnCheckedChangeListener((button, enabled) -> {
            settings.setShowCollectionEnabled(enabled);
            refreshCollectionHint();
            applySettingsImmediately();
        });
        addCard(root, favoriteCard);
        LinearLayout favoriteIgnoreHeader = card(cardColor);
        favoriteIgnoreHeader.addView(label("收藏忽略名单", bodySp + 1, textColor, true));
        favoriteIgnoreHeader.addView(label("勾选后只隐藏该应用的小窗红心，不影响桥接、方控、封面或歌词；默认均不勾选。",
                bodySp - 3, mutedColor, false));
        Button addFavoriteIgnored = action("＋ 手动添加", false, v -> showAddFavoriteIgnoredDialog());
        Button clearFavoriteIgnored = action("清空收藏忽略", true, v -> {
            settings.clearFavoriteIgnoredPackages();
            applySettingsImmediately();
            refreshStatus();
        });
        favoriteIgnoreHeader.addView(actionRow(addFavoriteIgnored, clearFavoriteIgnored), spacedFullWidth(dp(12)));
        addCard(root, favoriteIgnoreHeader);
        favoriteIgnoredListView = card(cardColor);
        addCard(root, favoriteIgnoredListView);

        addSection(root, "显示歌词");
        LinearLayout lyricsCard = card(cardColor);
        lyricsSwitch = blueSwitch(settings.isLyricsEnabled(), "显示歌词");
        lyricsCard.addView(settingRow("显示歌词", "获取歌词并同步到原车媒体界面", lyricsSwitch));
        lyricsSwitch.setOnCheckedChangeListener((button, enabled) -> {
            settings.setLyricsEnabled(enabled);
            applySettingsImmediately();
            refreshControlAvailability(notificationAccessStatus() == NotificationAccessStatus.GRANTED,
                    BridgeStateStore.getSnapshot());
        });
        View rule = new View(this);
        rule.setBackgroundColor(lineColor);
        LinearLayout.LayoutParams ruleParams = fullWidth();
        ruleParams.topMargin = dp(14);
        ruleParams.bottomMargin = dp(13);
        ruleParams.height = dp(1);
        lyricsCard.addView(rule, ruleParams);
        tuneView = label("偏移 0ms", bodySp + 1, blueColor, true);
        lyricsCard.addView(tuneView);
        tuneMinus = action("－100ms · 歌词延后", false, v -> changeTune(-100));
        tunePlus = action("＋100ms · 歌词提前", false, v -> changeTune(100));
        Button reset = action("归零", true, v -> changeTune(-settings.getTuneMs()));
        retryLyrics = action("重新获取当前歌词", true, v -> {
            try {
                startService(BridgeEvents.serviceIntent(this, BridgeEvents.ACTION_RETRY_LYRICS));
            } catch (RuntimeException error) {
                DiagnosticsLog.e("Unable to retry current lyrics", error);
            }
        });
        LinearLayout tuneActions = actionRow(retryLyrics, tuneMinus, tunePlus, reset);
        lyricsCard.addView(tuneActions, spacedFullWidth(dp(10)));
        TextView tuneHelp = label("歌词慢了点＋（提前）· 歌词快了点－（延后）· 当前歌曲立即生效，范围 -3000～+3000ms",
                bodySp - 3, mutedColor, false);
        tuneHelp.setPadding(0, dp(12), 0, 0);
        lyricsCard.addView(tuneHelp);
        Switch online = blueSwitch(settings.isOnlineLookupEnabled(), "在线歌词与封面检索");
        lyricsCard.addView(settingRow("在线歌词与封面", "默认沿用原版开启，会向所选歌词来源及封面来源发送歌名、歌手；可随时关闭，仅使用播放器内容和缓存。", online), spacedFullWidth(dp(12)));
        online.setOnCheckedChangeListener((button, enabled) -> {
            if (!enabled) { settings.setOnlineLookupEnabled(false); applySettingsImmediately(); MediaListenerService.requestRefresh(this); return; }
            new AlertDialog.Builder(this).setTitle("启用在线检索")
                    .setMessage("会向所选歌词服务发送歌名和歌手；封面回退会查询网易云和 QQ 音乐。不会上传通知内容或账号凭据。可随时关闭。")
                    .setPositiveButton("启用", (dialog, which) -> { settings.setOnlineLookupEnabled(true); applySettingsImmediately(); MediaListenerService.requestRefresh(this); })
                    .setNegativeButton("取消", (dialog, which) -> online.setChecked(false))
                    .setOnCancelListener(dialog -> online.setChecked(false)).show();
        });
        lyricsCard.addView(actionGroup(action("选择歌词来源", true, v -> chooseLyricsSources()),
                action("基础补偿", true, v -> editBaseOffset()),
                action("清除歌词与封面缓存", true, v -> clearMediaCaches())), spacedFullWidth(dp(12)));
        addCard(root, lyricsCard);

        addSection(root, "应用忽略名单");
        LinearLayout ignoreHeader = card(cardColor);
        Button addIgnored = action("＋ 手动添加", false, v -> showAddIgnoredDialog());
        Button clearIgnored = action("清空忽略名单", true, v -> {
            settings.clearBlacklist();
            MediaListenerService.requestRefresh(this);
            refreshStatus();
        });
        ignoreHeader.addView(label("勾选的应用自行接入车机，播放时优先让位", bodySp + 1, textColor, true));
        ignoreHeader.addView(label("未勾选的应用开始播放时恢复桥接；此名单与收藏忽略名单独立保存", bodySp - 3, mutedColor, false));
        ignoreHeader.addView(actionRow(addIgnored, clearIgnored), spacedFullWidth(dp(12)));
        addCard(root, ignoreHeader);
        ignoredListView = card(cardColor);
        addCard(root, ignoredListView);

        addSection(root, "运行状态");
        LinearLayout statusCard = card(cardColor);
        LinearLayout playerRow = horizontal();
        TextView cover = label("♪", bodySp + 13, Color.WHITE, true);
        cover.setGravity(Gravity.CENTER);
        cover.setBackground(roundRect(Color.rgb(69, 84, 139), dp(11), 0, 0));
        playerRow.addView(cover, new LinearLayout.LayoutParams(dp(68), dp(68)));
        playerView = label("当前播放器：—", bodySp, textColor, true);
        LinearLayout.LayoutParams playerParams = new LinearLayout.LayoutParams(0, -2, 1);
        playerParams.leftMargin = dp(16);
        playerRow.addView(playerView, playerParams);
        statusCard.addView(playerRow);
        LinearLayout statusGrid = column();
        statusValues = new TextView[5];
        String[] statusLabels = {"通知访问", "可用性", "输入", "输出（车机）", "连接代次"};
        int columns = wideLayout ? 2 : 1;
        for (int i = 0; i < statusLabels.length; i += columns) {
            LinearLayout statusRow = horizontal();
            for (int j = i; j < Math.min(i + columns, statusLabels.length); j++) {
                LinearLayout cell = column();
                cell.setPadding(0, dp(12), dp(12), dp(10));
                cell.addView(label(statusLabels[j], bodySp - 4, mutedColor, false));
                statusValues[j] = label("—", bodySp - 1, textColor, false);
                cell.addView(statusValues[j]);
                statusRow.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
            }
            statusGrid.addView(statusRow);
        }
        statusCard.addView(statusGrid, spacedFullWidth(dp(12)));
        pinPlayer = action("固定当前播放器", true, v -> {
            PlayerSnapshot snapshot = BridgeStateStore.getSnapshot();
            if (snapshot == null) return;
            settings.setSelectedPackage(snapshot.packageName);
            MediaListenerService.requestRefresh(this);
            refreshStatus();
        });
        Button autoPlayer = action("恢复自动选择", true, v -> {
            settings.clearSelectedPackage();
            MediaListenerService.requestRefresh(this);
            refreshStatus();
        });
        ignorePlayer = action("忽略当前播放器", true, v -> {
            PlayerSnapshot snapshot = BridgeStateStore.getSnapshot();
            if (snapshot == null) return;
            settings.setBlacklisted(snapshot.packageName, true);
            MediaListenerService.requestRefresh(this);
            refreshStatus();
        });
        openPlayer = action("打开当前播放器", true, v -> openCurrentPlayer());
        statusCard.addView(actionGroup(pinPlayer, autoPlayer, ignorePlayer, openPlayer), spacedFullWidth(dp(14)));
        TextView playerActionHelp = label("固定：持续选用当前播放器 · 忽略：不再桥接当前播放器 · 自动选择：取消固定并由系统选择 · 打开：进入当前播放器",
                bodySp - 4, mutedColor, false);
        statusCard.addView(playerActionHelp, spacedFullWidth(dp(10)));
        playerListsView = label("当前会话：—　历史播放器：—　已忽略：—", bodySp - 4, mutedColor, false);
        statusCard.addView(playerListsView, spacedFullWidth(dp(12)));
        addCard(root, statusCard);

        addSection(root, "其他设置");
        LinearLayout logCard = card(cardColor);
        logCard.addView(label("导出诊断日志", bodySp + 1, textColor, true));
        logCard.addView(label("直接保存到 Download/MediaBridge，默认包含当前歌名、歌手和专辑", bodySp - 3, mutedColor, false));
        Button exportLog = action("导出", false, v -> exportToDownloadsFallback());
        logCard.addView(exportLog, spacedFullWidth(dp(12)));
        addCard(root, logCard);

        feedbackView = label("", bodySp - 3, blueColor, false);
        root.addView(feedbackView);
        addSection(root, getString(com.mediabridge.app.R.string.section_about));
        LinearLayout aboutCard = card(cardColor);
        aboutCard.addView(label(getString(com.mediabridge.app.R.string.about_developer),
                bodySp, textColor, true));
        aboutCard.addView(label(getString(com.mediabridge.app.R.string.about_version, appVersionName()),
                bodySp - 3, mutedColor, false), spacedFullWidth(dp(8)));
        addCard(root, aboutCard);
        refreshIgnoredList();
        return scroll;
    }

    private void prepareColors() {
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        nightMode = night;
        pageColor = Color.parseColor(night ? "#30344F" : "#E8EBF2");
        cardColor = Color.parseColor(night ? "#4B5170" : "#F7F8FC");
        heroColor = Color.parseColor(night ? "#565D80" : "#FFFFFF");
        textColor = Color.parseColor(night ? "#FDF6E9" : "#24283C");
        mutedColor = Color.parseColor(night ? "#C3C8DC" : "#666D82");
        blueColor = Color.parseColor(night ? "#73AFFF" : "#1F6BFF");
        buttonColor = Color.parseColor(night ? "#6A7194" : "#E3E6EF");
        lineColor = Color.parseColor(night ? "#66708F" : "#D7DBE5");
        int width = getResources().getConfiguration().screenWidthDp;
        bodySp = (width >= 1200 ? 22 : width >= 700 ? 20 : 18) + 2;
        wideLayout = width >= 760;
        getWindow().setStatusBarColor(pageColor);
        getWindow().setNavigationBarColor(pageColor);
        if (!night) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private TextView label(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setLineSpacing(dp(2), 1f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable roundRect(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        if (strokeWidth > 0) shape.setStroke(strokeWidth, strokeColor);
        return shape;
    }

    private LinearLayout card(int color) {
        LinearLayout view = column();
        view.setPadding(dp(22), dp(20), dp(22), dp(20));
        view.setBackground(roundRect(color, dp(13), 0, 0));
        view.setElevation(dp(3));
        return view;
    }

    private void addCard(LinearLayout root, View card) {
        LinearLayout.LayoutParams params = fullWidth();
        params.bottomMargin = dp(11);
        root.addView(card, params);
    }

    private void addSection(LinearLayout root, String name) {
        TextView view = label(name, bodySp + 1, blueColor, true);
        LinearLayout.LayoutParams params = fullWidth();
        params.topMargin = dp(18);
        params.bottomMargin = dp(11);
        root.addView(view, params);
    }

    private Switch blueSwitch(boolean checked, String description) {
        Switch view = new Switch(this);
        view.setContentDescription(description);
        view.setText("");
        view.setShowText(false);
        view.setSwitchMinWidth(dp(96));
        view.setMinHeight(dp(52));
        view.setSplitTrack(false);
        StateListDrawable track = new StateListDrawable();
        GradientDrawable on = roundRect(Color.parseColor("#2F7DFF"), dp(26), 0, 0);
        on.setSize(dp(96), dp(52));
        GradientDrawable off = roundRect(Color.parseColor(nightMode ? "#7D84A0" : "#B9BFD0"), dp(26), 0, 0);
        off.setSize(dp(96), dp(52));
        track.addState(new int[]{android.R.attr.state_checked}, on);
        track.addState(new int[]{}, off);
        GradientDrawable thumb = roundRect(Color.WHITE, dp(22), 0, 0);
        thumb.setSize(dp(39), dp(39));
        view.setTrackDrawable(track);
        view.setThumbDrawable(thumb);
        view.setChecked(checked);
        return view;
    }

    private LinearLayout settingRow(String heading, String description, View control) {
        LinearLayout row = new LinearLayout(this);
        boolean horizontal = wideLayout || control instanceof Switch;
        row.setOrientation(horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        row.setGravity(horizontal ? Gravity.CENTER_VERTICAL : Gravity.START);
        row.setMinimumHeight(dp(68));
        LinearLayout words = column();
        words.addView(label(heading, bodySp + 1, textColor, true));
        words.addView(label(description, bodySp - 3, mutedColor, false));
        if (horizontal) {
            row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(-2, -2);
            controlParams.leftMargin = dp(15);
            row.addView(control, controlParams);
        } else {
            row.addView(words, fullWidth());
            row.addView(control, spacedFullWidth(dp(12)));
        }
        return row;
    }

    private Button action(String caption, boolean outlined, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(caption);
        button.setTextSize(bodySp - 2);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setMinHeight(dp(54));
        button.setMinimumHeight(dp(54));
        button.setPadding(dp(15), dp(8), dp(15), dp(8));
        button.setTextColor(outlined ? blueColor : textColor);
        button.setBackground(roundRect(outlined ? cardColor : buttonColor, dp(9),
                outlined ? blueColor : 0, outlined ? dp(1) : 0));
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout actionGroup(Button... buttons) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(wideLayout ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        for (int i = 0; i < buttons.length; i++) {
            LinearLayout.LayoutParams params = wideLayout
                    ? new LinearLayout.LayoutParams(0, -2, 1)
                    : new LinearLayout.LayoutParams(-1, -2);
            if (i > 0) {
                if (wideLayout) params.leftMargin = dp(9);
                else params.topMargin = dp(8);
            }
            group.addView(buttons[i], params);
        }
        return group;
    }

    private LinearLayout actionRow(Button... buttons) {
        LinearLayout group = horizontal();
        for (int i = 0; i < buttons.length; i++) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) params.leftMargin = dp(9);
            group.addView(buttons[i], params);
        }
        return group;
    }

    private void refreshIgnoredList() {
        if (ignoredListView == null) return;
        Set<String> packages = new HashSet<>(settings.getSeenPackages());
        packages.addAll(settings.getActivePackages());
        packages.addAll(settings.getBlacklist());
        ArrayList<String> sorted = new ArrayList<>(packages);
        Collections.sort(sorted);
        ArrayList<String> ignored = new ArrayList<>(settings.getBlacklist());
        Collections.sort(ignored);
        String signature = sorted.toString() + ignored.toString();
        if (signature.equals(lastIgnoredSignature)) return;
        lastIgnoredSignature = signature;
        ignoredListView.removeAllViews();
        if (sorted.isEmpty()) {
            ignoredListView.addView(label("暂无发现的媒体应用，可手动添加包名", bodySp - 2, mutedColor, false));
            return;
        }
        PlayerNameResolver resolver = new PlayerNameResolver(this);
        for (String packageName : sorted) {
            CheckBox checkbox = new CheckBox(this);
            checkbox.setText(resolver.resolve(packageName).label + "\n" + packageName);
            checkbox.setTextSize(bodySp - 2);
            checkbox.setTextColor(textColor);
            checkbox.setButtonTintList(ColorStateList.valueOf(blueColor));
            checkbox.setPadding(0, dp(11), 0, dp(11));
            checkbox.setMinHeight(dp(70));
            checkbox.setChecked(settings.getBlacklist().contains(packageName));
            checkbox.setOnCheckedChangeListener((button, checked) -> {
                settings.setBlacklisted(packageName, checked);
                MediaListenerService.requestRefresh(this);
                handler.post(this::refreshStatus);
            });
            ignoredListView.addView(checkbox, fullWidth());
        }
    }

    private void refreshFavoriteIgnoredList() {
        if (favoriteIgnoredListView == null) return;
        Set<String> packages = new HashSet<>(settings.getSeenPackages());
        packages.addAll(settings.getActivePackages());
        packages.addAll(settings.getFavoriteIgnoredPackages());
        ArrayList<String> sorted = new ArrayList<>(packages);
        Collections.sort(sorted);
        ArrayList<String> ignored = new ArrayList<>(settings.getFavoriteIgnoredPackages());
        Collections.sort(ignored);
        String signature = sorted.toString() + ignored.toString();
        if (signature.equals(lastFavoriteIgnoredSignature)) return;
        lastFavoriteIgnoredSignature = signature;
        favoriteIgnoredListView.removeAllViews();
        if (sorted.isEmpty()) {
            favoriteIgnoredListView.addView(label("暂无发现的媒体应用，可手动添加包名", bodySp - 2, mutedColor, false));
            return;
        }
        PlayerNameResolver resolver = new PlayerNameResolver(this);
        for (String packageName : sorted) {
            CheckBox checkbox = new CheckBox(this);
            checkbox.setText(resolver.resolve(packageName).label + "\n" + packageName);
            checkbox.setTextSize(bodySp - 2);
            checkbox.setTextColor(textColor);
            checkbox.setButtonTintList(ColorStateList.valueOf(blueColor));
            checkbox.setPadding(0, dp(11), 0, dp(11));
            checkbox.setMinHeight(dp(70));
            checkbox.setChecked(settings.isFavoriteIgnored(packageName));
            checkbox.setOnCheckedChangeListener((button, checked) -> {
                settings.setFavoriteIgnored(packageName, checked);
                applySettingsImmediately();
                handler.post(this::refreshStatus);
            });
            favoriteIgnoredListView.addView(checkbox, fullWidth());
        }
    }

    private void showAddFavoriteIgnoredDialog() {
        EditText input = new EditText(this);
        input.setHint("应用包名，例如 cn.kuwo.kwmusiccar");
        input.setSingleLine(true);
        input.setTextSize(bodySp - 2);
        LinearLayout wrapper = column();
        wrapper.setPadding(dp(22), dp(8), dp(22), 0);
        wrapper.addView(input);
        new AlertDialog.Builder(this)
                .setTitle("手动添加收藏忽略应用")
                .setView(wrapper)
                .setNegativeButton("取消", null)
                .setPositiveButton("添加", (dialog, which) -> {
                    String packageName = input.getText().toString().trim();
                    if (!packageName.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) {
                        Toast.makeText(this, "请输入有效的应用包名", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    settings.setFavoriteIgnored(packageName, true);
                    applySettingsImmediately();
                    refreshStatus();
                }).show();
    }

    private void showAddIgnoredDialog() {
        EditText input = new EditText(this);
        input.setHint("应用包名，例如 cn.kuwo.kwmusiccar");
        input.setSingleLine(true);
        input.setTextSize(bodySp - 2);
        LinearLayout wrapper = column();
        wrapper.setPadding(dp(22), dp(8), dp(22), 0);
        wrapper.addView(input);
        new AlertDialog.Builder(this)
                .setTitle("手动添加忽略应用")
                .setView(wrapper)
                .setNegativeButton("取消", null)
                .setPositiveButton("添加", (dialog, which) -> {
                    String packageName = input.getText().toString().trim();
                    if (!packageName.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) {
                        Toast.makeText(this, "请输入有效的应用包名", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    settings.setBlacklisted(packageName, true);
                    MediaListenerService.requestRefresh(this);
                    refreshStatus();
                }).show();
    }

    private String appVersionName() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return TextUtils.isEmpty(versionName) ? "—" : versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException error) {
            DiagnosticsLog.e("Unable to read app version", error);
            return "—";
        }
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams spacedFullWidth(int topMargin) {
        LinearLayout.LayoutParams params = fullWidth();
        params.topMargin = topMargin;
        return params;
    }

    private void changeTune(int delta) {
        int value = Math.max(-3000, Math.min(3000, settings.getTuneMs() + delta));
        settings.setTuneMs(value);
        refreshTune();
        // The running service receives the updated setting on its next snapshot; this broadcast
        // also makes the current-song timeline recalculate in the complete backend.
        sendBroadcast(BridgeEvents.stateIntent());
        applySettingsImmediately();
    }

    private void applySettingsImmediately() {
        if (!settings.isBridgeEnabled()) return;
        Intent intent = BridgeEvents.serviceIntent(this, BridgeEvents.ACTION_SETTINGS_CHANGED);
        try {
            startForegroundService(intent);
        } catch (RuntimeException error) {
            DiagnosticsLog.e("Unable to apply bridge settings", error);
        }
    }

    private void refreshStatus() {
        if (statusValues == null) return;
        NotificationAccessStatus access = notificationAccessStatus();
        boolean granted = access == NotificationAccessStatus.GRANTED;
        if (granted) settings.markNotificationAccessGranted();
        if (access == NotificationAccessStatus.NOT_GRANTED && (lastAccess != access || BridgeStateStore.getSnapshot() != null)) {
            MediaListenerService.observeAccess(NotificationAccess.State.DENIED);
            BridgeStateStore.clearSnapshot();
            BridgeStateStore.setInputState("通知访问未授权，无法读取播放器");
            invalidateVehicleSnapshot();
        }
        lastAccess = access;
        String accessText;
        if (granted) accessText = "通知访问：已授权";
        else if (access == NotificationAccessStatus.UNKNOWN) accessText = "暂时无法确认通知访问状态，请稍后重新检查。";
        else if (settings.wasNotificationAccessEverGranted()) accessText = "通知访问已关闭，暂时无法读取播放器。请重新开启通知访问。";
        else accessText = "尚未开启通知访问。请先授权后启动桥接。";
        if (accessBanner != null) {
            String checked = android.text.format.DateFormat.format("HH:mm:ss", NotificationAccess.lastCheckMs).toString();
            accessBanner.setText(accessText + "  最后检查：" + checked);
            accessBanner.setTextColor(granted ? textColor : Color.rgb(210, 120, 40));
        }
        PlayerSnapshot snapshot = BridgeStateStore.getSnapshot();
        String availability;
        if (!settings.isBridgeEnabled()) availability = "已停用";
        else if (access == NotificationAccessStatus.UNKNOWN) availability = "授权状态待确认";
        else if (!granted) availability = "输入不可用";
        else if (snapshot == null) availability = "等待播放器";
        else availability = "播放器已就绪";
        statusValues[0].setText(accessText);
        statusValues[1].setText(availability);
        statusValues[2].setText(BridgeStateStore.getInputState());
        statusValues[3].setText(BridgeStateStore.getOutputState() + "\n" + BridgeStateStore.getSourceDirectoryState());
        statusValues[4].setText(String.valueOf(BridgeStateStore.getGeneration()));
        if (snapshot == null) {
            playerView.setText("当前播放器：—");
        } else {
            playerView.setText("当前播放器：" + snapshot.appLabel + "\n包名：" + snapshot.packageName
                    + "\n用户：" + snapshot.userId + "\n歌曲：" + snapshot.title + " / " + snapshot.artist
                    + "\n名称来源：" + snapshot.labelSource + "\n歌词来源：" + BridgeStateStore.getLyricsState()
                    + "\n封面来源：" + BridgeStateStore.getArtworkState() + "\n收藏：" + snapshot.favoriteMessage);
        }
        refreshControlAvailability(granted, snapshot);
        refreshTune();
        refreshCollectionHint();
        refreshPlayerLists();
    }

    private void refreshControlAvailability(boolean notificationAccessGranted, PlayerSnapshot snapshot) {
        boolean hasUsableSession = notificationAccessGranted && snapshot != null;
        if (pinPlayer != null) pinPlayer.setEnabled(hasUsableSession);
        if (ignorePlayer != null) ignorePlayer.setEnabled(hasUsableSession);
        if (openPlayer != null) openPlayer.setEnabled(hasUsableSession);
        if (retryLyrics != null) {
            retryLyrics.setEnabled(hasUsableSession && settings.isLyricsEnabled());
        }
    }

    private void openCurrentPlayer() {
        PlayerSnapshot snapshot = BridgeStateStore.getSnapshot();
        if (snapshot == null) return;
        try {
            if (snapshot.sessionActivity != null) {
                snapshot.sessionActivity.send();
                return;
            }
            Intent launch = PlayerFeatures.playerIntent(this, snapshot.packageName);
            if (launch != null) startActivity(launch);
            else playerView.setText("无法打开播放器：未找到公开启动入口\n" + playerView.getText());
        } catch (Exception error) {
            DiagnosticsLog.e("Unable to open selected player", error);
            playerView.setText("无法打开当前播放器\n" + playerView.getText());
        }
    }

    private void refreshPlayerLists() {
        if (playerListsView == null) return;
        playerListsView.setText("当前会话：" + formatPackages(settings.getActivePackages())
                + "\n历史播放器：" + formatPackages(settings.getSeenPackages())
                + "\n已忽略：" + formatPackages(settings.getBlacklist()));
        refreshIgnoredList();
        refreshFavoriteIgnoredList();
    }

    private String formatPackages(java.util.Set<String> packages) {
        if (packages == null || packages.isEmpty()) return "—";
        java.util.ArrayList<String> sorted = new java.util.ArrayList<>(packages);
        java.util.Collections.sort(sorted);
        StringBuilder value = new StringBuilder();
        PlayerNameResolver resolver = new PlayerNameResolver(this);
        for (String packageName : sorted) {
            if (value.length() > 0) value.append("；");
            value.append(resolver.resolve(packageName).label).append("（").append(packageName).append("）");
        }
        return value.toString();
    }

    private void refreshTune() {
        if (tuneView != null) {
            int value = settings.getTuneMs();
            int base = settings.getBaseOffsetMs(BackendMode.LEGACY);
            tuneView.setText("用户微调 " + (value > 0 ? "+" : "") + value + "ms · 基础补偿 " + base
                    + "ms · 合计 " + (base + value) + "ms（正值提前）\n歌词：" + BridgeStateStore.getLyricsState());
            if (tuneMinus != null) tuneMinus.setEnabled(value > -3000);
            if (tunePlus != null) tunePlus.setEnabled(value < 3000);
        }
    }

    private void refreshCollectionHint() {
        if (collectionHint == null) return;
        PlayerSnapshot snapshot = BridgeStateStore.getSnapshot();
        if (!settings.isShowCollectionEnabled()) {
            collectionHint.setText("已关闭原车媒体界面的收藏入口。");
        } else if (snapshot == null) {
            collectionHint.setText("等待播放器；已发现的应用默认显示红心。");
        } else if (settings.isFavoriteIgnored(snapshot.packageName)) {
            collectionHint.setText("当前播放器在收藏忽略名单中，红心已隐藏。");
        } else if (snapshot.favoriteMessage != null && snapshot.favoriteMessage.startsWith("收藏失败")) {
            collectionHint.setText(snapshot.favoriteMessage + "；红心仍显示，可切歌后重试或加入收藏忽略名单。");
        } else if (snapshot.favoritePending) {
            collectionHint.setText("已发送收藏请求，等待播放器确认；请勿连续点击。");
        } else if (snapshot.favoriteState == FavoriteState.UNKNOWN) {
            collectionHint.setText("当前播放器未提供可靠收藏状态；红心供实测，点击后需以播放器内结果为准。");
        } else if (!snapshot.favoriteWriteSupported) {
            collectionHint.setText("当前播放器未声明标准收藏操作；红心供实测，失败会提示。");
        } else {
            collectionHint.setText("播放器已提供收藏状态；红心默认显示，实际结果以播放器回传为准。");
        }
    }

    private enum NotificationAccessStatus { GRANTED, NOT_GRANTED, UNKNOWN }

    private NotificationAccessStatus notificationAccessStatus() {
        NotificationAccess.State state = NotificationAccess.check(this);
        return state == NotificationAccess.State.GRANTED ? NotificationAccessStatus.GRANTED
                : state == NotificationAccess.State.DENIED ? NotificationAccessStatus.NOT_GRANTED : NotificationAccessStatus.UNKNOWN;
    }

    private void openNotificationSettings() {
        try {
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        } catch (RuntimeException error) {
            DiagnosticsLog.e("Unable to open notification listener settings", error);
            new AlertDialog.Builder(this).setTitle("需要手动打开通知访问")
                    .setMessage("请在车机系统设置中查找：应用 → 特殊应用权限 → 通知使用权（或通知访问），开启 MediaBridge 后返回。")
                    .setPositiveButton("打开系统设置", (dialog, which) -> {
                        try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
                        catch (RuntimeException unavailable) { showFeedback("无法打开系统设置，请从车机桌面手动进入设置。"); }
                    }).setNegativeButton("知道了", null).show();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerStateReceiver() {
        IntentFilter filter = new IntentFilter(BridgeEvents.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stateReceiver, filter, null, handler, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, filter, null, handler);
        }
    }

    private boolean requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
            return true;
        }
        return false;
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 42) handler.post(this::maybePromptNotificationAccess);
        else if (requestCode == REQUEST_WRITE_DOWNLOADS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) exportToDownloadsFallback();
            else showFeedback("无法写入下载目录：未授予存储权限");
        }
    }

    private void invalidateVehicleSnapshot() {
        if (!settings.isBridgeEnabled() || !BridgeStateStore.isOutputRunning()) return;
        try {
            startService(BridgeEvents.serviceIntent(this, BridgeEvents.ACTION_INPUT_INVALIDATED));
        } catch (RuntimeException error) {
            DiagnosticsLog.e("Unable to invalidate vehicle snapshot from UI", error);
        }
    }

    private void ensureServiceRunning() {
        ensureServiceRunning(false);
    }

    private void ensureServiceRunning(boolean reconnect) {
        if (!settings.isBridgeEnabled()) return;
        MediaListenerService.requestRefresh(this);
        Intent intent = BridgeEvents.serviceIntent(this,
                reconnect ? BridgeEvents.ACTION_RECONNECT : BridgeEvents.ACTION_START);
        try {
            startForegroundService(intent);
        } catch (RuntimeException error) {
            DiagnosticsLog.e("Unable to start bridge service", error);
        }
    }
    private void showFeedback(String message) {
        if (feedbackView != null) feedbackView.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    private void background(Runnable task) {
        try { UI_WORK.execute(task); }
        catch (java.util.concurrent.RejectedExecutionException busy) { showFeedback("正在处理其他操作，请稍后重试"); }
    }
    private void chooseLyricsSources() {
        String[] names = {"NetEase", "QQMusic", "Kuwo", "LrcLib", "Zvuk"};
        String[] labels = {"网易云音乐", "QQ 音乐", "酷我", "LrcLib", "Zvuk"};
        Set<String> selected = settings.getLyricsSources();
        boolean[] checked = new boolean[names.length];
        for (int i = 0; i < names.length; i++) checked[i] = selected.contains(names[i]);
        new AlertDialog.Builder(this).setTitle("歌词来源")
                .setMultiChoiceItems(labels, checked, (dialog, which, value) -> { if (value) selected.add(names[which]); else selected.remove(names[which]); })
                .setPositiveButton("保存", (dialog, which) -> { settings.setLyricsSources(selected); applySettingsImmediately(); })
                .setNegativeButton("取消", null).show();
    }
    private void editBaseOffset() {
        BackendMode mode = BackendMode.LEGACY;
        EditText value = new EditText(this);
        value.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        value.setText(String.valueOf(settings.getBaseOffsetMs(mode)));
        new AlertDialog.Builder(this).setTitle("基础补偿（毫秒）")
                .setMessage("用于通道自身的显示延迟；与用户微调相加。范围 -5000～5000，默认 1000。")
                .setView(value).setPositiveButton("保存", (dialog, which) -> {
                    try { settings.setBaseOffsetMs(mode, Integer.parseInt(value.getText().toString())); refreshTune(); applySettingsImmediately(); }
                    catch (NumberFormatException invalid) { showFeedback("请输入有效整数"); }
                }).setNegativeButton("取消", null).show();
    }
    private void maybePromptNotificationAccess() {
        if (notificationAccessPromptShown || isFinishing() || isDestroyed()
                || notificationAccessStatus() != NotificationAccessStatus.NOT_GRANTED) return;
        notificationAccessPromptShown = true;
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("需要开启通知访问")
                .setMessage("MediaBridge 需要通知访问权限来识别正在播放的媒体应用。请前往系统通知访问设置并开启 MediaBridge。")
                .setPositiveButton("去开启", (ignoredDialog, which) -> openNotificationSettings())
                .setNegativeButton("稍后", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            int alertTitleId = getResources().getIdentifier("alertTitle", "id", "android");
            if (alertTitleId != 0) increaseTextSize(dialog.findViewById(alertTitleId), 5);
            increaseTextSize(dialog.findViewById(android.R.id.message), 5);
            increaseTextSize(dialog.getButton(AlertDialog.BUTTON_POSITIVE), 5);
            increaseTextSize(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), 5);
        });
        dialog.show();
    }

    private void increaseTextSize(TextView view, int extraSp) {
        if (view == null) return;
        float currentSp = view.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
        view.setTextSize(currentSp + extraSp);
    }
    private void clearMediaCaches() {
        background(() -> {
            com.geely.auto.music.lyrics.LyricsManager.clearCache(getApplicationContext());
            ArtworkHelper.clearCache(getApplicationContext());
            handler.post(() -> {
                MediaListenerService.requestRefresh(this);
                if (settings.isBridgeEnabled()) startService(BridgeEvents.serviceIntent(this, BridgeEvents.ACTION_RETRY_LYRICS));
                showFeedback("歌词与封面缓存已清除，设置保持不变");
            });
        });
    }
    private void exportToDownloadsFallback() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            try {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_DOWNLOADS);
                showFeedback("请允许存储权限，以保存到 Download/MediaBridge");
            } catch (RuntimeException error) {
                DiagnosticsLog.e("Unable to request Downloads permission", error);
                showFeedback("无法请求下载目录权限");
            }
            return;
        }
        showFeedback("正在保存到 Download/MediaBridge…");
        background(() -> {
            String path = DiagnosticsLog.exportToDownloads(getApplicationContext(), true);
            handler.post(() -> showFeedback(path == null
                    ? "保存失败：无法写入 Download/MediaBridge"
                    : "诊断日志已保存到 " + path));
        });
    }

}
