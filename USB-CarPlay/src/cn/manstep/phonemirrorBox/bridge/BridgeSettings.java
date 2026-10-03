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
}
