package com.geely.auto.music;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

public class BridgeIoFenceTest {
    @Test public void discardedPendingRequestsStillWaitForInFlightRequestBeforeRelease() throws Exception {
        BridgeIo.Lane lane = new BridgeIo.Lane(error -> { throw new AssertionError(error); });
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), fenced = new CountDownLatch(1);
        AtomicBoolean stale = new AtomicBoolean(false);
        lane.execute("in-flight", () -> { entered.countDown(); try { release.await(2,TimeUnit.SECONDS); } catch(InterruptedException e) { Thread.currentThread().interrupt(); } });
        assertTrue(entered.await(1,TimeUnit.SECONDS));
        lane.execute("old-output", () -> stale.set(true));
        lane.discardPending();
        lane.execute("release-fence", fenced::countDown);
        assertFalse(fenced.await(50,TimeUnit.MILLISECONDS));
        release.countDown();
        assertTrue(fenced.await(1,TimeUnit.SECONDS));
        assertFalse(stale.get());
    }
}
