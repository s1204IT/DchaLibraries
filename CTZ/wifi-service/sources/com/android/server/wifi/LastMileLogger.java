package com.android.server.wifi;

import android.os.FileUtils;
import com.android.internal.annotations.VisibleForTesting;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import libcore.io.IoUtils;

public class LastMileLogger {
    private static final String TAG = "LastMileLogger";
    private static final String WIFI_EVENT_BUFFER_PATH = "/sys/kernel/debug/tracing/instances/wifi/trace";
    private static final String WIFI_EVENT_ENABLE_PATH = "/sys/kernel/debug/tracing/instances/wifi/tracing_on";
    private static final String WIFI_EVENT_RELEASE_PATH = "/sys/kernel/debug/tracing/instances/wifi/free_buffer";
    private final String mEventBufferPath;
    private final String mEventEnablePath;
    private final String mEventReleasePath;
    private byte[] mLastMileLogForLastFailure;
    private FileInputStream mLastMileTraceHandle;
    private WifiLog mLog;
    private long mPendingConnectionId;

    public LastMileLogger(WifiInjector wifiInjector) {
        this(wifiInjector, WIFI_EVENT_BUFFER_PATH, WIFI_EVENT_ENABLE_PATH, WIFI_EVENT_RELEASE_PATH);
    }

    @VisibleForTesting
    public LastMileLogger(WifiInjector wifiInjector, String str, String str2, String str3) {
        this.mPendingConnectionId = -1L;
        this.mLog = wifiInjector.makeLog(TAG);
        this.mEventBufferPath = str;
        this.mEventEnablePath = str2;
        this.mEventReleasePath = str3;
    }

    public void reportConnectionEvent(long j, byte b) {
        if (j < 0) {
            this.mLog.warn("Ignoring negative connection id: %").c(j).flush();
        }
        switch (b) {
            case 0:
                this.mPendingConnectionId = j;
                enableTracing();
                break;
            case 1:
                this.mPendingConnectionId = -1L;
                disableTracing();
                break;
            case 2:
                if (j >= this.mPendingConnectionId) {
                    this.mPendingConnectionId = -1L;
                    disableTracing();
                    this.mLastMileLogForLastFailure = readTrace();
                }
                break;
        }
    }

    public void dump(PrintWriter printWriter) {
        dumpInternal(printWriter, "Last failed last-mile log", this.mLastMileLogForLastFailure);
        dumpInternal(printWriter, "Latest last-mile log", readTrace());
        this.mLastMileLogForLastFailure = null;
    }

    private void enableTracing() {
        if (!ensureFailSafeIsArmed()) {
            this.mLog.wC("Failed to arm fail-safe.");
            return;
        }
        try {
            FileUtils.stringToFile(this.mEventEnablePath, "1");
        } catch (IOException e) {
            this.mLog.warn("Failed to start event tracing: %").r(e.getMessage()).flush();
        }
    }

    private void disableTracing() {
        try {
            FileUtils.stringToFile(this.mEventEnablePath, "0");
        } catch (IOException e) {
            this.mLog.warn("Failed to stop event tracing: %").r(e.getMessage()).flush();
        }
    }

    private byte[] readTrace() {
        try {
            return IoUtils.readFileAsByteArray(this.mEventBufferPath);
        } catch (IOException e) {
            this.mLog.warn("Failed to read event trace: %").r(e.getMessage()).flush();
            return new byte[0];
        }
    }

    private boolean ensureFailSafeIsArmed() {
        if (this.mLastMileTraceHandle != null) {
            return true;
        }
        try {
            this.mLastMileTraceHandle = new FileInputStream(this.mEventReleasePath);
            return true;
        } catch (IOException e) {
            this.mLog.warn("Failed to open free_buffer pseudo-file: %").r(e.getMessage()).flush();
            return false;
        }
    }

    private static void dumpInternal(PrintWriter printWriter, String str, byte[] bArr) {
        if (bArr == null || bArr.length < 1) {
            printWriter.format("No last mile log for \"%s\"\n", str);
            return;
        }
        printWriter.format("-------------------------- %s ---------------------------\n", str);
        printWriter.print(new String(bArr));
        printWriter.println("--------------------------------------------------------------------");
    }
}
