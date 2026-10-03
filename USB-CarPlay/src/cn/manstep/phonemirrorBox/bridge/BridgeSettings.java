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
    static final String BUILD = "2026.10.03-r2";
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
    // Resource ID is verified against the fixed original APK, absent from the isolated test R class.
    @android.annotation.SuppressLint("ResourceType")
    public static View wrap(View original) {
        if (original == null || android.os.Build.VERSION.SDK_INT < 28) return original;
        try {
            Context c = original.getContext();
            // Fixed baseline's actual settings ScrollView. Preserve its toolbar, root and bindings.
            View scroll = original.findViewById(0x7f090204);
            if (!(scroll instanceof android.widget.ScrollView)) return original;
            View list = ((android.widget.ScrollView)scroll).getChildAt(0);
            if (!(list instanceof LinearLayout) || list.findViewWithTag("usb-media-options-v2") != null) return original;
            LinearLayout entries = new LinearLayout(c); entries.setOrientation(LinearLayout.VERTICAL);
            entries.setTag("usb-media-options-v2");
            TextView mode = row(entries, "媒体接入");
            TextView format = row(entries, "缺失歌手时的标题格式");
            TextView online = row(entries, "直连在线封面与歌词");
            Runnable refresh = () -> {
                mode.setText(label(mode(c)) + "  ›");
                format.setText(CombinedTitleMetadata.LABELS[CombinedTitleMetadata.index(titleFormat(c))] + "  ›");
                online.setText((onlineResources(c) ? "已开启" : "已关闭") + "  ›");
            };
            refresh.run();
            ((View)mode.getParent()).setOnClickListener(v -> show(c, refresh));
            ((View)format.getParent()).setOnClickListener(v -> showTitleFormat(c, refresh));
            ((View)online.getParent()).setOnClickListener(v -> new AlertDialog.Builder(c)
                    .setTitle("直连在线封面与歌词")
                    .setSingleChoiceItems(new String[]{"关闭（仍使用原生内容和缓存）", "开启（需要车机联网）"}, onlineResources(c) ? 1 : 0, (dialog, which) -> {
                        c.getSharedPreferences("usb_media_route_v1", 0).edit().putBoolean("online_resources", which == 1).apply();
                        UsbMediaBridge.resourcesChanged(); refresh.run(); dialog.dismiss();
                    }).setPositiveButton("关闭", null).show());
            ((LinearLayout)list).addView(entries, 0, new LinearLayout.LayoutParams(-1, -2));
            return original;
        } catch (RuntimeException error) { return original; }
    }
    private static int style(Context c, int id) {
        try { c.getResources().getResourceTypeName(id); return id; }
        catch (android.content.res.Resources.NotFoundException missing) { return 0; }
    }
    private static TextView row(LinearLayout entries, String title) {
        Context c = entries.getContext();
        LinearLayout line = new LinearLayout(c, null, 0, style(c, 0x7f100166));
        line.setOrientation(LinearLayout.HORIZONTAL); line.setGravity(Gravity.CENTER_VERTICAL);
        int height = (int)(50 * c.getResources().getDisplayMetrics().density + .5f);
        android.content.res.TypedArray attrs = c.obtainStyledAttributes(style(c, 0x7f100166), new int[]{android.R.attr.layout_height});
        line.setMinimumHeight(attrs.getLayoutDimension(0, height)); attrs.recycle();
        TextView label = new TextView(c, null, 0, style(c, 0x7f10016c));
        label.setBackground(null); label.setText(title); label.setGravity(Gravity.CENTER_VERTICAL);
        TextView value = new TextView(c, null, 0, style(c, 0x7f10016d));
        value.setBackground(null); value.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        value.setMaxLines(2);
        line.addView(label, new LinearLayout.LayoutParams(0, -2, 1.1f));
        line.addView(value, new LinearLayout.LayoutParams(0, -2, 1f));
        entries.addView(line, new LinearLayout.LayoutParams(-1, -2));
        return value;
    }
    private static void showTitleFormat(Context c, Runnable refresh) {
        new AlertDialog.Builder(c).setTitle("缺失歌手时的标题格式")
                .setSingleChoiceItems(CombinedTitleMetadata.LABELS, CombinedTitleMetadata.index(titleFormat(c)), (dialog, which) -> {
                    c.getSharedPreferences("usb_media_route_v1", 0).edit().putString("combined_title_format", CombinedTitleMetadata.FORMATS[which]).apply();
                    UsbMediaBridge.titleFormatChanged(); refresh.run(); dialog.dismiss();
                }).setPositiveButton("关闭", null).show();
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
            .setNeutralButton("接入状态", (d, w) -> new AlertDialog.Builder(c)
                .setTitle("媒体接入状态").setMessage("USB 媒体增强版 " + BUILD + "\n" + UsbMediaBridge.status()
                    + "\n\n自动：未安装 MediaBridge、关闭桥接或忽略 CarPlay 时使用 F25 直连；桥接已启用时等待 MediaBridge 就绪。"
                    + "\n\n切换只调整车机媒体通道。歌曲信息来自手机经 USB 盒子上报。")
                .setPositiveButton("关闭", null)
                .setNeutralButton("打开 MediaBridge", (a,b) -> {
                    Intent launch = c.getPackageManager().getLaunchIntentForPackage("com.mediabridge.app");
                    if (launch == null) launch = c.getPackageManager().getLaunchIntentForPackage("com.mediabridge.app.dev");
                    if (launch != null) c.startActivity(launch);
                }).show()).show();
    }
}
