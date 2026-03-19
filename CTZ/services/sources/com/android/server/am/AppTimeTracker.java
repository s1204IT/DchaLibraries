package com.android.server.am;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.MutableLong;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoUtils;
import java.io.PrintWriter;

public class AppTimeTracker {
    private final ArrayMap<String, MutableLong> mPackageTimes = new ArrayMap<>();
    private final PendingIntent mReceiver;
    private String mStartedPackage;
    private MutableLong mStartedPackageTime;
    private long mStartedTime;
    private long mTotalTime;

    public AppTimeTracker(PendingIntent pendingIntent) {
        this.mReceiver = pendingIntent;
    }

    public void start(String str) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        if (this.mStartedTime == 0) {
            this.mStartedTime = jElapsedRealtime;
        }
        if (!str.equals(this.mStartedPackage)) {
            if (this.mStartedPackageTime != null) {
                long j = jElapsedRealtime - this.mStartedTime;
                this.mStartedPackageTime.value += j;
                this.mTotalTime += j;
            }
            this.mStartedPackage = str;
            this.mStartedPackageTime = this.mPackageTimes.get(str);
            if (this.mStartedPackageTime == null) {
                this.mStartedPackageTime = new MutableLong(0L);
                this.mPackageTimes.put(str, this.mStartedPackageTime);
            }
        }
    }

    public void stop() {
        if (this.mStartedTime != 0) {
            long jElapsedRealtime = SystemClock.elapsedRealtime() - this.mStartedTime;
            this.mTotalTime += jElapsedRealtime;
            if (this.mStartedPackageTime != null) {
                this.mStartedPackageTime.value += jElapsedRealtime;
            }
            this.mStartedPackage = null;
            this.mStartedPackageTime = null;
        }
    }

    public void deliverResult(Context context) {
        stop();
        Bundle bundle = new Bundle();
        bundle.putLong("android.activity.usage_time", this.mTotalTime);
        Bundle bundle2 = new Bundle();
        for (int size = this.mPackageTimes.size() - 1; size >= 0; size--) {
            bundle2.putLong(this.mPackageTimes.keyAt(size), this.mPackageTimes.valueAt(size).value);
        }
        bundle.putBundle("android.usage_time_packages", bundle2);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        try {
            this.mReceiver.send(context, 0, intent);
        } catch (PendingIntent.CanceledException e) {
        }
    }

    public void dumpWithHeader(PrintWriter printWriter, String str, boolean z) {
        printWriter.print(str);
        printWriter.print("AppTimeTracker #");
        printWriter.print(Integer.toHexString(System.identityHashCode(this)));
        printWriter.println(":");
        dump(printWriter, str + "  ", z);
    }

    public void dump(PrintWriter printWriter, String str, boolean z) {
        printWriter.print(str);
        printWriter.print("mReceiver=");
        printWriter.println(this.mReceiver);
        printWriter.print(str);
        printWriter.print("mTotalTime=");
        TimeUtils.formatDuration(this.mTotalTime, printWriter);
        printWriter.println();
        for (int i = 0; i < this.mPackageTimes.size(); i++) {
            printWriter.print(str);
            printWriter.print("mPackageTime:");
            printWriter.print(this.mPackageTimes.keyAt(i));
            printWriter.print("=");
            TimeUtils.formatDuration(this.mPackageTimes.valueAt(i).value, printWriter);
            printWriter.println();
        }
        if (z && this.mStartedTime != 0) {
            printWriter.print(str);
            printWriter.print("mStartedTime=");
            TimeUtils.formatDuration(SystemClock.elapsedRealtime(), this.mStartedTime, printWriter);
            printWriter.println();
            printWriter.print(str);
            printWriter.print("mStartedPackage=");
            printWriter.println(this.mStartedPackage);
        }
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j, boolean z) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1138166333441L, this.mReceiver.toString());
        protoOutputStream.write(1112396529666L, this.mTotalTime);
        for (int i = 0; i < this.mPackageTimes.size(); i++) {
            long jStart2 = protoOutputStream.start(2246267895811L);
            protoOutputStream.write(1138166333441L, this.mPackageTimes.keyAt(i));
            protoOutputStream.write(1112396529666L, this.mPackageTimes.valueAt(i).value);
            protoOutputStream.end(jStart2);
        }
        if (z && this.mStartedTime != 0) {
            ProtoUtils.toDuration(protoOutputStream, 1146756268036L, this.mStartedTime, SystemClock.elapsedRealtime());
            protoOutputStream.write(1138166333445L, this.mStartedPackage);
        }
        protoOutputStream.end(jStart);
    }
}
