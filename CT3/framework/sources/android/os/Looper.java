package android.os;

import android.content.Context;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.MessageMonitorLogger;
import android.util.Log;
import android.util.Printer;

public final class Looper {
    private static final boolean IS_USER_BUILD;
    private static final String TAG = "Looper";
    private static Looper sMainLooper;
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<>();
    private Printer mLogging;
    private Printer mMsgMonitorLogging;
    final MessageQueue mQueue;
    final Thread mThread = Thread.currentThread();
    private long mTraceTag;

    static {
        boolean zEquals;
        if (Context.USER_SERVICE.equals(Build.TYPE)) {
            zEquals = true;
        } else {
            zEquals = "userdebug".equals(Build.TYPE);
        }
        IS_USER_BUILD = zEquals;
    }

    public static void prepare() {
        prepare(true);
    }

    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }

    public static void prepareMainLooper() {
        prepare(false);
        synchronized (Looper.class) {
            if (sMainLooper != null) {
                throw new IllegalStateException("The main Looper has already been prepared.");
            }
            sMainLooper = myLooper();
        }
    }

    public static Looper getMainLooper() {
        Looper looper;
        synchronized (Looper.class) {
            looper = sMainLooper;
        }
        return looper;
    }

    public static void loop() {
        Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        MessageQueue queue = me.mQueue;
        Binder.clearCallingIdentity();
        long ident = Binder.clearCallingIdentity();
        while (true) {
            Message msg = queue.next();
            if (msg == null) {
                return;
            }
            Printer logging = me.mLogging;
            if (logging != null) {
                logging.println(">>>>> Dispatching to " + msg.target + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + msg.callback + ": " + msg.what);
            }
            if (!IS_USER_BUILD) {
                Printer msglogging = me.mMsgMonitorLogging;
                if (msglogging != null) {
                    msglogging.println(">>>>> Dispatching to " + msg.target + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + msg.callback + ": " + msg.what);
                }
                if (MessageMonitorLogger.monitorMsg.containsKey(msg)) {
                    MessageMonitorLogger.MonitorMSGInfo monitorMsg = MessageMonitorLogger.monitorMsg.get(msg);
                    if (MessageMonitorLogger.mMsgLoggerHandler.hasMessages(MessageMonitorLogger.START_MONITOR_PENDING_TIMEOUT_MSG, monitorMsg)) {
                        Log.d(TAG, "RemoveMessages PENDING_TIMEOUT_MSG msg= " + msg);
                        MessageMonitorLogger.mMsgLoggerHandler.removeMessages(MessageMonitorLogger.START_MONITOR_PENDING_TIMEOUT_MSG, monitorMsg);
                        try {
                            if (monitorMsg.executionTimeout > 100) {
                                Message msg1 = MessageMonitorLogger.mMsgLoggerHandler.obtainMessage(MessageMonitorLogger.START_MONITOR_EXECUTION_TIMEOUT_MSG, monitorMsg);
                                MessageMonitorLogger.mMsgLoggerHandler.sendMessageDelayed(msg1, monitorMsg.executionTimeout);
                            } else {
                                MessageMonitorLogger.monitorMsg.remove(msg);
                                if (monitorMsg.executionTimeout != -1) {
                                    throw new IllegalArgumentException("Execution timeout <100 ms!");
                                }
                            }
                        } catch (IllegalArgumentException e) {
                            Log.d(TAG, "Execution timeout exception " + e);
                        }
                    }
                }
            }
            long traceTag = me.mTraceTag;
            if (traceTag != 0) {
                Trace.traceBegin(traceTag, msg.target.getTraceName(msg));
            }
            try {
                msg.target.dispatchMessage(msg);
                if (logging != null) {
                    logging.println("<<<<< Finished to " + msg.target + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + msg.callback);
                }
                if (!IS_USER_BUILD) {
                    Printer msglogging2 = me.mMsgMonitorLogging;
                    if (msglogging2 != null) {
                        msglogging2.println("<<<<< Finished to " + msg.target + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + msg.callback);
                    }
                    if (MessageMonitorLogger.monitorMsg.containsKey(msg)) {
                        MessageMonitorLogger.MonitorMSGInfo monitorMsg2 = MessageMonitorLogger.monitorMsg.get(msg);
                        if (MessageMonitorLogger.mMsgLoggerHandler.hasMessages(MessageMonitorLogger.START_MONITOR_EXECUTION_TIMEOUT_MSG, monitorMsg2)) {
                            Log.d(TAG, "RemoveMessages EXECUTION_TIMEOUT msg=" + msg);
                            MessageMonitorLogger.mMsgLoggerHandler.removeMessages(MessageMonitorLogger.START_MONITOR_EXECUTION_TIMEOUT_MSG, monitorMsg2);
                            MessageMonitorLogger.monitorMsg.remove(msg);
                        }
                    }
                }
                long newIdent = Binder.clearCallingIdentity();
                if (ident != newIdent) {
                    Log.wtf(TAG, "Thread identity changed from 0x" + Long.toHexString(ident) + " to 0x" + Long.toHexString(newIdent) + " while dispatching to " + msg.target.getClass().getName() + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + msg.callback + " what=" + msg.what);
                }
                msg.recycleUnchecked();
            } finally {
                if (traceTag != 0) {
                    Trace.traceEnd(traceTag);
                }
            }
        }
    }

    public static Looper myLooper() {
        return sThreadLocal.get();
    }

    public static MessageQueue myQueue() {
        return myLooper().mQueue;
    }

    private Looper(boolean quitAllowed) {
        this.mQueue = new MessageQueue(quitAllowed);
    }

    public boolean isCurrentThread() {
        return Thread.currentThread() == this.mThread;
    }

    public void setMessageLogging(Printer printer) {
        this.mLogging = printer;
    }

    public void setTraceTag(long traceTag) {
        this.mTraceTag = traceTag;
    }

    public void quit() {
        this.mQueue.quit(false);
    }

    public void quitSafely() {
        this.mQueue.quit(true);
    }

    public Thread getThread() {
        return this.mThread;
    }

    public MessageQueue getQueue() {
        return this.mQueue;
    }

    public void dump(Printer pw, String prefix) {
        pw.println(prefix + toString());
        this.mQueue.dump(pw, prefix + "  ");
    }

    public String toString() {
        return "Looper (" + this.mThread.getName() + ", tid " + this.mThread.getId() + ") {" + Integer.toHexString(System.identityHashCode(this)) + "}";
    }

    public void setMonitorMessageLogging(Printer printer) {
        this.mMsgMonitorLogging = printer;
    }
}
