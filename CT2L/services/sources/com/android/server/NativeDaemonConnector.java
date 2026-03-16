package com.android.server;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.LocalLog;
import android.util.Slog;
import com.android.server.Watchdog;
import com.android.server.pm.PackageManagerService;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class NativeDaemonConnector implements Runnable, Handler.Callback, Watchdog.Monitor {
    private static final int DEFAULT_TIMEOUT = 60000;
    private static final boolean LOGD = false;
    private static final boolean VDBG = false;
    private static final long WARN_EXECUTE_DELAY_MS = 500;
    private final int BUFFER_SIZE;
    private final String TAG;
    private Handler mCallbackHandler;
    private INativeDaemonConnectorCallbacks mCallbacks;
    private final Object mDaemonLock;
    private LocalLog mLocalLog;
    private final Looper mLooper;
    private OutputStream mOutputStream;
    private final ResponseQueue mResponseQueue;
    private AtomicInteger mSequenceNumber;
    private String mSocket;
    private final PowerManager.WakeLock mWakeLock;

    NativeDaemonConnector(INativeDaemonConnectorCallbacks callbacks, String socket, int responseQueueSize, String logTag, int maxLogSize, PowerManager.WakeLock wl) {
        this(callbacks, socket, responseQueueSize, logTag, maxLogSize, wl, FgThread.get().getLooper());
    }

    NativeDaemonConnector(INativeDaemonConnectorCallbacks callbacks, String socket, int responseQueueSize, String logTag, int maxLogSize, PowerManager.WakeLock wl, Looper looper) {
        this.mDaemonLock = new Object();
        this.BUFFER_SIZE = PackageManagerService.DumpState.DUMP_VERSION;
        this.mCallbacks = callbacks;
        this.mSocket = socket;
        this.mResponseQueue = new ResponseQueue(responseQueueSize);
        this.mWakeLock = wl;
        if (this.mWakeLock != null) {
            this.mWakeLock.setReferenceCounted(true);
        }
        this.mLooper = looper;
        this.mSequenceNumber = new AtomicInteger(0);
        this.TAG = logTag == null ? "NativeDaemonConnector" : logTag;
        this.mLocalLog = new LocalLog(maxLogSize);
    }

    @Override
    public void run() throws Throwable {
        this.mCallbackHandler = new Handler(this.mLooper, this);
        while (true) {
            try {
                listenToSocket();
            } catch (Exception e) {
                loge("Error in NativeDaemonConnector: " + e);
                SystemClock.sleep(5000L);
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        String event = (String) msg.obj;
        try {
            try {
                if (!this.mCallbacks.onEvent(msg.what, event, NativeDaemonEvent.unescapeArgs(event))) {
                    log(String.format("Unhandled event '%s'", event));
                }
            } catch (Exception e) {
                loge("Error handling '" + event + "': " + e);
                if (this.mCallbacks.onCheckHoldWakeLock(msg.what) && this.mWakeLock != null) {
                    this.mWakeLock.release();
                }
            }
            return true;
        } finally {
            if (this.mCallbacks.onCheckHoldWakeLock(msg.what) && this.mWakeLock != null) {
                this.mWakeLock.release();
            }
        }
    }

    private LocalSocketAddress determineSocketAddress() {
        return (this.mSocket.startsWith("__test__") && Build.IS_DEBUGGABLE) ? new LocalSocketAddress(this.mSocket) : new LocalSocketAddress(this.mSocket, LocalSocketAddress.Namespace.RESERVED);
    }

    private void listenToSocket() throws Throwable {
        LocalSocket socket;
        int count;
        LocalSocket socket2 = null;
        try {
            try {
                socket = new LocalSocket();
            } catch (IOException e) {
                ex = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            LocalSocketAddress address = determineSocketAddress();
            socket.connect(address);
            InputStream inputStream = socket.getInputStream();
            synchronized (this.mDaemonLock) {
                this.mOutputStream = socket.getOutputStream();
            }
            this.mCallbacks.onDaemonConnected();
            byte[] buffer = new byte[PackageManagerService.DumpState.DUMP_VERSION];
            int start = 0;
            while (true) {
                count = inputStream.read(buffer, start, 4096 - start);
                if (count < 0) {
                    break;
                }
                int count2 = count + start;
                int start2 = 0;
                for (int i = 0; i < count2; i++) {
                    if (buffer[i] == 0) {
                        String rawEvent = new String(buffer, start2, i - start2, StandardCharsets.UTF_8);
                        boolean releaseWl = false;
                        try {
                            try {
                                NativeDaemonEvent event = NativeDaemonEvent.parseRawEvent(rawEvent);
                                log("RCV <- {" + event + "}");
                                if (event.isClassUnsolicited()) {
                                    if (this.mCallbacks.onCheckHoldWakeLock(event.getCode()) && this.mWakeLock != null) {
                                        this.mWakeLock.acquire();
                                        releaseWl = true;
                                    }
                                    if (this.mCallbackHandler.sendMessage(this.mCallbackHandler.obtainMessage(event.getCode(), event.getRawEvent()))) {
                                        releaseWl = false;
                                    }
                                } else {
                                    this.mResponseQueue.add(event.getCmdNumber(), event);
                                }
                            } finally {
                                if (0 != 0) {
                                    this.mWakeLock.acquire();
                                }
                            }
                        } catch (IllegalArgumentException e2) {
                            log("Problem parsing message " + e2);
                            if (0 != 0) {
                                this.mWakeLock.acquire();
                            }
                        }
                        start2 = i + 1;
                    }
                }
                if (start2 == 0) {
                    log("RCV incomplete");
                }
                if (start2 != count2) {
                    int remaining = 4096 - start2;
                    System.arraycopy(buffer, start2, buffer, 0, remaining);
                    start = remaining;
                } else {
                    start = 0;
                }
            }
            loge("got " + count + " reading with start = " + start);
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
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    loge("Failed closing socket: " + ex);
                }
            }
        } catch (IOException e4) {
            ex = e4;
            socket2 = socket;
            loge("Communications error: " + ex);
            throw ex;
        } catch (Throwable th2) {
            th = th2;
            socket2 = socket;
            synchronized (this.mDaemonLock) {
                if (this.mOutputStream != null) {
                    try {
                        loge("closing stream for " + this.mSocket);
                        this.mOutputStream.close();
                    } catch (IOException e5) {
                        loge("Failed closing output stream: " + e5);
                    }
                    this.mOutputStream = null;
                }
            }
            if (socket2 == null) {
                throw th;
            }
            try {
                socket2.close();
                throw th;
            } catch (IOException ex2) {
                loge("Failed closing socket: " + ex2);
                throw th;
            }
        }
    }

    public static class SensitiveArg {
        private final Object mArg;

        public SensitiveArg(Object arg) {
            this.mArg = arg;
        }

        public String toString() {
            return String.valueOf(this.mArg);
        }
    }

    static void makeCommand(StringBuilder rawBuilder, StringBuilder logBuilder, int sequenceNumber, String cmd, Object... args) {
        if (cmd.indexOf(0) >= 0) {
            throw new IllegalArgumentException("Unexpected command: " + cmd);
        }
        if (cmd.indexOf(32) >= 0) {
            throw new IllegalArgumentException("Arguments must be separate from command");
        }
        rawBuilder.append(sequenceNumber).append(' ').append(cmd);
        logBuilder.append(sequenceNumber).append(' ').append(cmd);
        for (Object arg : args) {
            String argString = String.valueOf(arg);
            if (argString.indexOf(0) >= 0) {
                throw new IllegalArgumentException("Unexpected argument: " + arg);
            }
            rawBuilder.append(' ');
            logBuilder.append(' ');
            appendEscaped(rawBuilder, argString);
            if (arg instanceof SensitiveArg) {
                logBuilder.append("[scrubbed]");
            } else {
                appendEscaped(logBuilder, argString);
            }
        }
        rawBuilder.append((char) 0);
    }

    public NativeDaemonEvent execute(Command cmd) throws NativeDaemonConnectorException {
        return execute(cmd.mCmd, cmd.mArguments.toArray());
    }

    public NativeDaemonEvent execute(String cmd, Object... args) throws NativeDaemonConnectorException {
        NativeDaemonEvent[] events = executeForList(cmd, args);
        if (events.length != 1) {
            throw new NativeDaemonConnectorException("Expected exactly one response, but received " + events.length);
        }
        return events[0];
    }

    public NativeDaemonEvent[] executeForList(Command cmd) throws NativeDaemonConnectorException {
        return executeForList(cmd.mCmd, cmd.mArguments.toArray());
    }

    public NativeDaemonEvent[] executeForList(String cmd, Object... args) throws NativeDaemonConnectorException {
        return execute(DEFAULT_TIMEOUT, cmd, args);
    }

    public NativeDaemonEvent[] execute(int timeout, String cmd, Object... args) throws Throwable {
        NativeDaemonEvent event;
        long startTime = SystemClock.elapsedRealtime();
        ArrayList<NativeDaemonEvent> events = Lists.newArrayList();
        StringBuilder rawBuilder = new StringBuilder();
        StringBuilder logBuilder = new StringBuilder();
        int sequenceNumber = this.mSequenceNumber.incrementAndGet();
        makeCommand(rawBuilder, logBuilder, sequenceNumber, cmd, args);
        String rawCmd = rawBuilder.toString();
        String logCmd = logBuilder.toString();
        log("SND -> {" + logCmd + "}");
        synchronized (this.mDaemonLock) {
            if (this.mOutputStream == null) {
                throw new NativeDaemonConnectorException("missing output stream");
            }
            try {
                this.mOutputStream.write(rawCmd.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new NativeDaemonConnectorException("problem sending command", e);
            }
        }
        do {
            event = this.mResponseQueue.remove(sequenceNumber, timeout, logCmd);
            if (event == null) {
                loge("timed-out waiting for response to " + logCmd);
                throw new NativeDaemonFailureException(logCmd, event);
            }
            events.add(event);
        } while (event.isClassContinue());
        long endTime = SystemClock.elapsedRealtime();
        if (endTime - startTime > WARN_EXECUTE_DELAY_MS) {
            loge("NDC Command {" + logCmd + "} took too long (" + (endTime - startTime) + "ms)");
        }
        if (event.isClassClientError()) {
            throw new NativeDaemonArgumentException(logCmd, event);
        }
        if (event.isClassServerError()) {
            throw new NativeDaemonFailureException(logCmd, event);
        }
        return (NativeDaemonEvent[]) events.toArray(new NativeDaemonEvent[events.size()]);
    }

    static void appendEscaped(StringBuilder builder, String arg) {
        boolean hasSpaces = arg.indexOf(32) >= 0;
        if (hasSpaces) {
            builder.append('\"');
        }
        int length = arg.length();
        for (int i = 0; i < length; i++) {
            char c = arg.charAt(i);
            if (c == '\"') {
                builder.append("\\\"");
            } else if (c == '\\') {
                builder.append("\\\\");
            } else {
                builder.append(c);
            }
        }
        if (hasSpaces) {
            builder.append('\"');
        }
    }

    private static class NativeDaemonArgumentException extends NativeDaemonConnectorException {
        public NativeDaemonArgumentException(String command, NativeDaemonEvent event) {
            super(command, event);
        }

        @Override
        public IllegalArgumentException rethrowAsParcelableException() {
            throw new IllegalArgumentException(getMessage(), this);
        }
    }

    private static class NativeDaemonFailureException extends NativeDaemonConnectorException {
        public NativeDaemonFailureException(String command, NativeDaemonEvent event) {
            super(command, event);
        }
    }

    public static class Command {
        private ArrayList<Object> mArguments = Lists.newArrayList();
        private String mCmd;

        public Command(String cmd, Object... args) {
            this.mCmd = cmd;
            for (Object arg : args) {
                appendArg(arg);
            }
        }

        public Command appendArg(Object arg) {
            this.mArguments.add(arg);
            return this;
        }
    }

    @Override
    public void monitor() {
        synchronized (this.mDaemonLock) {
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mLocalLog.dump(fd, pw, args);
        pw.println();
        this.mResponseQueue.dump(fd, pw, args);
    }

    private void log(String logstring) {
        this.mLocalLog.log(logstring);
    }

    private void loge(String logstring) {
        Slog.e(this.TAG, logstring);
        this.mLocalLog.log(logstring);
    }

    private static class ResponseQueue {
        private int mMaxCount;
        private final LinkedList<PendingCmd> mPendingCmds = new LinkedList<>();

        private static class PendingCmd {
            public int availableResponseCount;
            public final int cmdNum;
            public final String logCmd;
            public BlockingQueue<NativeDaemonEvent> responses = new ArrayBlockingQueue(10);

            public PendingCmd(int cmdNum, String logCmd) {
                this.cmdNum = cmdNum;
                this.logCmd = logCmd;
            }
        }

        ResponseQueue(int maxCount) {
            this.mMaxCount = maxCount;
        }

        public void add(int cmdNum, NativeDaemonEvent response) throws Throwable {
            PendingCmd found;
            PendingCmd found2;
            synchronized (this.mPendingCmds) {
                try {
                    Iterator<PendingCmd> it = this.mPendingCmds.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            found = null;
                            break;
                        }
                        PendingCmd pendingCmd = it.next();
                        if (pendingCmd.cmdNum == cmdNum) {
                            found = pendingCmd;
                            break;
                        }
                    }
                    if (found == null) {
                        while (this.mPendingCmds.size() >= this.mMaxCount) {
                            try {
                                Slog.e("NativeDaemonConnector.ResponseQueue", "more buffered than allowed: " + this.mPendingCmds.size() + " >= " + this.mMaxCount);
                                PendingCmd pendingCmd2 = this.mPendingCmds.remove();
                                Slog.e("NativeDaemonConnector.ResponseQueue", "Removing request: " + pendingCmd2.logCmd + " (" + pendingCmd2.cmdNum + ")");
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                        found2 = new PendingCmd(cmdNum, null);
                        this.mPendingCmds.add(found2);
                    } else {
                        found2 = found;
                    }
                    found2.availableResponseCount++;
                    if (found2.availableResponseCount == 0) {
                        this.mPendingCmds.remove(found2);
                    }
                    try {
                        found2.responses.put(response);
                    } catch (InterruptedException e) {
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }

        public NativeDaemonEvent remove(int cmdNum, int timeoutMs, String logCmd) throws Throwable {
            PendingCmd found;
            PendingCmd found2;
            synchronized (this.mPendingCmds) {
                try {
                    Iterator<PendingCmd> it = this.mPendingCmds.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            found = null;
                            break;
                        }
                        PendingCmd pendingCmd = it.next();
                        if (pendingCmd.cmdNum == cmdNum) {
                            found = pendingCmd;
                            break;
                        }
                    }
                    if (found == null) {
                        try {
                            found2 = new PendingCmd(cmdNum, logCmd);
                            this.mPendingCmds.add(found2);
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    } else {
                        found2 = found;
                    }
                    found2.availableResponseCount--;
                    if (found2.availableResponseCount == 0) {
                        this.mPendingCmds.remove(found2);
                    }
                    NativeDaemonEvent result = null;
                    try {
                        result = found2.responses.poll(timeoutMs, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                    }
                    if (result == null) {
                        Slog.e("NativeDaemonConnector.ResponseQueue", "Timeout waiting for response");
                    }
                    return result;
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            pw.println("Pending requests:");
            synchronized (this.mPendingCmds) {
                for (PendingCmd pendingCmd : this.mPendingCmds) {
                    pw.println("  Cmd " + pendingCmd.cmdNum + " - " + pendingCmd.logCmd);
                }
            }
        }
    }
}
