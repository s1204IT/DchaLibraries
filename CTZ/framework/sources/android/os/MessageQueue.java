package android.os;

import android.util.Log;
import android.util.Printer;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import java.io.FileDescriptor;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public final class MessageQueue {
    private static final boolean DEBUG = false;
    private static final String TAG = "MessageQueue";
    private boolean mBlocked;
    private SparseArray<FileDescriptorRecord> mFileDescriptorRecords;
    Message mMessages;
    private int mNextBarrierToken;
    private IdleHandler[] mPendingIdleHandlers;
    private final boolean mQuitAllowed;
    private boolean mQuitting;
    private final ArrayList<IdleHandler> mIdleHandlers = new ArrayList<>();
    private long mPtr = nativeInit();

    public interface IdleHandler {
        boolean queueIdle();
    }

    public interface OnFileDescriptorEventListener {
        public static final int EVENT_ERROR = 4;
        public static final int EVENT_INPUT = 1;
        public static final int EVENT_OUTPUT = 2;

        @Retention(RetentionPolicy.SOURCE)
        public @interface Events {
        }

        int onFileDescriptorEvents(FileDescriptor fileDescriptor, int i);
    }

    private static native void nativeDestroy(long j);

    private static native long nativeInit();

    private static native boolean nativeIsPolling(long j);

    private native void nativePollOnce(long j, int i);

    private static native void nativeSetFileDescriptorEvents(long j, int i, int i2);

    private static native void nativeWake(long j);

    MessageQueue(boolean z) {
        this.mQuitAllowed = z;
    }

    protected void finalize() throws Throwable {
        try {
            dispose();
        } finally {
            super.finalize();
        }
    }

    private void dispose() {
        if (this.mPtr != 0) {
            nativeDestroy(this.mPtr);
            this.mPtr = 0L;
        }
    }

    public boolean isIdle() {
        boolean z;
        synchronized (this) {
            z = this.mMessages == null || SystemClock.uptimeMillis() < this.mMessages.when;
        }
        return z;
    }

    public void addIdleHandler(IdleHandler idleHandler) {
        if (idleHandler == null) {
            throw new NullPointerException("Can't add a null IdleHandler");
        }
        synchronized (this) {
            this.mIdleHandlers.add(idleHandler);
        }
    }

    public void removeIdleHandler(IdleHandler idleHandler) {
        synchronized (this) {
            this.mIdleHandlers.remove(idleHandler);
        }
    }

    public boolean isPolling() {
        boolean zIsPollingLocked;
        synchronized (this) {
            zIsPollingLocked = isPollingLocked();
        }
        return zIsPollingLocked;
    }

    private boolean isPollingLocked() {
        return !this.mQuitting && nativeIsPolling(this.mPtr);
    }

    public void addOnFileDescriptorEventListener(FileDescriptor fileDescriptor, int i, OnFileDescriptorEventListener onFileDescriptorEventListener) {
        if (fileDescriptor == null) {
            throw new IllegalArgumentException("fd must not be null");
        }
        if (onFileDescriptorEventListener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this) {
            updateOnFileDescriptorEventListenerLocked(fileDescriptor, i, onFileDescriptorEventListener);
        }
    }

    public void removeOnFileDescriptorEventListener(FileDescriptor fileDescriptor) {
        if (fileDescriptor == null) {
            throw new IllegalArgumentException("fd must not be null");
        }
        synchronized (this) {
            updateOnFileDescriptorEventListenerLocked(fileDescriptor, 0, null);
        }
    }

    private void updateOnFileDescriptorEventListenerLocked(FileDescriptor fileDescriptor, int i, OnFileDescriptorEventListener onFileDescriptorEventListener) {
        int iIndexOfKey;
        int int$ = fileDescriptor.getInt$();
        FileDescriptorRecord fileDescriptorRecordValueAt = null;
        if (this.mFileDescriptorRecords != null) {
            iIndexOfKey = this.mFileDescriptorRecords.indexOfKey(int$);
            if (iIndexOfKey >= 0 && (fileDescriptorRecordValueAt = this.mFileDescriptorRecords.valueAt(iIndexOfKey)) != null && fileDescriptorRecordValueAt.mEvents == i) {
                return;
            }
        } else {
            iIndexOfKey = -1;
        }
        if (i != 0) {
            int i2 = i | 4;
            if (fileDescriptorRecordValueAt == null) {
                if (this.mFileDescriptorRecords == null) {
                    this.mFileDescriptorRecords = new SparseArray<>();
                }
                this.mFileDescriptorRecords.put(int$, new FileDescriptorRecord(fileDescriptor, i2, onFileDescriptorEventListener));
            } else {
                fileDescriptorRecordValueAt.mListener = onFileDescriptorEventListener;
                fileDescriptorRecordValueAt.mEvents = i2;
                fileDescriptorRecordValueAt.mSeq++;
            }
            nativeSetFileDescriptorEvents(this.mPtr, int$, i2);
            return;
        }
        if (fileDescriptorRecordValueAt != null) {
            fileDescriptorRecordValueAt.mEvents = 0;
            this.mFileDescriptorRecords.removeAt(iIndexOfKey);
            nativeSetFileDescriptorEvents(this.mPtr, int$, 0);
        }
    }

    private int dispatchEvents(int i, int i2) {
        synchronized (this) {
            FileDescriptorRecord fileDescriptorRecord = this.mFileDescriptorRecords.get(i);
            if (fileDescriptorRecord == null) {
                return 0;
            }
            int i3 = fileDescriptorRecord.mEvents;
            int i4 = i2 & i3;
            if (i4 == 0) {
                return i3;
            }
            OnFileDescriptorEventListener onFileDescriptorEventListener = fileDescriptorRecord.mListener;
            int i5 = fileDescriptorRecord.mSeq;
            int iOnFileDescriptorEvents = onFileDescriptorEventListener.onFileDescriptorEvents(fileDescriptorRecord.mDescriptor, i4);
            if (iOnFileDescriptorEvents != 0) {
                iOnFileDescriptorEvents |= 4;
            }
            if (iOnFileDescriptorEvents != i3) {
                synchronized (this) {
                    int iIndexOfKey = this.mFileDescriptorRecords.indexOfKey(i);
                    if (iIndexOfKey >= 0 && this.mFileDescriptorRecords.valueAt(iIndexOfKey) == fileDescriptorRecord && fileDescriptorRecord.mSeq == i5) {
                        fileDescriptorRecord.mEvents = iOnFileDescriptorEvents;
                        if (iOnFileDescriptorEvents == 0) {
                            this.mFileDescriptorRecords.removeAt(iIndexOfKey);
                        }
                    }
                }
            }
            return iOnFileDescriptorEvents;
        }
    }

    Message next() {
        Message message;
        IdleHandler idleHandler;
        boolean zQueueIdle;
        Message message2;
        long j = this.mPtr;
        if (j == 0) {
            return null;
        }
        int size = -1;
        int iMin = 0;
        while (true) {
            if (iMin != 0) {
                Binder.flushPendingCommands();
            }
            nativePollOnce(j, iMin);
            synchronized (this) {
                long jUptimeMillis = SystemClock.uptimeMillis();
                Message message3 = this.mMessages;
                if (message3 == null || message3.target != null) {
                    message = null;
                } else {
                    while (true) {
                        message2 = message3.next;
                        if (message2 == null || message2.isAsynchronous()) {
                            break;
                        }
                        message3 = message2;
                    }
                    message = message3;
                    message3 = message2;
                }
                if (message3 == null) {
                    iMin = -1;
                } else if (jUptimeMillis < message3.when) {
                    iMin = (int) Math.min(message3.when - jUptimeMillis, 2147483647L);
                } else {
                    this.mBlocked = false;
                    if (message != null) {
                        message.next = message3.next;
                    } else {
                        this.mMessages = message3.next;
                    }
                    message3.next = null;
                    message3.markInUse();
                    return message3;
                }
                if (this.mQuitting) {
                    dispose();
                    return null;
                }
                if (size < 0 && (this.mMessages == null || jUptimeMillis < this.mMessages.when)) {
                    size = this.mIdleHandlers.size();
                }
                if (size <= 0) {
                    this.mBlocked = true;
                } else {
                    if (this.mPendingIdleHandlers == null) {
                        this.mPendingIdleHandlers = new IdleHandler[Math.max(size, 4)];
                    }
                    this.mPendingIdleHandlers = (IdleHandler[]) this.mIdleHandlers.toArray(this.mPendingIdleHandlers);
                }
            }
        }
        if (!zQueueIdle) {
            synchronized (this) {
                this.mIdleHandlers.remove(idleHandler);
            }
        }
        int i = i + 1;
    }

    void quit(boolean z) {
        if (!this.mQuitAllowed) {
            throw new IllegalStateException("Main thread not allowed to quit.");
        }
        synchronized (this) {
            if (this.mQuitting) {
                return;
            }
            this.mQuitting = true;
            if (z) {
                removeAllFutureMessagesLocked();
            } else {
                removeAllMessagesLocked();
            }
            nativeWake(this.mPtr);
        }
    }

    public int postSyncBarrier() {
        return postSyncBarrier(SystemClock.uptimeMillis());
    }

    private int postSyncBarrier(long j) {
        int i;
        synchronized (this) {
            i = this.mNextBarrierToken;
            this.mNextBarrierToken = i + 1;
            Message messageObtain = Message.obtain();
            messageObtain.markInUse();
            messageObtain.when = j;
            messageObtain.arg1 = i;
            Message message = null;
            Message message2 = this.mMessages;
            if (j != 0) {
                while (message2 != null && message2.when <= j) {
                    Message message3 = message2;
                    message2 = message2.next;
                    message = message3;
                }
            }
            if (message != null) {
                messageObtain.next = message2;
                message.next = messageObtain;
            } else {
                messageObtain.next = message2;
                this.mMessages = messageObtain;
            }
        }
        return i;
    }

    public void removeSyncBarrier(int i) {
        Message message;
        synchronized (this) {
            Message message2 = null;
            Message message3 = this.mMessages;
            while (true) {
                Message message4 = message3;
                message = message2;
                message2 = message4;
                if (message2 == null || (message2.target == null && message2.arg1 == i)) {
                    break;
                } else {
                    message3 = message2.next;
                }
            }
            if (message2 == null) {
                throw new IllegalStateException("The specified message queue synchronization  barrier token has not been posted or has already been removed.");
            }
            boolean z = false;
            if (message != null) {
                message.next = message2.next;
            } else {
                this.mMessages = message2.next;
                if (this.mMessages == null || this.mMessages.target != null) {
                    z = true;
                }
            }
            message2.recycleUnchecked();
            if (z && !this.mQuitting) {
                nativeWake(this.mPtr);
            }
        }
    }

    boolean enqueueMessage(Message message, long j) {
        boolean z;
        Message message2;
        if (message.target == null) {
            throw new IllegalArgumentException("Message must have a target.");
        }
        if (message.isInUse()) {
            throw new IllegalStateException(message + " This message is already in use.");
        }
        synchronized (this) {
            if (this.mQuitting) {
                IllegalStateException illegalStateException = new IllegalStateException(message.target + " sending message to a Handler on a dead thread");
                Log.w(TAG, illegalStateException.getMessage(), illegalStateException);
                message.recycle();
                return false;
            }
            message.markInUse();
            message.when = j;
            Message message3 = this.mMessages;
            if (message3 == null || j == 0 || j < message3.when) {
                message.next = message3;
                this.mMessages = message;
                z = this.mBlocked;
            } else {
                z = this.mBlocked && message3.target == null && message.isAsynchronous();
                while (true) {
                    message2 = message3.next;
                    if (message2 == null || j < message2.when) {
                        break;
                    }
                    if (z && message2.isAsynchronous()) {
                        z = false;
                    }
                    message3 = message2;
                }
                message.next = message2;
                message3.next = message;
            }
            if (z) {
                nativeWake(this.mPtr);
            }
            return true;
        }
    }

    boolean hasMessages(Handler handler, int i, Object obj) {
        if (handler == null) {
            return false;
        }
        synchronized (this) {
            for (Message message = this.mMessages; message != null; message = message.next) {
                if (message.target == handler && message.what == i && (obj == null || message.obj == obj)) {
                    return true;
                }
            }
            return false;
        }
    }

    boolean hasMessages(Handler handler, Runnable runnable, Object obj) {
        if (handler == null) {
            return false;
        }
        synchronized (this) {
            for (Message message = this.mMessages; message != null; message = message.next) {
                if (message.target == handler && message.callback == runnable && (obj == null || message.obj == obj)) {
                    return true;
                }
            }
            return false;
        }
    }

    boolean hasMessages(Handler handler) {
        if (handler == null) {
            return false;
        }
        synchronized (this) {
            for (Message message = this.mMessages; message != null; message = message.next) {
                if (message.target == handler) {
                    return true;
                }
            }
            return false;
        }
    }

    void removeMessages(Handler handler, int i, Object obj) {
        if (handler == null) {
            return;
        }
        synchronized (this) {
            Message message = this.mMessages;
            while (message != null && message.target == handler && message.what == i && (obj == null || message.obj == obj)) {
                Message message2 = message.next;
                this.mMessages = message2;
                message.recycleUnchecked();
                message = message2;
            }
            while (message != null) {
                Message message3 = message.next;
                if (message3 == null || message3.target != handler || message3.what != i || (obj != null && message3.obj != obj)) {
                    message = message3;
                } else {
                    Message message4 = message3.next;
                    message3.recycleUnchecked();
                    message.next = message4;
                }
            }
        }
    }

    void removeMessages(Handler handler, Runnable runnable, Object obj) {
        if (handler == null || runnable == null) {
            return;
        }
        synchronized (this) {
            Message message = this.mMessages;
            while (message != null && message.target == handler && message.callback == runnable && (obj == null || message.obj == obj)) {
                Message message2 = message.next;
                this.mMessages = message2;
                message.recycleUnchecked();
                message = message2;
            }
            while (message != null) {
                Message message3 = message.next;
                if (message3 == null || message3.target != handler || message3.callback != runnable || (obj != null && message3.obj != obj)) {
                    message = message3;
                } else {
                    Message message4 = message3.next;
                    message3.recycleUnchecked();
                    message.next = message4;
                }
            }
        }
    }

    void removeCallbacksAndMessages(Handler handler, Object obj) {
        if (handler == null) {
            return;
        }
        synchronized (this) {
            Message message = this.mMessages;
            while (message != null && message.target == handler && (obj == null || message.obj == obj)) {
                Message message2 = message.next;
                this.mMessages = message2;
                message.recycleUnchecked();
                message = message2;
            }
            while (message != null) {
                Message message3 = message.next;
                if (message3 == null || message3.target != handler || (obj != null && message3.obj != obj)) {
                    message = message3;
                } else {
                    Message message4 = message3.next;
                    message3.recycleUnchecked();
                    message.next = message4;
                }
            }
        }
    }

    private void removeAllMessagesLocked() {
        Message message = this.mMessages;
        while (message != null) {
            Message message2 = message.next;
            message.recycleUnchecked();
            message = message2;
        }
        this.mMessages = null;
    }

    private void removeAllFutureMessagesLocked() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        Message message = this.mMessages;
        if (message == null) {
            return;
        }
        if (message.when > jUptimeMillis) {
            removeAllMessagesLocked();
            return;
        }
        while (true) {
            Message message2 = message.next;
            if (message2 == null) {
                return;
            }
            if (message2.when <= jUptimeMillis) {
                message = message2;
            } else {
                message.next = null;
                while (true) {
                    Message message3 = message2.next;
                    message2.recycleUnchecked();
                    if (message3 != null) {
                        message2 = message3;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    void dump(Printer printer, String str, Handler handler) {
        synchronized (this) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            int i = 0;
            for (Message message = this.mMessages; message != null; message = message.next) {
                if (handler == null || handler == message.target) {
                    printer.println(str + "Message " + i + ": " + message.toString(jUptimeMillis));
                }
                i++;
            }
            printer.println(str + "(Total messages: " + i + ", polling=" + isPollingLocked() + ", quitting=" + this.mQuitting + ")");
        }
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        synchronized (this) {
            for (Message message = this.mMessages; message != null; message = message.next) {
                message.writeToProto(protoOutputStream, 2246267895809L);
            }
            protoOutputStream.write(1133871366146L, isPollingLocked());
            protoOutputStream.write(1133871366147L, this.mQuitting);
        }
        protoOutputStream.end(jStart);
    }

    private static final class FileDescriptorRecord {
        public final FileDescriptor mDescriptor;
        public int mEvents;
        public OnFileDescriptorEventListener mListener;
        public int mSeq;

        public FileDescriptorRecord(FileDescriptor fileDescriptor, int i, OnFileDescriptorEventListener onFileDescriptorEventListener) {
            this.mDescriptor = fileDescriptor;
            this.mEvents = i;
            this.mListener = onFileDescriptorEventListener;
        }
    }
}
