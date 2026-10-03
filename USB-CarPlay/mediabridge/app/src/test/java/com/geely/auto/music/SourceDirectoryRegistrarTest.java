package com.geely.auto.music;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
import static com.geely.auto.music.SourceDirectoryRegistrar.Status.*;

public class SourceDirectoryRegistrarTest {
    public static final class Source {
        final String pkg, name;
        Source(String pkg, String name) { this.pkg = pkg; this.name = name; }
        public String getPackageName() { return pkg; }
        public String getAppName() { return name; }
        public String getIconPath() { return "android.resource://" + pkg + "/123"; }
        public int getPriorityLevel() { return 1; }
        public int[] getSourceTypeList() { return new int[]{6}; }
    }
    private static final Source OWN = new Source("com.mediabridge.app", "MediaBridge");
    private static final class Directory implements SourceDirectoryRegistrar.Directory {
        List<?> sources; int writes; boolean retain = true;
        Directory(Object... entries) { sources = Arrays.asList(entries); }
        public List<?> read() { return sources; }
        public void write(List<Object> value) { writes++; if (retain) sources = value; }
        public Object ownSource() { return OWN; }
    }
    @Test public void addsIndependentIdentityWithoutReplacingNeteaseOrChangingFactoryObjects() throws Exception {
        Source netease = new Source("com.netease.cloudmusic.iot", "网易云音乐");
        Source radio = new Source("factory.radio", "收音机");
        Directory d = new Directory(netease, radio);
        assertEquals(ADDED, SourceDirectoryRegistrar.mergeAndVerify(d, OWN.pkg, () -> false).status);
        assertEquals(3, d.sources.size()); assertSame(netease, d.sources.get(0)); assertSame(radio, d.sources.get(1));
        assertSame(OWN, d.sources.get(2));
        assertEquals(PRESENT, SourceDirectoryRegistrar.mergeAndVerify(d, OWN.pkg, () -> false).status);
        assertEquals(1, d.writes);
    }
    @Test public void replacesAndDeduplicatesOnlyOurEntry() throws Exception {
        Source factory = new Source("factory.music", "原车音乐");
        Directory d = new Directory(new Source(OWN.pkg, "old"), factory, new Source(OWN.pkg, "old"));
        assertEquals(UPDATED, SourceDirectoryRegistrar.mergeAndVerify(d, OWN.pkg, () -> false).status);
        assertEquals(Arrays.asList(OWN, factory), d.sources);
    }
    @Test public void unavailableDirectoryAndCancelledConnectionNeverWrite() throws Exception {
        Directory empty = new Directory();
        assertEquals(NOT_READY, SourceDirectoryRegistrar.mergeAndVerify(empty, OWN.pkg, () -> false).status);
        assertEquals(0, empty.writes);
        Directory cancelled = new Directory(new Source("factory", "Factory"));
        assertEquals(CANCELLED, SourceDirectoryRegistrar.mergeAndVerify(cancelled, OWN.pkg, () -> true).status);
        assertEquals(0, cancelled.writes);
    }
    @Test public void rejectedWriteIsNotReportedAsSuccess() throws Exception {
        Directory d = new Directory(new Source("factory", "Factory")); d.retain = false;
        assertEquals(UNCONFIRMED, SourceDirectoryRegistrar.mergeAndVerify(d, OWN.pkg, () -> false).status);
    }
    @Test public void cancellationAfterReadPreventsLateOldConnectionWrite() throws Exception {
        Directory d = new Directory(new Source("factory", "Factory"));
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        SourceDirectoryRegistrar.mergeAndVerify(d, OWN.pkg, () -> calls.incrementAndGet() > 1);
        assertEquals(0, d.writes);
    }
}
