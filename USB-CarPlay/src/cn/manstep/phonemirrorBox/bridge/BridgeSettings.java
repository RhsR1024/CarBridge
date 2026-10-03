package cn.manstep.phonemirrorBox.bridge;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Separate preferences and a settings-only entry; never changes original USB settings. */
public final class BridgeSettings {
    private static final String[] MODES = {"AUTO", "DIRECT", "BRIDGE"};
    private static final String[] LABELS = {"自动", "F25 直连", "MediaBridge 桥接"};
    private BridgeSettings() {}
    public static String mode(Context c) {
        String v = c.getSharedPreferences("usb_media_route_v1", 0).getString("mode", "AUTO");
        return "DIRECT".equals(v) || "BRIDGE".equals(v) ? v : "AUTO";
    }
    public static String choose(String mode, boolean ignored, boolean enabled, boolean ready) {
        if ("DIRECT".equals(mode)) return "DIRECT";
        if ("BRIDGE".equals(mode)) return ignored ? "IGNORED" : enabled && ready ? "BRIDGE" : "WAIT";
        return ignored || !enabled ? "DIRECT" : ready ? "BRIDGE" : "WAIT";
    }
    static String titleFormat(Context c) {
        return CombinedTitleMetadata.normalizeFormat(c.getSharedPreferences("usb_media_route_v1", 0)
                .getString("combined_title_format", "ORIGINAL"));
    }
    static boolean onlineResources(Context c) {
        return c.getSharedPreferences("usb_media_route_v1", 0).getBoolean("online_resources", false);
    }
    public static View wrap(View original) {
        if (original == null || original.getParent() != null || android.os.Build.VERSION.SDK_INT < 28) return original;
        try {
            Context c = original.getContext();
            LinearLayout root = new LinearLayout(c);
            root.setOrientation(LinearLayout.VERTICAL);
            TextView entry = new TextView(c);
            entry.setTextColor(Color.WHITE); entry.setBackgroundColor(0xff263544);
            entry.setTextSize(16); entry.setGravity(Gravity.CENTER_VERTICAL);
            int padding = (int)(16 * c.getResources().getDisplayMetrics().density + .5f);
            entry.setPadding(padding, 0, padding, 0);
            Runnable refresh = () -> entry.setText("媒体接入：" + label(mode(c)) + "  ›");
            refresh.run();
            entry.setOnClickListener(v -> show(c, refresh));
            root.addView(entry, new LinearLayout.LayoutParams(-1,
                    (int)(52 * c.getResources().getDisplayMetrics().density + .5f)));
            root.addView(original, new LinearLayout.LayoutParams(-1, 0, 1));
            return root;
        } catch (RuntimeException error) { return original; }
    }
    private static String label(String value) {
        for (int i=0; i<MODES.length; i++) if (MODES[i].equals(value)) return LABELS[i];
        return LABELS[0];
    }
    private static void show(Context c, Runnable refresh) {
        String current = mode(c); int selected = 0;
        for (int i=0; i<MODES.length; i++) if (MODES[i].equals(current)) selected = i;
        new AlertDialog.Builder(c).setTitle("媒体接入方式")
            .setSingleChoiceItems(LABELS, selected, (dialog, which) -> {
                c.getSharedPreferences("usb_media_route_v1", 0).edit().putString("mode", MODES[which]).apply();
                UsbMediaBridge.refresh(); refresh.run(); dialog.dismiss();
            })
            .setPositiveButton("关闭", null)
            .setNegativeButton("媒体选项", (dialog, which) -> showMediaOptions(c))
            .setNeutralButton("接入状态", (d, w) -> new AlertDialog.Builder(c)
                .setTitle("媒体接入状态").setMessage(UsbMediaBridge.status()
                    + "\n\n自动：未安装 MediaBridge、关闭桥接或忽略 CarPlay 时使用 F25 直连；桥接已启用时等待 MediaBridge 就绪。"
                    + "\n\n切换只调整车机媒体通道。歌曲信息来自手机经 USB 盒子上报。")
                .setPositiveButton("关闭", null)
                .setNeutralButton("打开 MediaBridge", (a,b) -> {
                    Intent launch = c.getPackageManager().getLaunchIntentForPackage("com.mediabridge.app");
                    if (launch == null) launch = c.getPackageManager().getLaunchIntentForPackage("com.mediabridge.app.dev");
                    if (launch != null) c.startActivity(launch);
                }).show()).show();
    }
    private static void showMediaOptions(Context c) {
        LinearLayout body = new LinearLayout(c); body.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(16 * c.getResources().getDisplayMetrics().density + .5f);
        body.setPadding(pad, pad, pad, pad);
        android.widget.Button format = new android.widget.Button(c);
        Runnable label = () -> format.setText("缺失歌手时的标题格式\n" + CombinedTitleMetadata.LABELS[CombinedTitleMetadata.index(titleFormat(c))]);
        label.run();
        format.setOnClickListener(view -> new AlertDialog.Builder(c).setTitle("缺失歌手时的标题格式")
                .setSingleChoiceItems(CombinedTitleMetadata.LABELS, CombinedTitleMetadata.index(titleFormat(c)), (dialog, which) -> {
                    c.getSharedPreferences("usb_media_route_v1", 0).edit().putString("combined_title_format", CombinedTitleMetadata.FORMATS[which]).apply();
                    label.run(); dialog.dismiss();
                    android.widget.Toast.makeText(c, "重新连接手机后生效", android.widget.Toast.LENGTH_SHORT).show();
                }).setPositiveButton("关闭", null).show());
        body.addView(format, new LinearLayout.LayoutParams(-1, -2));
        TextView hint = new TextView(c);
        hint.setText("仅歌手为空且标题使用带空格的横线分为两段时拆分。已有歌手保持原样；例如“周铁男 - 三国杀”选“歌手 - 歌曲名”。重新连接手机后生效。");
        body.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        android.widget.Switch online = new android.widget.Switch(c);
        online.setText("直连在线封面与歌词"); online.setPadding(0, pad, 0, pad);
        online.setChecked(onlineResources(c));
        online.setOnCheckedChangeListener((button, enabled) -> {
            c.getSharedPreferences("usb_media_route_v1", 0).edit().putBoolean("online_resources", enabled).apply();
            UsbMediaBridge.resourcesChanged();
        });
        body.addView(online, new LinearLayout.LayoutParams(-1, -2));
        TextView help = new TextView(c);
        help.setText("开启后，直连时向资源服务发送歌名、歌手和时长查找封面与歌词，需要车机联网。关闭后仍可用原生内容和缓存。桥接使用 MediaBridge 的资源设置。");
        body.addView(help, new LinearLayout.LayoutParams(-1, -2));
        android.widget.ScrollView scroll = new android.widget.ScrollView(c); scroll.addView(body);
        new AlertDialog.Builder(c).setTitle("媒体选项").setView(scroll).setPositiveButton("关闭", null).show();
    }
}
