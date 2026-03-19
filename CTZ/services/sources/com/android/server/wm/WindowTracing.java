package com.android.server.wm;

import android.content.Context;
import android.os.Build;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class WindowTracing {
    private static final long MAGIC_NUMBER_VALUE = 4990904633914181975L;
    private static final String TAG = "WindowTracing";
    private boolean mEnabled;
    private volatile boolean mEnabledLockFree;
    private final File mTraceFile;
    private final Object mLock = new Object();
    private final BlockingQueue<ProtoOutputStream> mWriteQueue = new ArrayBlockingQueue(200);

    WindowTracing(File file) {
        this.mTraceFile = file;
    }

    void startTrace(PrintWriter printWriter) throws IOException {
        if (Build.IS_USER) {
            logAndPrintln(printWriter, "Error: Tracing is not supported on user builds.");
            return;
        }
        synchronized (this.mLock) {
            logAndPrintln(printWriter, "Start tracing to " + this.mTraceFile + ".");
            this.mWriteQueue.clear();
            this.mTraceFile.delete();
            FileOutputStream fileOutputStream = new FileOutputStream(this.mTraceFile);
            Throwable th = null;
            try {
                this.mTraceFile.setReadable(true, false);
                ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileOutputStream);
                protoOutputStream.write(1125281431553L, MAGIC_NUMBER_VALUE);
                protoOutputStream.flush();
                $closeResource(null, fileOutputStream);
                this.mEnabledLockFree = true;
                this.mEnabled = true;
            } catch (Throwable th2) {
                $closeResource(th, fileOutputStream);
                throw th2;
            }
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private void logAndPrintln(PrintWriter printWriter, String str) {
        Log.i(TAG, str);
        if (printWriter != null) {
            printWriter.println(str);
            printWriter.flush();
        }
    }

    void stopTrace(PrintWriter printWriter) {
        if (Build.IS_USER) {
            logAndPrintln(printWriter, "Error: Tracing is not supported on user builds.");
            return;
        }
        synchronized (this.mLock) {
            logAndPrintln(printWriter, "Stop tracing to " + this.mTraceFile + ". Waiting for traces to flush.");
            this.mEnabledLockFree = false;
            this.mEnabled = false;
            while (!this.mWriteQueue.isEmpty()) {
                if (this.mEnabled) {
                    logAndPrintln(printWriter, "ERROR: tracing was re-enabled while waiting for flush.");
                    throw new IllegalStateException("tracing enabled while waiting for flush.");
                }
                try {
                    this.mLock.wait();
                    this.mLock.notify();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            logAndPrintln(printWriter, "Trace written to " + this.mTraceFile + ".");
        }
    }

    void appendTraceEntry(ProtoOutputStream protoOutputStream) {
        if (this.mEnabledLockFree && !this.mWriteQueue.offer(protoOutputStream)) {
            Log.e(TAG, "Dropping window trace entry, queue full");
        }
    }

    void loop() {
        while (true) {
            loopOnce();
        }
    }

    @VisibleForTesting
    void loopOnce() {
        FileOutputStream fileOutputStream;
        Throwable th;
        try {
            ProtoOutputStream protoOutputStreamTake = this.mWriteQueue.take();
            synchronized (this.mLock) {
                try {
                    try {
                        Trace.traceBegin(32L, "writeToFile");
                        fileOutputStream = new FileOutputStream(this.mTraceFile, true);
                        th = null;
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to write file " + this.mTraceFile, e);
                    }
                    try {
                        try {
                            fileOutputStream.write(protoOutputStreamTake.getBytes());
                            this.mLock.notify();
                        } finally {
                        }
                    } finally {
                        $closeResource(th, fileOutputStream);
                    }
                } finally {
                    Trace.traceEnd(32L);
                }
            }
        } catch (InterruptedException e2) {
            Thread.currentThread().interrupt();
        }
    }

    boolean isEnabled() {
        return this.mEnabledLockFree;
    }

    static WindowTracing createDefaultAndStartLooper(Context context) {
        final WindowTracing windowTracing = new WindowTracing(new File("/data/misc/wmtrace/wm_trace.pb"));
        if (!Build.IS_USER) {
            Objects.requireNonNull(windowTracing);
            new Thread(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.loop();
                }
            }, "window_tracing").start();
        }
        return windowTracing;
    }

    int onShellCommand(ShellCommand shellCommand, String str) {
        byte b;
        PrintWriter outPrintWriter = shellCommand.getOutPrintWriter();
        try {
            int iHashCode = str.hashCode();
            if (iHashCode != 3540994) {
                b = (iHashCode == 109757538 && str.equals("start")) ? (byte) 0 : (byte) -1;
            } else if (str.equals("stop")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    startTrace(outPrintWriter);
                    return 0;
                case 1:
                    stopTrace(outPrintWriter);
                    return 0;
                default:
                    outPrintWriter.println("Unknown command: " + str);
                    return -1;
            }
        } catch (IOException e) {
            logAndPrintln(outPrintWriter, e.toString());
            throw new RuntimeException(e);
        }
    }

    void traceStateLocked(String str, WindowManagerService windowManagerService) {
        if (!isEnabled()) {
            return;
        }
        ProtoOutputStream protoOutputStream = new ProtoOutputStream();
        long jStart = protoOutputStream.start(2246267895810L);
        protoOutputStream.write(1125281431553L, SystemClock.elapsedRealtimeNanos());
        protoOutputStream.write(1138166333442L, str);
        Trace.traceBegin(32L, "writeToProtoLocked");
        try {
            long jStart2 = protoOutputStream.start(1146756268035L);
            windowManagerService.writeToProtoLocked(protoOutputStream, true);
            protoOutputStream.end(jStart2);
            Trace.traceEnd(32L);
            protoOutputStream.end(jStart);
            appendTraceEntry(protoOutputStream);
        } finally {
            Trace.traceEnd(32L);
        }
    }
}
