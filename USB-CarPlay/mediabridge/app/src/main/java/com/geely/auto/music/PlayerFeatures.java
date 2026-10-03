package com.geely.auto.music;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.PlaybackState;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.SystemClock;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.Locale;

/** Verified source-specific behavior from the original app; unknown actions stay unsupported. */
final class PlayerFeatures {
    static final String NAVI = "ru.yandex.yandexnavi", YANDEX = "ru.yandex.music", ZVUK = "com.zvooq.openplay";
    static final String LX_MUSIC = "cn.toside.music.mobile";
    private static final String NAVI_PREFIX = "com.yandex.music.sdk.helper.foreground.mediasession.";
    private PlayerFeatures() {}
    static boolean handleSpecialTransport(MediaController controller, String command) {
        if (!"com.hyinfo.dab".equals(controller.getPackageName())) return false;
        int code;
        switch (command.toUpperCase(Locale.ROOT)) {
            case "NEXT": code = KeyEvent.KEYCODE_MEDIA_NEXT; break;
            case "PREVIOUS": code = KeyEvent.KEYCODE_MEDIA_PREVIOUS; break;
            case "PLAY": case "PAUSE": case "STOP": case "REWIND": case "FAST_FORWARD":
                BridgeStateStore.setLastControl(command + "：DAB 播放器不支持该操作");
                return true;
            default: return false;
        }
        long now = SystemClock.uptimeMillis();
        controller.dispatchMediaButtonEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0));
        controller.dispatchMediaButtonEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0));
        return true;
    }
    static Boolean favoriteFromState(String pkg, PlaybackState state) {
        if (state == null) return null;
        for (PlaybackState.CustomAction action : state.getCustomActions()) {
            if (NAVI.equals(pkg)) {
                if ((NAVI_PREFIX + "RESET_CUSTOM_ACTION").equals(action.getAction())) return true;
                if ((NAVI_PREFIX + "LIKE_CUSTOM_ACTION").equals(action.getAction())) return false;
            } else if (ZVUK.equals(pkg) && "com.zvooq.openplay.CUSTOM_ACTION_LIKE".equals(action.getAction())) {
                if (action.getIcon() == 2131231640) return true;
                if (action.getIcon() == 2131231641) return false;
                // Resource id parity is not a state contract; other versions remain unknown.
            }
        }
        return null;
    }
    static String favoriteAction(String pkg, PlaybackState state, boolean desired, FavoriteState current) {
        if (current == FavoriteState.UNKNOWN || (current == FavoriteState.FAVORITED) == desired) return null;
        if (NAVI.equals(pkg)) return "com.yandex.music.sdk.helper.action." + (desired ? "ADD_LIKE" : "REMOVE_LIKE");
        ArrayList<String> actions = new ArrayList<>();
        if (state != null) for (PlaybackState.CustomAction action : state.getCustomActions()) actions.add(action.getAction());
        if (ZVUK.equals(pkg) && actions.contains("com.zvooq.openplay.CUSTOM_ACTION_LIKE"))
            return "com.zvooq.openplay.CUSTOM_ACTION_LIKE";
        return FavoriteActionPolicy.select(actions, desired);
    }
    static boolean ratingSupported(String pkg, PlaybackState state, int ratingType) {
        boolean type = ratingType == android.media.Rating.RATING_HEART || ratingType == android.media.Rating.RATING_THUMB_UP_DOWN;
        return type && ((state != null && (state.getActions() & PlaybackState.ACTION_SET_RATING) != 0)
                || YANDEX.equals(pkg) || "com.spotify.music".equals(pkg));
    }
    static boolean supportsLoop(String pkg) { return NAVI.equals(pkg) || YANDEX.equals(pkg); }
    static String loopAction(int mode) { return mode == 2 ? "SHUFFLE" : mode == 1 || mode == 3 ? "REPEAT" : null; }
    static Intent playerIntent(Context context, String pkg) {
        if (NAVI.equals(pkg)) return new Intent(Intent.ACTION_VIEW, Uri.parse("yandexnavi://show_ui/music/player"))
                .setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (YANDEX.equals(pkg)) return new Intent("ACTION_OPEN_PLAYER")
                .setComponent(new ComponentName(pkg, "ru.yandex.music.main.MainScreenActivity"))
                .putExtra("need_expand", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return context.getPackageManager().getLaunchIntentForPackage(pkg);
    }
}
