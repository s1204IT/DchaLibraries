package com.android.systemui.shared.recents.utilities;

import android.os.Trace;
import android.support.v4.media.session.PlaybackStateCompat;

/* loaded from: classes.dex */
public class AppTrace {
    public static void start(String key, int cookie) {
        Trace.asyncTraceBegin(PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM, key, cookie);
    }

    public static void start(String key) {
        Trace.asyncTraceBegin(PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM, key, 0);
    }

    public static void end(String key) {
        Trace.asyncTraceEnd(PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM, key, 0);
    }

    public static void end(String key, int cookie) {
        Trace.asyncTraceEnd(PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM, key, cookie);
    }

    public static void beginSection(String key) {
        Trace.beginSection(key);
    }

    public static void endSection() {
        Trace.endSection();
    }

    public static void count(String name, int count) {
        Trace.traceCounter(PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM, name, count);
    }
}
