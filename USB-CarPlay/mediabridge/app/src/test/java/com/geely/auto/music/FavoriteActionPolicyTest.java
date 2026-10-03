package com.geely.auto.music;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FavoriteActionPolicyTest {
    @Test public void selectsTheActionMatchingTheRequestedFavoriteState() {
        assertEquals("player.ADD_LIKE", FavoriteActionPolicy.select(Arrays.asList(
                "player.REMOVE_LIKE", "player.ADD_LIKE"), true));
        assertEquals("player.REMOVE_LIKE", FavoriteActionPolicy.select(Arrays.asList(
                "player.REMOVE_LIKE", "player.ADD_LIKE"), false));
    }

    @Test public void unverifiedToggleDislikeAndOppositeActionsStayUnsupported() {
        assertNull(FavoriteActionPolicy.select(
                Collections.singletonList("player.TOGGLE_FAV"), true));
        assertNull(FavoriteActionPolicy.select(
                Collections.singletonList("player.TOGGLE_FAV"), false));
        assertNull(FavoriteActionPolicy.select(Collections.singletonList("player.DISLIKE"), false));
        assertNull(FavoriteActionPolicy.select(Collections.singletonList("player.ADD_LIKE"), false));
    }
    @Test public void bareLikeIsNeverUsedToUnlike() {
        assertNull(FavoriteActionPolicy.select(Collections.singletonList("LIKE"), false));
        assertEquals("LIKE", FavoriteActionPolicy.select(Collections.singletonList("LIKE"), true));
    }
    @Test public void substringCollisionDoesNotAdvertiseFavorite() {
        assertNull(FavoriteActionPolicy.select(Arrays.asList("PLAYER_LIKELIHOOD", "HEARTBEAT", "DOWNLOAD_FAVORITES"), true));
    }
}
