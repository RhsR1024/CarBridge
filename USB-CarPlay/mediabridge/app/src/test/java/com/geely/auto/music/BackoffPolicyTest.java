package com.geely.auto.music;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BackoffPolicyTest {
    @Test public void followsSpecAndCapsAtSixtySeconds() {
        BackoffPolicy policy = new BackoffPolicy();
        assertEquals(2000L, policy.delayMs(1));
        assertEquals(5000L, policy.delayMs(2));
        assertEquals(10000L, policy.delayMs(3));
        assertEquals(20000L, policy.delayMs(4));
        assertEquals(30000L, policy.delayMs(5));
        assertEquals(60000L, policy.delayMs(6));
        assertEquals(60000L, policy.delayMs(1000));
    }
}
