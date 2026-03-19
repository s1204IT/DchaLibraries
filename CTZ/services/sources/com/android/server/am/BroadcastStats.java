package com.android.server.am;

import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.TimeUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class BroadcastStats {
    static final Comparator<ActionEntry> ACTIONS_COMPARATOR = new Comparator<ActionEntry>() {
        @Override
        public int compare(ActionEntry actionEntry, ActionEntry actionEntry2) {
            if (actionEntry.mTotalDispatchTime < actionEntry2.mTotalDispatchTime) {
                return -1;
            }
            if (actionEntry.mTotalDispatchTime > actionEntry2.mTotalDispatchTime) {
                return 1;
            }
            return 0;
        }
    };
    long mEndRealtime;
    long mEndUptime;
    final ArrayMap<String, ActionEntry> mActions = new ArrayMap<>();
    final long mStartRealtime = SystemClock.elapsedRealtime();
    final long mStartUptime = SystemClock.uptimeMillis();

    static final class ActionEntry {
        final String mAction;
        long mMaxDispatchTime;
        int mReceiveCount;
        int mSkipCount;
        long mTotalDispatchTime;
        final ArrayMap<String, PackageEntry> mPackages = new ArrayMap<>();
        final ArrayMap<String, ViolationEntry> mBackgroundCheckViolations = new ArrayMap<>();

        ActionEntry(String str) {
            this.mAction = str;
        }
    }

    static final class PackageEntry {
        int mSendCount;

        PackageEntry() {
        }
    }

    static final class ViolationEntry {
        int mCount;

        ViolationEntry() {
        }
    }

    public void addBroadcast(String str, String str2, int i, int i2, long j) {
        ActionEntry actionEntry = this.mActions.get(str);
        if (actionEntry == null) {
            actionEntry = new ActionEntry(str);
            this.mActions.put(str, actionEntry);
        }
        actionEntry.mReceiveCount += i;
        actionEntry.mSkipCount += i2;
        actionEntry.mTotalDispatchTime += j;
        if (actionEntry.mMaxDispatchTime < j) {
            actionEntry.mMaxDispatchTime = j;
        }
        PackageEntry packageEntry = actionEntry.mPackages.get(str2);
        if (packageEntry == null) {
            packageEntry = new PackageEntry();
            actionEntry.mPackages.put(str2, packageEntry);
        }
        packageEntry.mSendCount++;
    }

    public void addBackgroundCheckViolation(String str, String str2) {
        ActionEntry actionEntry = this.mActions.get(str);
        if (actionEntry == null) {
            actionEntry = new ActionEntry(str);
            this.mActions.put(str, actionEntry);
        }
        ViolationEntry violationEntry = actionEntry.mBackgroundCheckViolations.get(str2);
        if (violationEntry == null) {
            violationEntry = new ViolationEntry();
            actionEntry.mBackgroundCheckViolations.put(str2, violationEntry);
        }
        violationEntry.mCount++;
    }

    public boolean dumpStats(PrintWriter printWriter, String str, String str2) {
        ArrayList arrayList = new ArrayList(this.mActions.size());
        for (int size = this.mActions.size() - 1; size >= 0; size--) {
            arrayList.add(this.mActions.valueAt(size));
        }
        Collections.sort(arrayList, ACTIONS_COMPARATOR);
        boolean z = false;
        for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
            ActionEntry actionEntry = (ActionEntry) arrayList.get(size2);
            if (str2 == null || actionEntry.mPackages.containsKey(str2)) {
                printWriter.print(str);
                printWriter.print(actionEntry.mAction);
                printWriter.println(":");
                printWriter.print(str);
                printWriter.print("  Number received: ");
                printWriter.print(actionEntry.mReceiveCount);
                printWriter.print(", skipped: ");
                printWriter.println(actionEntry.mSkipCount);
                printWriter.print(str);
                printWriter.print("  Total dispatch time: ");
                TimeUtils.formatDuration(actionEntry.mTotalDispatchTime, printWriter);
                printWriter.print(", max: ");
                TimeUtils.formatDuration(actionEntry.mMaxDispatchTime, printWriter);
                printWriter.println();
                for (int size3 = actionEntry.mPackages.size() - 1; size3 >= 0; size3--) {
                    printWriter.print(str);
                    printWriter.print("  Package ");
                    printWriter.print(actionEntry.mPackages.keyAt(size3));
                    printWriter.print(": ");
                    printWriter.print(actionEntry.mPackages.valueAt(size3).mSendCount);
                    printWriter.println(" times");
                }
                for (int size4 = actionEntry.mBackgroundCheckViolations.size() - 1; size4 >= 0; size4--) {
                    printWriter.print(str);
                    printWriter.print("  Bg Check Violation ");
                    printWriter.print(actionEntry.mBackgroundCheckViolations.keyAt(size4));
                    printWriter.print(": ");
                    printWriter.print(actionEntry.mBackgroundCheckViolations.valueAt(size4).mCount);
                    printWriter.println(" times");
                }
                z = true;
            }
        }
        return z;
    }

    public void dumpCheckinStats(PrintWriter printWriter, String str) {
        printWriter.print("broadcast-stats,1,");
        printWriter.print(this.mStartRealtime);
        printWriter.print(",");
        printWriter.print(this.mEndRealtime == 0 ? SystemClock.elapsedRealtime() : this.mEndRealtime);
        printWriter.print(",");
        printWriter.println((this.mEndUptime == 0 ? SystemClock.uptimeMillis() : this.mEndUptime) - this.mStartUptime);
        for (int size = this.mActions.size() - 1; size >= 0; size--) {
            ActionEntry actionEntryValueAt = this.mActions.valueAt(size);
            if (str == null || actionEntryValueAt.mPackages.containsKey(str)) {
                printWriter.print("a,");
                printWriter.print(this.mActions.keyAt(size));
                printWriter.print(",");
                printWriter.print(actionEntryValueAt.mReceiveCount);
                printWriter.print(",");
                printWriter.print(actionEntryValueAt.mSkipCount);
                printWriter.print(",");
                printWriter.print(actionEntryValueAt.mTotalDispatchTime);
                printWriter.print(",");
                printWriter.print(actionEntryValueAt.mMaxDispatchTime);
                printWriter.println();
                for (int size2 = actionEntryValueAt.mPackages.size() - 1; size2 >= 0; size2--) {
                    printWriter.print("p,");
                    printWriter.print(actionEntryValueAt.mPackages.keyAt(size2));
                    PackageEntry packageEntryValueAt = actionEntryValueAt.mPackages.valueAt(size2);
                    printWriter.print(",");
                    printWriter.print(packageEntryValueAt.mSendCount);
                    printWriter.println();
                }
                for (int size3 = actionEntryValueAt.mBackgroundCheckViolations.size() - 1; size3 >= 0; size3--) {
                    printWriter.print("v,");
                    printWriter.print(actionEntryValueAt.mBackgroundCheckViolations.keyAt(size3));
                    ViolationEntry violationEntryValueAt = actionEntryValueAt.mBackgroundCheckViolations.valueAt(size3);
                    printWriter.print(",");
                    printWriter.print(violationEntryValueAt.mCount);
                    printWriter.println();
                }
            }
        }
    }
}
