package com.android.ims.internal;

import android.telecom.Log;
import android.util.ArraySet;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VideoPauseTracker {
    public static final int SOURCE_DATA_ENABLED = 2;
    private static final String SOURCE_DATA_ENABLED_STR = "DATA_ENABLED";
    public static final int SOURCE_INCALL = 1;
    private static final String SOURCE_INCALL_STR = "INCALL";
    private Set<Integer> mPauseRequests = new ArraySet(2);
    private Object mPauseRequestsLock = new Object();

    public boolean shouldPauseVideoFor(int i) {
        synchronized (this.mPauseRequestsLock) {
            boolean zIsPaused = isPaused();
            this.mPauseRequests.add(Integer.valueOf(i));
            if (!zIsPaused) {
                Log.i(this, "shouldPauseVideoFor: source=%s, pendingRequests=%s - should pause", new Object[]{sourceToString(i), sourcesToString(this.mPauseRequests)});
                return true;
            }
            Log.i(this, "shouldPauseVideoFor: source=%s, pendingRequests=%s - already paused", new Object[]{sourceToString(i), sourcesToString(this.mPauseRequests)});
            return false;
        }
    }

    public boolean shouldResumeVideoFor(int i) {
        synchronized (this.mPauseRequestsLock) {
            boolean zIsPaused = isPaused();
            this.mPauseRequests.remove(Integer.valueOf(i));
            boolean zIsPaused2 = isPaused();
            if (zIsPaused && !zIsPaused2) {
                Log.i(this, "shouldResumeVideoFor: source=%s, pendingRequests=%s - should resume", new Object[]{sourceToString(i), sourcesToString(this.mPauseRequests)});
                return true;
            }
            if (zIsPaused && zIsPaused2) {
                Log.i(this, "shouldResumeVideoFor: source=%s, pendingRequests=%s - stay paused", new Object[]{sourceToString(i), sourcesToString(this.mPauseRequests)});
                return false;
            }
            Log.i(this, "shouldResumeVideoFor: source=%s, pendingRequests=%s - not paused", new Object[]{sourceToString(i), sourcesToString(this.mPauseRequests)});
            return true;
        }
    }

    public boolean isPaused() {
        boolean z;
        synchronized (this.mPauseRequestsLock) {
            z = !this.mPauseRequests.isEmpty();
        }
        return z;
    }

    public boolean wasVideoPausedFromSource(int i) {
        boolean zContains;
        synchronized (this.mPauseRequestsLock) {
            zContains = this.mPauseRequests.contains(Integer.valueOf(i));
        }
        return zContains;
    }

    public void clearPauseRequests() {
        synchronized (this.mPauseRequestsLock) {
            this.mPauseRequests.clear();
        }
    }

    private String sourceToString(int i) {
        switch (i) {
            case 1:
                return SOURCE_INCALL_STR;
            case 2:
                return SOURCE_DATA_ENABLED_STR;
            default:
                return "unknown";
        }
    }

    private String sourcesToString(Collection<Integer> collection) {
        String str;
        synchronized (this.mPauseRequestsLock) {
            str = (String) collection.stream().map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return this.f$0.sourceToString(((Integer) obj).intValue());
                }
            }).collect(Collectors.joining(", "));
        }
        return str;
    }
}
