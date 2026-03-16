package android.os;

import android.util.Log;
import android.util.Printer;
import java.util.ArrayList;

public final class MessageQueue {
    private boolean mBlocked;
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

    private static native void nativeDestroy(long j);

    private static native long nativeInit();

    private static native boolean nativeIsIdling(long j);

    private static native void nativePollOnce(long j, int i);

    private static native void nativeWake(long j);

    public void addIdleHandler(IdleHandler handler) {
        if (handler == null) {
            throw new NullPointerException("Can't add a null IdleHandler");
        }
        synchronized (this) {
            this.mIdleHandlers.add(handler);
        }
    }

    public void removeIdleHandler(IdleHandler handler) {
        synchronized (this) {
            this.mIdleHandlers.remove(handler);
        }
    }

    MessageQueue(boolean quitAllowed) {
        this.mQuitAllowed = quitAllowed;
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

    Message next() {
        IdleHandler idler;
        boolean keep;
        long ptr = this.mPtr;
        if (ptr == 0) {
            return null;
        }
        int pendingIdleHandlerCount = -1;
        int nextPollTimeoutMillis = 0;
        while (true) {
            if (nextPollTimeoutMillis != 0) {
                Binder.flushPendingCommands();
            }
            nativePollOnce(ptr, nextPollTimeoutMillis);
            synchronized (this) {
                long now = SystemClock.uptimeMillis();
                Message prevMsg = null;
                Message msg = this.mMessages;
                if (msg != null && msg.target == null) {
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                        if (msg == null) {
                            break;
                        }
                    } while (!msg.isAsynchronous());
                }
                if (msg == null) {
                    nextPollTimeoutMillis = -1;
                } else if (now < msg.when) {
                    nextPollTimeoutMillis = (int) Math.min(msg.when - now, 2147483647L);
                } else {
                    this.mBlocked = false;
                    if (prevMsg != null) {
                        prevMsg.next = msg.next;
                    } else {
                        this.mMessages = msg.next;
                    }
                    msg.next = null;
                    return msg;
                }
                if (this.mQuitting) {
                    dispose();
                    return null;
                }
                if (pendingIdleHandlerCount < 0 && (this.mMessages == null || now < this.mMessages.when)) {
                    pendingIdleHandlerCount = this.mIdleHandlers.size();
                }
                if (pendingIdleHandlerCount <= 0) {
                    this.mBlocked = true;
                } else {
                    if (this.mPendingIdleHandlers == null) {
                        this.mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
                    }
                    this.mPendingIdleHandlers = (IdleHandler[]) this.mIdleHandlers.toArray(this.mPendingIdleHandlers);
                }
            }
        }
        if (!keep) {
            synchronized (this) {
                this.mIdleHandlers.remove(idler);
            }
        }
        int i = i + 1;
    }

    void quit(boolean safe) {
        if (!this.mQuitAllowed) {
            throw new IllegalStateException("Main thread not allowed to quit.");
        }
        synchronized (this) {
            if (!this.mQuitting) {
                this.mQuitting = true;
                if (safe) {
                    removeAllFutureMessagesLocked();
                } else {
                    removeAllMessagesLocked();
                }
                nativeWake(this.mPtr);
            }
        }
    }

    int enqueueSyncBarrier(long when) {
        int token;
        synchronized (this) {
            token = this.mNextBarrierToken;
            this.mNextBarrierToken = token + 1;
            Message msg = Message.obtain();
            msg.markInUse();
            msg.when = when;
            msg.arg1 = token;
            Message prev = null;
            Message p = this.mMessages;
            if (when != 0) {
                while (p != null && p.when <= when) {
                    prev = p;
                    p = p.next;
                }
            }
            if (prev != null) {
                msg.next = p;
                prev.next = msg;
            } else {
                msg.next = p;
                this.mMessages = msg;
            }
        }
        return token;
    }

    void removeSyncBarrier(int token) {
        boolean needWake;
        synchronized (this) {
            Message prev = null;
            Message p = this.mMessages;
            while (p != null && (p.target != null || p.arg1 != token)) {
                prev = p;
                p = p.next;
            }
            if (p == null) {
                throw new IllegalStateException("The specified message queue synchronization  barrier token has not been posted or has already been removed.");
            }
            if (prev != null) {
                prev.next = p.next;
                needWake = false;
            } else {
                this.mMessages = p.next;
                needWake = this.mMessages == null || this.mMessages.target != null;
            }
            p.recycleUnchecked();
            if (needWake && !this.mQuitting) {
                nativeWake(this.mPtr);
            }
        }
    }

    boolean enqueueMessage(Message msg, long when) {
        Message prev;
        boolean needWake = false;
        if (msg.target == null) {
            throw new IllegalArgumentException("Message must have a target.");
        }
        if (msg.isInUse()) {
            throw new IllegalStateException(msg + " This message is already in use.");
        }
        synchronized (this) {
            if (this.mQuitting) {
                IllegalStateException e = new IllegalStateException(msg.target + " sending message to a Handler on a dead thread");
                Log.w("MessageQueue", e.getMessage(), e);
                msg.recycle();
                return false;
            }
            msg.markInUse();
            msg.when = when;
            Message p = this.mMessages;
            if (p == null || when == 0 || when < p.when) {
                msg.next = p;
                this.mMessages = msg;
                needWake = this.mBlocked;
            } else {
                if (this.mBlocked && p.target == null && msg.isAsynchronous()) {
                    needWake = true;
                }
                while (true) {
                    prev = p;
                    p = p.next;
                    if (p == null || when < p.when) {
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
                        needWake = false;
                    }
                }
                msg.next = p;
                prev.next = msg;
            }
            if (needWake) {
                nativeWake(this.mPtr);
            }
            return true;
        }
    }

    boolean hasMessages(Handler h, int what, Object object) {
        boolean z = false;
        if (h != null) {
            synchronized (this) {
                for (Message p = this.mMessages; p != null; p = p.next) {
                    if (p.target == h && p.what == what && (object == null || p.obj == object)) {
                        z = true;
                        break;
                    }
                }
            }
        }
        return z;
    }

    boolean hasMessages(Handler h, Runnable r, Object object) {
        boolean z = false;
        if (h != null) {
            synchronized (this) {
                for (Message p = this.mMessages; p != null; p = p.next) {
                    if (p.target == h && p.callback == r && (object == null || p.obj == object)) {
                        z = true;
                        break;
                    }
                }
            }
        }
        return z;
    }

    boolean isIdling() {
        boolean zIsIdlingLocked;
        synchronized (this) {
            zIsIdlingLocked = isIdlingLocked();
        }
        return zIsIdlingLocked;
    }

    private boolean isIdlingLocked() {
        return !this.mQuitting && nativeIsIdling(this.mPtr);
    }

    void removeMessages(Handler h, int what, Object object) {
        if (h != null) {
            synchronized (this) {
                Message p = this.mMessages;
                while (p != null && p.target == h && p.what == what && (object == null || p.obj == object)) {
                    Message n = p.next;
                    this.mMessages = n;
                    p.recycleUnchecked();
                    p = n;
                }
                while (p != null) {
                    Message n2 = p.next;
                    if (n2 != null && n2.target == h && n2.what == what && (object == null || n2.obj == object)) {
                        Message nn = n2.next;
                        n2.recycleUnchecked();
                        p.next = nn;
                    } else {
                        p = n2;
                    }
                }
            }
        }
    }

    void removeMessages(Handler h, Runnable r, Object object) {
        if (h != null && r != null) {
            synchronized (this) {
                Message p = this.mMessages;
                while (p != null && p.target == h && p.callback == r && (object == null || p.obj == object)) {
                    Message n = p.next;
                    this.mMessages = n;
                    p.recycleUnchecked();
                    p = n;
                }
                while (p != null) {
                    Message n2 = p.next;
                    if (n2 != null && n2.target == h && n2.callback == r && (object == null || n2.obj == object)) {
                        Message nn = n2.next;
                        n2.recycleUnchecked();
                        p.next = nn;
                    } else {
                        p = n2;
                    }
                }
            }
        }
    }

    void removeCallbacksAndMessages(Handler h, Object object) {
        if (h != null) {
            synchronized (this) {
                Message p = this.mMessages;
                while (p != null && p.target == h && (object == null || p.obj == object)) {
                    Message n = p.next;
                    this.mMessages = n;
                    p.recycleUnchecked();
                    p = n;
                }
                while (p != null) {
                    Message n2 = p.next;
                    if (n2 != null && n2.target == h && (object == null || n2.obj == object)) {
                        Message nn = n2.next;
                        n2.recycleUnchecked();
                        p.next = nn;
                    } else {
                        p = n2;
                    }
                }
            }
        }
    }

    private void removeAllMessagesLocked() {
        Message p = this.mMessages;
        while (p != null) {
            Message n = p.next;
            p.recycleUnchecked();
            p = n;
        }
        this.mMessages = null;
    }

    private void removeAllFutureMessagesLocked() {
        long now = SystemClock.uptimeMillis();
        Message p = this.mMessages;
        if (p == null) {
            return;
        }
        if (p.when > now) {
            removeAllMessagesLocked();
            return;
        }
        while (true) {
            Message n = p.next;
            if (n == null) {
                return;
            }
            if (n.when <= now) {
                p = n;
            } else {
                p.next = null;
                do {
                    Message p2 = n;
                    n = p2.next;
                    p2.recycleUnchecked();
                } while (n != null);
                return;
            }
        }
    }

    void dump(Printer pw, String prefix) {
        synchronized (this) {
            long now = SystemClock.uptimeMillis();
            int n = 0;
            for (Message msg = this.mMessages; msg != null; msg = msg.next) {
                pw.println(prefix + "Message " + n + ": " + msg.toString(now));
                n++;
            }
            pw.println(prefix + "(Total messages: " + n + ", idling=" + isIdlingLocked() + ", quitting=" + this.mQuitting + ")");
        }
    }
}
