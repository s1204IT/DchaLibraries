package com.mediatek.anr;

import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.Printer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MessageLogger implements Printer {
    static final int LONGER_TIME = 200;
    static final int LONGER_TIME_MESSAGE_COUNT = 20;
    static final int MESSAGE_COUNT = 20;
    private static final int MESSAGE_DUMP_SIZE_MAX = 20;
    private static final String TAG = "MessageLogger";
    public static boolean mEnableLooperLog = false;
    private static Method sGetCurrentTimeMicro = getSystemClockMethod("currentTimeMicro");
    private String MSL_Warn;
    private Method mGetMessageQueue;
    private String mLastRecord;
    private long mLastRecordDateTime;
    private long mLastRecordKernelTime;
    private CircularMessageInfoArray mLongTimeMessageHistory;
    private Field mMessageField;
    private CircularMessageInfoArray mMessageHistory;
    private Field mMessageQueueField;
    private long mMsgCnt;
    private String mName;
    private long mNonSleepLastRecordKernelTime;
    private long mProcessId;
    private int mState;
    private StringBuilder messageInfo;
    public long nonSleepWallStart;
    public long nonSleepWallTime;
    private String sInstNotCreated;
    public long wallStart;
    public long wallTime;

    private static Method getSystemClockMethod(String str) {
        try {
            return Class.forName("android.os.SystemClock").getDeclaredMethod(str, new Class[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private Method getLooperMethod(String str) {
        try {
            return Class.forName("android.os.Looper").getDeclaredMethod(str, new Class[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private Field getMessageQueueField(String str) {
        try {
            Field declaredField = Class.forName("android.os.MessageQueue").getDeclaredField(str);
            declaredField.setAccessible(true);
            return declaredField;
        } catch (Exception e) {
            return null;
        }
    }

    private Field getMessageField(String str) {
        try {
            Field declaredField = Class.forName("android.os.Message").getDeclaredField(str);
            declaredField.setAccessible(true);
            return declaredField;
        } catch (Exception e) {
            return null;
        }
    }

    public MessageLogger() {
        this.mLastRecord = null;
        this.mState = 0;
        this.mMsgCnt = 0L;
        this.mName = null;
        this.MSL_Warn = "MSL Waraning:";
        this.sInstNotCreated = this.MSL_Warn + "!!! MessageLoggerInstance might not be created !!!\n";
        this.mGetMessageQueue = getLooperMethod("getQueue");
        this.mMessageQueueField = getMessageQueueField("mMessages");
        this.mMessageField = getMessageField("next");
        init();
    }

    public MessageLogger(boolean z) {
        this.mLastRecord = null;
        this.mState = 0;
        this.mMsgCnt = 0L;
        this.mName = null;
        this.MSL_Warn = "MSL Waraning:";
        this.sInstNotCreated = this.MSL_Warn + "!!! MessageLoggerInstance might not be created !!!\n";
        this.mGetMessageQueue = getLooperMethod("getQueue");
        this.mMessageQueueField = getMessageQueueField("mMessages");
        this.mMessageField = getMessageField("next");
        mEnableLooperLog = z;
        init();
    }

    public MessageLogger(boolean z, String str) {
        this.mLastRecord = null;
        this.mState = 0;
        this.mMsgCnt = 0L;
        this.mName = null;
        this.MSL_Warn = "MSL Waraning:";
        this.sInstNotCreated = this.MSL_Warn + "!!! MessageLoggerInstance might not be created !!!\n";
        this.mGetMessageQueue = getLooperMethod("getQueue");
        this.mMessageQueueField = getMessageQueueField("mMessages");
        this.mMessageField = getMessageField("next");
        this.mName = str;
        mEnableLooperLog = z;
        init();
    }

    private void init() {
        this.mMessageHistory = new CircularMessageInfoArray(20);
        this.mLongTimeMessageHistory = new CircularMessageInfoArray(20);
        this.messageInfo = new StringBuilder(20480);
        this.mProcessId = Process.myPid();
    }

    @Override
    public void println(String str) {
        synchronized (this) {
            this.mState++;
            this.mMsgCnt++;
            this.mLastRecordKernelTime = SystemClock.elapsedRealtime();
            this.mNonSleepLastRecordKernelTime = SystemClock.uptimeMillis();
            try {
                if (sGetCurrentTimeMicro != null) {
                    this.mLastRecordDateTime = ((Long) sGetCurrentTimeMicro.invoke(null, new Object[0])).longValue();
                }
            } catch (Exception e) {
            }
            if (this.mState == 1) {
                MessageInfo messageInfoAdd = this.mMessageHistory.add();
                messageInfoAdd.init();
                messageInfoAdd.startDispatch = str;
                messageInfoAdd.msgIdStart = this.mMsgCnt;
                messageInfoAdd.startTimeElapsed = this.mLastRecordDateTime;
                messageInfoAdd.startTimeUp = this.mNonSleepLastRecordKernelTime;
            } else {
                this.mState = 0;
                MessageInfo last = this.mMessageHistory.getLast();
                last.finishDispatch = str;
                last.msgIdFinish = this.mMsgCnt;
                last.durationElapsed = this.mLastRecordDateTime - last.startTimeElapsed;
                last.durationUp = this.mNonSleepLastRecordKernelTime - last.startTimeUp;
                this.wallTime = last.durationElapsed;
                if (last.durationElapsed >= 200000) {
                    this.mLongTimeMessageHistory.add().copy(last);
                }
            }
            if (mEnableLooperLog) {
                if (this.mState == 1) {
                    Log.d(TAG, "Debugging_MessageLogger: " + str + " start");
                } else {
                    Log.d(TAG, "Debugging_MessageLogger: " + str + " spent " + (this.wallTime / 1000) + "ms");
                }
            }
        }
    }

    public void setInitStr(String str) {
        this.messageInfo.delete(0, this.messageInfo.length());
        this.messageInfo.append(str);
    }

    private void log(String str) {
        StringBuilder sb = this.messageInfo;
        sb.append(str);
        sb.append("\n");
    }

    public void dumpMessageQueue() {
        try {
            Looper mainLooper = Looper.getMainLooper();
            if (mainLooper == null) {
                log(this.MSL_Warn + "!!! Current MainLooper is Null !!!");
            } else {
                MessageQueue messageQueue = (MessageQueue) this.mGetMessageQueue.invoke(mainLooper, new Object[0]);
                if (messageQueue == null) {
                    log(this.MSL_Warn + "!!! Current MainLooper's MsgQueue is Null !!!");
                } else {
                    dumpMessageQueueImpl(messageQueue);
                }
            }
        } catch (Exception e) {
        }
        log(String.format(this.MSL_Warn + "!!! Calling thread from PID:%d's TID:%d(%s),Thread's type is %s!!!", Integer.valueOf(Process.myPid()), Long.valueOf(Thread.currentThread().getId()), Thread.currentThread().getName(), Thread.currentThread().getClass().getName()));
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        log(String.format(this.MSL_Warn + "!!! get StackTrace: !!!", new Object[0]));
        for (int i = 0; i < stackTrace.length; i++) {
            log(String.format(this.MSL_Warn + "File:%s's Linenumber:%d, Class:%s, Method:%s", stackTrace[i].getFileName(), Integer.valueOf(stackTrace[i].getLineNumber()), stackTrace[i].getClassName(), stackTrace[i].getMethodName()));
        }
    }

    public void dumpMessageQueueImpl(MessageQueue messageQueue) throws Exception {
        synchronized (messageQueue) {
            Message message = null;
            if (this.mMessageQueueField != null) {
                message = (Message) this.mMessageQueueField.get(messageQueue);
            }
            if (message != null) {
                log("Dump first 20 messages in Queue: ");
                int i = 0;
                while (message != null) {
                    i++;
                    if (i <= 20) {
                        log("Dump Message in Queue (" + i + "): " + message);
                    }
                    message = (Message) this.mMessageField.get(message);
                }
                log("Total Message Count: " + i);
            } else {
                log("mMessages is null");
            }
        }
    }

    public void dumpMessageHistory() {
        synchronized (this) {
            log(">>> Entering MessageLogger.dump. to Dump MSG HISTORY <<<");
            if (this.mMessageHistory == null || this.mMessageHistory.size() == 0) {
                log(this.sInstNotCreated);
                dumpMessageQueue();
                try {
                    AnrManagerNative.getDefault().informMessageDump(this.messageInfo.toString(), Process.myPid());
                } catch (RemoteException e) {
                    Log.d(TAG, "informMessageDump exception " + e);
                }
                return;
            }
            log("MSG HISTORY IN MAIN THREAD:");
            log("Current kernel time : " + SystemClock.uptimeMillis() + "ms PID=" + this.mProcessId);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            int size = this.mMessageHistory.size() - 1;
            if (this.mState == 1) {
                Date date = new Date(this.mLastRecordDateTime / 1000);
                long jElapsedRealtime = SystemClock.elapsedRealtime() - this.mLastRecordKernelTime;
                long jUptimeMillis = SystemClock.uptimeMillis() - this.mNonSleepLastRecordKernelTime;
                MessageInfo last = this.mMessageHistory.getLast();
                log("Last record : Msg#:" + last.msgIdStart + " " + last.startDispatch);
                log("Last record dispatching elapsedTime:" + jElapsedRealtime + " ms/upTime:" + jUptimeMillis + " ms");
                StringBuilder sb = new StringBuilder();
                sb.append("Last record dispatching time : ");
                sb.append(simpleDateFormat.format(date));
                log(sb.toString());
                size += -1;
            }
            while (size >= 0) {
                MessageInfo messageInfo = this.mMessageHistory.get(size);
                Date date2 = new Date(messageInfo.startTimeElapsed / 1000);
                log("Msg#:" + messageInfo.msgIdFinish + " " + messageInfo.finishDispatch + " elapsedTime:" + (messageInfo.durationElapsed / 1000) + " ms/upTime:" + messageInfo.durationUp + " ms");
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Msg#:");
                sb2.append(messageInfo.msgIdStart);
                sb2.append(" ");
                sb2.append(messageInfo.startDispatch);
                sb2.append(" from ");
                sb2.append(simpleDateFormat.format(date2));
                log(sb2.toString());
                size += -1;
            }
            log("=== Finish Dumping MSG HISTORY===");
            log("=== LONGER MSG HISTORY IN MAIN THREAD ===");
            for (int size2 = this.mLongTimeMessageHistory.size() - 1; size2 >= 0; size2 += -1) {
                MessageInfo messageInfo2 = this.mLongTimeMessageHistory.get(size2);
                log("Msg#:" + messageInfo2.msgIdStart + " " + messageInfo2.startDispatch + " from " + simpleDateFormat.format(new Date(messageInfo2.startTimeElapsed / 1000)) + " elapsedTime:" + (messageInfo2.durationElapsed / 1000) + " ms/upTime:" + messageInfo2.durationUp + "ms");
            }
            log("=== Finish Dumping LONGER MSG HISTORY===");
            try {
                dumpMessageQueue();
                AnrManagerNative.getDefault().informMessageDump(new String(this.messageInfo.toString()), Process.myPid());
                this.messageInfo.delete(0, this.messageInfo.length());
            } catch (RemoteException e2) {
                Log.d(TAG, "informMessageDump exception " + e2);
            }
            return;
        }
    }

    public class MessageInfo {
        public long durationElapsed;
        public long durationUp;
        public String finishDispatch;
        public long msgIdFinish;
        public long msgIdStart;
        public String startDispatch;
        public long startTimeElapsed;
        public long startTimeUp;

        public MessageInfo() {
            init();
        }

        public void init() {
            this.startDispatch = null;
            this.finishDispatch = null;
            this.msgIdStart = -1L;
            this.msgIdFinish = -1L;
            this.startTimeUp = 0L;
            this.durationUp = -1L;
            this.startTimeElapsed = 0L;
            this.durationElapsed = -1L;
        }

        public void copy(MessageInfo messageInfo) {
            this.startDispatch = messageInfo.startDispatch;
            this.finishDispatch = messageInfo.finishDispatch;
            this.msgIdStart = messageInfo.msgIdStart;
            this.msgIdFinish = messageInfo.msgIdFinish;
            this.startTimeUp = messageInfo.startTimeUp;
            this.durationUp = messageInfo.durationUp;
            this.startTimeElapsed = messageInfo.startTimeElapsed;
            this.durationElapsed = messageInfo.durationElapsed;
        }
    }

    public class CircularMessageInfoArray {
        private MessageInfo[] mElem;
        private int mHead;
        private MessageInfo mLastElem;
        private int mSize;
        private int mTail;

        public CircularMessageInfoArray(int i) {
            int i2 = i + 1;
            this.mElem = new MessageInfo[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                this.mElem[i3] = MessageLogger.this.new MessageInfo();
            }
            this.mHead = 0;
            this.mTail = 0;
            this.mLastElem = null;
            this.mSize = i2;
        }

        public boolean empty() {
            return this.mHead == this.mTail || this.mElem == null;
        }

        public boolean full() {
            return this.mTail == this.mHead - 1 || this.mTail - this.mHead == this.mSize - 1;
        }

        public int size() {
            if (this.mTail - this.mHead >= 0) {
                return this.mTail - this.mHead;
            }
            return (this.mSize + this.mTail) - this.mHead;
        }

        private MessageInfo getLocked(int i) {
            if (this.mHead + i <= this.mSize - 1) {
                return this.mElem[this.mHead + i];
            }
            return this.mElem[(this.mHead + i) - this.mSize];
        }

        public synchronized MessageInfo get(int i) {
            if (i >= 0) {
                if (i < size()) {
                    return getLocked(i);
                }
            }
            return null;
        }

        public MessageInfo getLast() {
            return this.mLastElem;
        }

        public synchronized MessageInfo add() {
            if (full()) {
                this.mHead++;
                if (this.mHead == this.mSize) {
                    this.mHead = 0;
                }
            }
            this.mLastElem = this.mElem[this.mTail];
            this.mTail++;
            if (this.mTail == this.mSize) {
                this.mTail = 0;
            }
            return this.mLastElem;
        }
    }
}
