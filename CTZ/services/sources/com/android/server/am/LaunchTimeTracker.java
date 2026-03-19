package com.android.server.am;

import android.os.SystemClock;
import android.os.Trace;
import android.util.SparseArray;

class LaunchTimeTracker {
    private final SparseArray<Entry> mWindowingModeLaunchTime = new SparseArray<>();

    LaunchTimeTracker() {
    }

    void setLaunchTime(ActivityRecord activityRecord) {
        Entry entry = this.mWindowingModeLaunchTime.get(activityRecord.getWindowingMode());
        if (entry == null) {
            entry = new Entry();
            this.mWindowingModeLaunchTime.append(activityRecord.getWindowingMode(), entry);
        }
        entry.setLaunchTime(activityRecord);
    }

    void stopFullyDrawnTraceIfNeeded(int i) {
        Entry entry = this.mWindowingModeLaunchTime.get(i);
        if (entry == null) {
            return;
        }
        entry.stopFullyDrawnTraceIfNeeded();
    }

    Entry getEntry(int i) {
        return this.mWindowingModeLaunchTime.get(i);
    }

    static class Entry {
        long mFullyDrawnStartTime;
        long mLaunchStartTime;

        Entry() {
        }

        void setLaunchTime(ActivityRecord activityRecord) {
            if (activityRecord.displayStartTime != 0) {
                if (this.mLaunchStartTime == 0) {
                    startLaunchTraces(activityRecord.packageName);
                    long jUptimeMillis = SystemClock.uptimeMillis();
                    this.mFullyDrawnStartTime = jUptimeMillis;
                    this.mLaunchStartTime = jUptimeMillis;
                    return;
                }
                return;
            }
            long jUptimeMillis2 = SystemClock.uptimeMillis();
            activityRecord.displayStartTime = jUptimeMillis2;
            activityRecord.fullyDrawnStartTime = jUptimeMillis2;
            if (this.mLaunchStartTime == 0) {
                startLaunchTraces(activityRecord.packageName);
                long j = activityRecord.displayStartTime;
                this.mFullyDrawnStartTime = j;
                this.mLaunchStartTime = j;
            }
        }

        private void startLaunchTraces(String str) {
            if (this.mFullyDrawnStartTime != 0) {
                Trace.asyncTraceEnd(64L, "drawing", 0);
            }
            Trace.asyncTraceBegin(64L, "launching: " + str, 0);
            Trace.asyncTraceBegin(64L, "drawing", 0);
        }

        private void stopFullyDrawnTraceIfNeeded() {
            if (this.mFullyDrawnStartTime != 0 && this.mLaunchStartTime == 0) {
                Trace.asyncTraceEnd(64L, "drawing", 0);
                this.mFullyDrawnStartTime = 0L;
            }
        }
    }
}
