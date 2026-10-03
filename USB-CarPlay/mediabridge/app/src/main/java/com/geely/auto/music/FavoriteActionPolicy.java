package com.geely.auto.music;

import java.util.List;
import java.util.Locale;

/** Only direction-bearing actions are accepted; a bare LIKE is never an unlike/toggle. */
final class FavoriteActionPolicy {
    private FavoriteActionPolicy() {}
    static String select(List<String> actions, boolean favorite) {
        if (actions == null) return null;
        for (String action : actions) {
            if (action == null) continue;
            String name = action.toUpperCase(Locale.ROOT);
            name = name.substring(name.lastIndexOf('.') + 1);
            if (favorite && (name.equals("LIKE") || name.equals("ADD_LIKE") || name.equals("ADD_FAVORITE")
                    || name.equals("SET_FAVORITE") || name.equals("FAVORITE_ON"))) return action;
            if (!favorite && (name.equals("UNLIKE") || name.equals("REMOVE_LIKE") || name.equals("REMOVE_FAVORITE")
                    || name.equals("UNFAVORITE") || name.equals("FAVORITE_OFF"))) return action;
        }
        return null;
    }
}

