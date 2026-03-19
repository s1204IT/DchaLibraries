package com.android.server;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.LocalLog;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.Watchdog;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.power.ShutdownThread;
import com.google.android.collect.Lists;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class NativeDaemonConnector implements Runnable, Handler.Callback, Watchdog.Monitor {
    private static final long DEFAULT_TIMEOUT = 60000;
    private static final boolean VDBG = false;
    private static final long WARN_EXECUTE_DELAY_MS = 500;
    private final int BUFFER_SIZE;
    private final String TAG;
    private Handler mCallbackHandler;
    private INativeDaemonConnectorCallbacks mCallbacks;
    private final Object mDaemonLock;
    private volatile boolean mDebug;
    private LocalLog mLocalLog;
    private final Looper mLooper;
    private OutputStream mOutputStream;
    private final ResponseQueue mResponseQueue;
    private AtomicInteger mSequenceNumber;
    private String mSocket;
    private final PowerManager.WakeLock mWakeLock;
    private volatile Object mWarnIfHeld;

    NativeDaemonConnector(INativeDaemonConnectorCallbacks iNativeDaemonConnectorCallbacks, String str, int i, String str2, int i2, PowerManager.WakeLock wakeLock) {
        this(iNativeDaemonConnectorCallbacks, str, i, str2, i2, wakeLock, FgThread.get().getLooper());
    }

    NativeDaemonConnector(INativeDaemonConnectorCallbacks iNativeDaemonConnectorCallbacks, String str, int i, String str2, int i2, PowerManager.WakeLock wakeLock, Looper looper) {
        this.mDebug = false;
        this.mDaemonLock = new Object();
        this.BUFFER_SIZE = 4096;
        this.mCallbacks = iNativeDaemonConnectorCallbacks;
        this.mSocket = str;
        this.mResponseQueue = new ResponseQueue(i);
        this.mWakeLock = wakeLock;
        if (this.mWakeLock != null) {
            this.mWakeLock.setReferenceCounted(true);
        }
        this.mLooper = looper;
        this.mSequenceNumber = new AtomicInteger(0);
        this.TAG = str2 == null ? "NativeDaemonConnector" : str2;
        this.mLocalLog = new LocalLog(i2);
    }

    public void setDebug(boolean z) {
        this.mDebug = z;
    }

    private int uptimeMillisInt() {
        return ((int) SystemClock.uptimeMillis()) & Integer.MAX_VALUE;
    }

    public void setWarnIfHeld(Object obj) {
        Preconditions.checkState(this.mWarnIfHeld == null);
        this.mWarnIfHeld = Preconditions.checkNotNull(obj);
    }

    @Override
    public void run() throws Throwable {
        this.mCallbackHandler = new Handler(this.mLooper, this);
        while (!isShuttingDown()) {
            try {
                listenToSocket();
            } catch (Exception e) {
                loge("Error in NativeDaemonConnector: " + e);
                if (!isShuttingDown()) {
                    SystemClock.sleep(5000L);
                } else {
                    return;
                }
            }
        }
    }

    private static boolean isShuttingDown() {
        String str = SystemProperties.get(ShutdownThread.SHUTDOWN_ACTION_PROPERTY, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        return str != null && str.length() > 0;
    }

    @Override
    public boolean handleMessage(Message message) {
        String str;
        Object[] objArr;
        int iUptimeMillisInt;
        String str2 = (String) message.obj;
        int iUptimeMillisInt2 = uptimeMillisInt();
        int i = message.arg1;
        try {
            try {
                if (!this.mCallbacks.onEvent(message.what, str2, NativeDaemonEvent.unescapeArgs(str2))) {
                    log(String.format("Unhandled event '%s'", str2));
                }
                if (this.mCallbacks.onCheckHoldWakeLock(message.what) && this.mWakeLock != null) {
                    this.mWakeLock.release();
                }
                iUptimeMillisInt = uptimeMillisInt();
                if (iUptimeMillisInt2 > i) {
                    int i2 = iUptimeMillisInt2 - i;
                    if (i2 > 500) {
                        loge(String.format("NDC event {%s} processed too late: %dms", str2, Integer.valueOf(i2)));
                    }
                }
            } catch (Exception e) {
                loge("Error handling '" + str2 + "': " + e);
                if (this.mCallbacks.onCheckHoldWakeLock(message.what) && this.mWakeLock != null) {
                    this.mWakeLock.release();
                }
                int iUptimeMillisInt3 = uptimeMillisInt();
                if (iUptimeMillisInt2 > i) {
                    int i3 = iUptimeMillisInt2 - i;
                    if (i3 > 500) {
                        loge(String.format("NDC event {%s} processed too late: %dms", str2, Integer.valueOf(i3)));
                    }
                }
                if (iUptimeMillisInt3 > iUptimeMillisInt2) {
                    int i4 = iUptimeMillisInt3 - iUptimeMillisInt2;
                    if (i4 > 500) {
                        str = "NDC event {%s} took too long: %dms";
                        objArr = new Object[]{str2, Integer.valueOf(i4)};
                    }
                }
            }
            if (iUptimeMillisInt > iUptimeMillisInt2) {
                int i5 = iUptimeMillisInt - iUptimeMillisInt2;
                if (i5 > 500) {
                    str = "NDC event {%s} took too long: %dms";
                    objArr = new Object[]{str2, Integer.valueOf(i5)};
                    loge(String.format(str, objArr));
                }
            }
            return true;
        } catch (Throwable th) {
            if (this.mCallbacks.onCheckHoldWakeLock(message.what) && this.mWakeLock != null) {
                this.mWakeLock.release();
            }
            int iUptimeMillisInt4 = uptimeMillisInt();
            if (iUptimeMillisInt2 > i) {
                int i6 = iUptimeMillisInt2 - i;
                if (i6 > 500) {
                    loge(String.format("NDC event {%s} processed too late: %dms", str2, Integer.valueOf(i6)));
                }
            }
            if (iUptimeMillisInt4 > iUptimeMillisInt2) {
                int i7 = iUptimeMillisInt4 - iUptimeMillisInt2;
                if (i7 > 500) {
                    loge(String.format("NDC event {%s} took too long: %dms", str2, Integer.valueOf(i7)));
                }
            }
            throw th;
        }
    }

    private LocalSocketAddress determineSocketAddress() {
        if (this.mSocket.startsWith("__test__") && Build.IS_DEBUGGABLE) {
            return new LocalSocketAddress(this.mSocket);
        }
        return new LocalSocketAddress(this.mSocket, LocalSocketAddress.Namespace.RESERVED);
    }

    private void listenToSocket() throws Throwable {
        int i;
        boolean z;
        PowerManager.WakeLock wakeLock;
        LocalSocket localSocket = null;
        try {
            try {
                LocalSocket localSocket2 = new LocalSocket();
                try {
                    localSocket2.connect(determineSocketAddress());
                    InputStream inputStream = localSocket2.getInputStream();
                    synchronized (this.mDaemonLock) {
                        this.mOutputStream = localSocket2.getOutputStream();
                    }
                    this.mCallbacks.onDaemonConnected();
                    byte[] bArr = new byte[4096];
                    boolean z2 = false;
                    int i2 = 0;
                    while (true) {
                        i = inputStream.read(bArr, i2, 4096 - i2);
                        if (i < 0) {
                            break;
                        }
                        FileDescriptor[] ancillaryFileDescriptors = localSocket2.getAncillaryFileDescriptors();
                        int i3 = i + i2;
                        int i4 = 0;
                        for (int i5 = 0; i5 < i3; i5++) {
                            if (bArr[i5] == 0) {
                                try {
                                    NativeDaemonEvent rawEvent = NativeDaemonEvent.parseRawEvent(new String(bArr, i4, i5 - i4, StandardCharsets.UTF_8), ancillaryFileDescriptors);
                                    log("RCV <- {" + rawEvent + "}");
                                    if (rawEvent.isClassUnsolicited()) {
                                        if (!this.mCallbacks.onCheckHoldWakeLock(rawEvent.getCode()) || this.mWakeLock == null) {
                                            z = false;
                                        } else {
                                            this.mWakeLock.acquire();
                                            z = true;
                                        }
                                        try {
                                            try {
                                                if (this.mCallbackHandler.sendMessage(this.mCallbackHandler.obtainMessage(rawEvent.getCode(), uptimeMillisInt(), 0, rawEvent.getRawEvent()))) {
                                                    z = false;
                                                }
                                            } catch (IllegalArgumentException e) {
                                                e = e;
                                                log("Problem parsing message " + e);
                                                if (z) {
                                                    wakeLock = this.mWakeLock;
                                                }
                                                i4 = i5 + 1;
                                            }
                                        } catch (Throwable th) {
                                            th = th;
                                            z2 = z;
                                            if (z2) {
                                                this.mWakeLock.release();
                                            }
                                            throw th;
                                        }
                                    } else {
                                        this.mResponseQueue.add(rawEvent.getCmdNumber(), rawEvent);
                                        z = false;
                                    }
                                } catch (IllegalArgumentException e2) {
                                    e = e2;
                                    z = false;
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                                if (z) {
                                    wakeLock = this.mWakeLock;
                                    wakeLock.release();
                                }
                                i4 = i5 + 1;
                            }
                        }
                        if (i4 == 0) {
                            log("RCV incomplete");
                        }
                        if (i4 != i3) {
                            i2 = 4096 - i4;
                            System.arraycopy(bArr, i4, bArr, 0, i2);
                        } else {
                            i2 = 0;
                        }
                    }
                    loge("got " + i + " reading with start = " + i2);
                    synchronized (this.mDaemonLock) {
                        if (this.mOutputStream != null) {
                            try {
                                loge("closing stream for " + this.mSocket);
                                this.mOutputStream.close();
                            } catch (IOException e3) {
                                loge("Failed closing output stream: " + e3);
                            }
                            this.mOutputStream = null;
                        }
                    }
                    try {
                        localSocket2.close();
                    } catch (IOException e4) {
                        loge("Failed closing socket: " + e4);
                    }
                } catch (IOException e5) {
                    e = e5;
                    loge("Communications error: " + e);
                    throw e;
                }
            } catch (Throwable th3) {
                th = th3;
                synchronized (this.mDaemonLock) {
                    if (this.mOutputStream != null) {
                        try {
                            loge("closing stream for " + this.mSocket);
                            this.mOutputStream.close();
                        } catch (IOException e6) {
                            loge("Failed closing output stream: " + e6);
                        }
                        this.mOutputStream = null;
                    }
                }
                if (0 == 0) {
                    throw th;
                }
                try {
                    localSocket.close();
                    throw th;
                } catch (IOException e7) {
                    loge("Failed closing socket: " + e7);
                    throw th;
                }
            }
        } catch (IOException e8) {
            e = e8;
        } catch (Throwable th4) {
            th = th4;
            synchronized (this.mDaemonLock) {
            }
        }
    }

    public static class SensitiveArg {
        private final Object mArg;

        public SensitiveArg(Object obj) {
            this.mArg = obj;
        }

        public String toString() {
            return String.valueOf(this.mArg);
        }
    }

    @VisibleForTesting
    static void makeCommand(StringBuilder sb, StringBuilder sb2, int i, String str, Object... objArr) {
        if (str.indexOf(0) >= 0) {
            throw new IllegalArgumentException("Unexpected command: " + str);
        }
        if (str.indexOf(32) >= 0) {
            throw new IllegalArgumentException("Arguments must be separate from command");
        }
        sb.append(i);
        sb.append(' ');
        sb.append(str);
        sb2.append(i);
        sb2.append(' ');
        sb2.append(str);
        for (Object obj : objArr) {
            String strValueOf = String.valueOf(obj);
            if (strValueOf.indexOf(0) >= 0) {
                throw new IllegalArgumentException("Unexpected argument: " + obj);
            }
            sb.append(' ');
            sb2.append(' ');
            appendEscaped(sb, strValueOf);
            if (obj instanceof SensitiveArg) {
                sb2.append("[scrubbed]");
            } else {
                appendEscaped(sb2, strValueOf);
            }
        }
        sb.append((char) 0);
    }

    public void waitForCallbacks() {
        if (Thread.currentThread() == this.mLooper.getThread()) {
            throw new IllegalStateException("Must not call this method on callback thread");
        }
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        this.mCallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Slog.wtf(this.TAG, "Interrupted while waiting for unsolicited response handling", e);
        }
    }

    public NativeDaemonEvent execute(Command command) throws NativeDaemonConnectorException {
        return execute(command.mCmd, command.mArguments.toArray());
    }

    public NativeDaemonEvent execute(String str, Object... objArr) throws NativeDaemonConnectorException {
        return execute(60000L, str, objArr);
    }

    public NativeDaemonEvent execute(long j, String str, Object... objArr) throws NativeDaemonConnectorException {
        NativeDaemonEvent[] nativeDaemonEventArrExecuteForList = executeForList(j, str, objArr);
        if (nativeDaemonEventArrExecuteForList.length != 1) {
            throw new NativeDaemonConnectorException("Expected exactly one response, but received " + nativeDaemonEventArrExecuteForList.length);
        }
        return nativeDaemonEventArrExecuteForList[0];
    }

    public NativeDaemonEvent[] executeForList(Command command) throws NativeDaemonConnectorException {
        return executeForList(command.mCmd, command.mArguments.toArray());
    }

    public NativeDaemonEvent[] executeForList(String str, Object... objArr) throws NativeDaemonConnectorException {
        return executeForList(60000L, str, objArr);
    }

    public NativeDaemonEvent[] executeForList(long j, String str, Object... objArr) throws NativeDaemonConnectorException {
        NativeDaemonEvent nativeDaemonEventRemove;
        if (this.mWarnIfHeld != null && Thread.holdsLock(this.mWarnIfHeld)) {
            Slog.wtf(this.TAG, "Calling thread " + Thread.currentThread().getName() + " is holding 0x" + Integer.toHexString(System.identityHashCode(this.mWarnIfHeld)), new Throwable());
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        ArrayList arrayListNewArrayList = Lists.newArrayList();
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        int iIncrementAndGet = this.mSequenceNumber.incrementAndGet();
        makeCommand(sb, sb2, iIncrementAndGet, str, objArr);
        String string = sb.toString();
        String string2 = sb2.toString();
        log("SND -> {" + string2 + "}");
        synchronized (this.mDaemonLock) {
            if (this.mOutputStream == null) {
                throw new NativeDaemonConnectorException("missing output stream");
            }
            try {
                this.mOutputStream.write(string.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new NativeDaemonConnectorException("problem sending command", e);
            }
        }
        do {
            nativeDaemonEventRemove = this.mResponseQueue.remove(iIncrementAndGet, j, string2);
            if (nativeDaemonEventRemove == null) {
                loge("timed-out waiting for response to " + string2);
                throw new NativeDaemonTimeoutException(string2, nativeDaemonEventRemove);
            }
            arrayListNewArrayList.add(nativeDaemonEventRemove);
        } while (nativeDaemonEventRemove.isClassContinue());
        long jElapsedRealtime2 = SystemClock.elapsedRealtime() - jElapsedRealtime;
        if (jElapsedRealtime2 > 500) {
            loge("NDC Command {" + string2 + "} took too long (" + jElapsedRealtime2 + "ms)");
        }
        if (nativeDaemonEventRemove.isClassClientError()) {
            throw new NativeDaemonArgumentException(string2, nativeDaemonEventRemove);
        }
        if (nativeDaemonEventRemove.isClassServerError()) {
            throw new NativeDaemonFailureException(string2, nativeDaemonEventRemove);
        }
        return (NativeDaemonEvent[]) arrayListNewArrayList.toArray(new NativeDaemonEvent[arrayListNewArrayList.size()]);
    }

    @VisibleForTesting
    static void appendEscaped(StringBuilder sb, String str) {
        boolean z = str.indexOf(32) >= 0;
        if (z) {
            sb.append('\"');
        }
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\"') {
                sb.append("\\\"");
            } else if (cCharAt == '\\') {
                sb.append("\\\\");
            } else {
                sb.append(cCharAt);
            }
        }
        if (z) {
            sb.append('\"');
        }
    }

    private static class NativeDaemonArgumentException extends NativeDaemonConnectorException {
        public NativeDaemonArgumentException(String str, NativeDaemonEvent nativeDaemonEvent) {
            super(str, nativeDaemonEvent);
        }

        @Override
        public IllegalArgumentException rethrowAsParcelableException() {
            throw new IllegalArgumentException(getMessage(), this);
        }
    }

    private static class NativeDaemonFailureException extends NativeDaemonConnectorException {
        public NativeDaemonFailureException(String str, NativeDaemonEvent nativeDaemonEvent) {
            super(str, nativeDaemonEvent);
        }
    }

    public static class Command {
        private ArrayList<Object> mArguments = Lists.newArrayList();
        private String mCmd;

        public Command(String str, Object... objArr) {
            this.mCmd = str;
            for (Object obj : objArr) {
                appendArg(obj);
            }
        }

        public Command appendArg(Object obj) {
            this.mArguments.add(obj);
            return this;
        }
    }

    @Override
    public void monitor() {
        synchronized (this.mDaemonLock) {
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        this.mLocalLog.dump(fileDescriptor, printWriter, strArr);
        printWriter.println();
        this.mResponseQueue.dump(fileDescriptor, printWriter, strArr);
    }

    private void log(String str) {
        if (this.mDebug) {
            Slog.d(this.TAG, str);
        }
        this.mLocalLog.log(str);
    }

    private void loge(String str) {
        Slog.e(this.TAG, str);
        this.mLocalLog.log(str);
    }

    private static class ResponseQueue {
        private int mMaxCount;
        private final LinkedList<PendingCmd> mPendingCmds = new LinkedList<>();

        private static class PendingCmd {
            public int availableResponseCount;
            public final int cmdNum;
            public final String logCmd;
            public BlockingQueue<NativeDaemonEvent> responses = new ArrayBlockingQueue(10);

            public PendingCmd(int i, String str) {
                this.cmdNum = i;
                this.logCmd = str;
            }
        }

        ResponseQueue(int i) {
            this.mMaxCount = i;
        }

        public void add(int i, NativeDaemonEvent nativeDaemonEvent) {
            PendingCmd pendingCmd;
            synchronized (this.mPendingCmds) {
                Iterator<PendingCmd> it = this.mPendingCmds.iterator();
                while (true) {
                    if (it.hasNext()) {
                        pendingCmd = it.next();
                        if (pendingCmd.cmdNum == i) {
                            break;
                        }
                    } else {
                        pendingCmd = null;
                        break;
                    }
                }
                if (pendingCmd == null) {
                    while (this.mPendingCmds.size() >= this.mMaxCount) {
                        Slog.e("NativeDaemonConnector.ResponseQueue", "more buffered than allowed: " + this.mPendingCmds.size() + " >= " + this.mMaxCount);
                        PendingCmd pendingCmdRemove = this.mPendingCmds.remove();
                        Slog.e("NativeDaemonConnector.ResponseQueue", "Removing request: " + pendingCmdRemove.logCmd + " (" + pendingCmdRemove.cmdNum + ")");
                    }
                    pendingCmd = new PendingCmd(i, null);
                    this.mPendingCmds.add(pendingCmd);
                }
                pendingCmd.availableResponseCount++;
                if (pendingCmd.availableResponseCount == 0) {
                    this.mPendingCmds.remove(pendingCmd);
                }
            }
            try {
                pendingCmd.responses.put(nativeDaemonEvent);
            } catch (InterruptedException e) {
            }
        }

        public NativeDaemonEvent remove(int i, long j, String str) {
            PendingCmd pendingCmd;
            NativeDaemonEvent nativeDaemonEventPoll;
            synchronized (this.mPendingCmds) {
                Iterator<PendingCmd> it = this.mPendingCmds.iterator();
                while (true) {
                    if (it.hasNext()) {
                        pendingCmd = it.next();
                        if (pendingCmd.cmdNum == i) {
                            break;
                        }
                    } else {
                        pendingCmd = null;
                        break;
                    }
                }
                if (pendingCmd == null) {
                    pendingCmd = new PendingCmd(i, str);
                    this.mPendingCmds.add(pendingCmd);
                }
                pendingCmd.availableResponseCount--;
                if (pendingCmd.availableResponseCount == 0) {
                    this.mPendingCmds.remove(pendingCmd);
                }
            }
            try {
                nativeDaemonEventPoll = pendingCmd.responses.poll(j, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                nativeDaemonEventPoll = null;
            }
            if (nativeDaemonEventPoll == null) {
                Slog.e("NativeDaemonConnector.ResponseQueue", "Timeout waiting for response");
            }
            return nativeDaemonEventPoll;
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            printWriter.println("Pending requests:");
            synchronized (this.mPendingCmds) {
                for (PendingCmd pendingCmd : this.mPendingCmds) {
                    printWriter.println("  Cmd " + pendingCmd.cmdNum + " - " + pendingCmd.logCmd);
                }
            }
        }
    }
}
