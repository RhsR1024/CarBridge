package com.geely.auto.music;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import java.util.Locale;

/** A cached label is usable only after the installation and locale identity is verified. */
public final class PlayerNameResolver {
    public static final class Result {
        public final String label, source;
        Result(String label, String source) { this.label = label; this.source = source; }
    }
    private final PackageManager packages;
    private final SharedPreferences cache;
    private final int userId;
    public PlayerNameResolver(Context context) {
        packages = context.getPackageManager();
        cache = context.getSharedPreferences("player_label_cache", Context.MODE_PRIVATE);
        userId = Math.max(0, Process.myUid() / 100000);
    }
    public Result resolve(String pkg) {
        String base = userId + "|" + pkg + "|";
        String identity;
        PackageInfo info;
        try {
            info = packages.getPackageInfo(pkg, 0);
            identity = info.getLongVersionCode() + "|" + info.firstInstallTime + "|" + info.lastUpdateTime
                    + "|" + Locale.getDefault().toLanguageTag();
        } catch (PackageManager.NameNotFoundException error) {
            cache.edit().remove(base + "label").remove(base + "identity").apply();
            return new Result(pkg, "未能读取应用名称：未安装或对本应用不可见");
        } catch (RuntimeException error) {
            return new Result(pkg, "未能确认安装信息：" + error.getClass().getSimpleName());
        }
        String cached = identity.equals(cache.getString(base + "identity", "")) ? cache.getString(base + "label", "") : "";
        if (!cached.isEmpty()) return new Result(cached, "已验证缓存");
        try {
            CharSequence value = info.applicationInfo == null ? null : packages.getApplicationLabel(info.applicationInfo);
            String label = value == null ? "" : value.toString().trim();
            if (label.isEmpty()) return new Result(pkg, "未能读取应用名称：标签为空");
            cache.edit().putString(base + "identity", identity).putString(base + "label", label).apply();
            return new Result(label, "系统应用标签");
        } catch (RuntimeException error) {
            return new Result(pkg, "未能读取应用名称：" + error.getClass().getSimpleName());
        }
    }
    public int userId() { return userId; }
    public static void invalidate(Context context) { context.getSharedPreferences("player_label_cache", Context.MODE_PRIVATE).edit().clear().apply(); }
}

