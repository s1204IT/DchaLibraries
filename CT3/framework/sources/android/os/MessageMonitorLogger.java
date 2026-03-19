package android.os;

import android.content.Context;
import android.util.Log;
import com.mediatek.anrappframeworks.ANRAppFrameworks;
import com.mediatek.anrappmanager.MessageLogger;
import com.mediatek.msglogger.MessageLoggerWrapper;
import com.mediatek.msgmonitorservice.IMessageLogger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class MessageMonitorLogger extends MessageLogger {
    public static final int DISABLE_MONITOR_EXECUTION_TIMEOUT_MSG = -1;
    public static final int DISABLE_MONITOR_PENDING_TIMEOUT_MSG = -1;
    private static final boolean IS_USER_BUILD;
    public static final int START_MONITOR_EXECUTION_TIMEOUT_MSG = 3001;
    public static final int START_MONITOR_PENDING_TIMEOUT_MSG = 3002;
    private static final String TAG = "MessageMonitorLogger";
    public static HandlerThread mHandleThread;
    private static MessageLoggerCallbacks mMessageLoggerCallbacks;
    protected static boolean sEnableLooperLog;
    private MessageMonitorLogger mInstance;
    private MessageLoggerWrapper mMessageLoggerWrapper;
    protected String mName;
    protected static HashMap<MSGLoggerInfo, MessageMonitorLogger> sMap = new HashMap<>();
    public static ConcurrentHashMap<Message, MonitorMSGInfo> monitorMsg = new ConcurrentHashMap<>();
    public static MsgLoggerHandler mMsgLoggerHandler = null;

    public static class MSGLoggerInfo {
        public String msgLoggerName;
        public int msgLoggerPid;
        public int msgLoggerTid;
    }

    public interface MessageLoggerCallbacks {
        void onMessageTimeout(Message message);
    }

    static {
        boolean zEquals;
        if (Context.USER_SERVICE.equals(Build.TYPE)) {
            zEquals = true;
        } else {
            zEquals = "userdebug".equals(Build.TYPE);
        }
        IS_USER_BUILD = zEquals;
    }

    public static class MonitorMSGInfo {
        long executionTimeout;
        Message msg;
        String msgLoggerName;

        MonitorMSGInfo() {
        }
    }

    class MsgLoggerHandler extends Handler {
        public MsgLoggerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            IBinder b = ServiceManager.getService(Context.MESSAGE_MONITOR_SERVICE);
            IMessageLogger msgLoggerManager = IMessageLogger.Stub.asInterface(b);
            Iterator<MonitorMSGInfo> valueiter = MessageMonitorLogger.monitorMsg.values().iterator();
            switch (msg.what) {
                case MessageMonitorLogger.START_MONITOR_EXECUTION_TIMEOUT_MSG:
                case MessageMonitorLogger.START_MONITOR_PENDING_TIMEOUT_MSG:
                    if (MessageMonitorLogger.mMessageLoggerCallbacks == null) {
                        MonitorMSGInfo msgMonitorInfo = (MonitorMSGInfo) msg.obj;
                        if (msgMonitorInfo != null) {
                            Log.d(MessageMonitorLogger.TAG, "Monitor message timeout begin.");
                            for (MSGLoggerInfo key : MessageMonitorLogger.sMap.keySet()) {
                                if (msgMonitorInfo.msgLoggerName == key.msgLoggerName) {
                                    MessageMonitorLogger.this.dumpMessageHistory(key.msgLoggerName);
                                    try {
                                        msgLoggerManager.dumpCallStack(Process.myPid());
                                    } catch (RemoteException e) {
                                        Log.d(MessageMonitorLogger.TAG, "DumpCallStack fail" + e);
                                    }
                                }
                            }
                            MessageMonitorLogger.mMsgLoggerHandler.removeMessages(MessageMonitorLogger.START_MONITOR_PENDING_TIMEOUT_MSG);
                            MessageMonitorLogger.mMsgLoggerHandler.removeMessages(MessageMonitorLogger.START_MONITOR_EXECUTION_TIMEOUT_MSG);
                            while (true) {
                                if (valueiter.hasNext()) {
                                    if (msgMonitorInfo == valueiter.next()) {
                                        valueiter.remove();
                                    }
                                }
                            }
                            Log.d(MessageMonitorLogger.TAG, "Monitor message timeout end.");
                        }
                    } else {
                        MessageMonitorLogger.mMessageLoggerCallbacks.onMessageTimeout(msg);
                    }
                    break;
            }
        }
    }

    public MessageMonitorLogger(String name) {
        super(new ANRAppFrameworks());
        this.mMessageLoggerWrapper = null;
        this.mName = name;
        this.mInstance = this;
        IBinder b = ServiceManager.getService(Context.MESSAGE_MONITOR_SERVICE);
        IMessageLogger msgLoggerManager = IMessageLogger.Stub.asInterface(b);
        try {
            this.mMessageLoggerWrapper = new MessageLoggerWrapper(this.mInstance);
            msgLoggerManager.registerMsgLogger(name, Process.myPid(), Process.myTid(), this.mMessageLoggerWrapper);
            Log.d(TAG, "Register message logger successfully name= " + name);
        } catch (RemoteException e) {
            Log.d(TAG, "Register message logger fail " + e);
        }
        if (mHandleThread == null) {
            mHandleThread = new HandlerThread("MSGLoggerMonitorThread");
            mHandleThread.start();
            mMsgLoggerHandler = new MsgLoggerHandler(mHandleThread.getLooper());
            return;
        }
        Log.d(TAG, "Message Monitor HandlerThread has exist " + mHandleThread);
    }

    public static MessageMonitorLogger createMessageLogger(boolean mValue, String name, MessageLoggerCallbacks callback) {
        if (!IS_USER_BUILD) {
            sEnableLooperLog = mValue;
            mMessageLoggerCallbacks = callback;
            Iterator<MSGLoggerInfo> it = sMap.keySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                MSGLoggerInfo key = it.next();
                if (name.equals(key.msgLoggerName)) {
                    sMap.remove(key);
                    break;
                }
            }
            MessageMonitorLogger logger = new MessageMonitorLogger(name);
            MSGLoggerInfo msgLoggerInfo = new MSGLoggerInfo();
            msgLoggerInfo.msgLoggerName = name;
            msgLoggerInfo.msgLoggerPid = Process.myPid();
            msgLoggerInfo.msgLoggerTid = Process.myTid();
            sMap.put(msgLoggerInfo, logger);
            return logger;
        }
        return null;
    }

    public void dumpMessageHistory(String name) {
        if (sMap == null) {
            return;
        }
        for (MSGLoggerInfo key : sMap.keySet()) {
            if (name.equals(key.msgLoggerName)) {
                MessageMonitorLogger logger = sMap.get(key);
                if (logger == null) {
                    return;
                }
                logger.dump();
                return;
            }
        }
    }

    public void unregisterMsgLogger(String msgLoggerName) {
        for (MSGLoggerInfo key : sMap.keySet()) {
            if (msgLoggerName.equals(key.msgLoggerName)) {
                sMap.remove(key);
                return;
            }
        }
    }

    public void dumpAllMessageHistory() {
        if (sMap == null) {
            return;
        }
        Iterator<MessageMonitorLogger> it = sMap.values().iterator();
        while (it.hasNext()) {
            it.next().dump();
        }
    }

    public static Handler getMsgLoggerHandler() {
        if (mMsgLoggerHandler != null) {
            return mMsgLoggerHandler;
        }
        Log.d(TAG, "Monitor message handler is null");
        return null;
    }
}
