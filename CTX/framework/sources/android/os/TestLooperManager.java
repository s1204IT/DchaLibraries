package android.os;

import android.util.ArraySet;
import java.util.concurrent.LinkedBlockingQueue;

public class TestLooperManager {
    private static final ArraySet<Looper> sHeldLoopers = new ArraySet<>();
    private final LinkedBlockingQueue<MessageExecution> mExecuteQueue = new LinkedBlockingQueue<>();
    private final Looper mLooper;
    private boolean mLooperBlocked;
    private final MessageQueue mQueue;
    private boolean mReleased;

    public TestLooperManager(Looper looper) {
        synchronized (sHeldLoopers) {
            if (sHeldLoopers.contains(looper)) {
                throw new RuntimeException("TestLooperManager already held for this looper");
            }
            sHeldLoopers.add(looper);
        }
        this.mLooper = looper;
        this.mQueue = this.mLooper.getQueue();
        new Handler(looper).post(new LooperHolder());
    }

    public MessageQueue getMessageQueue() {
        checkReleased();
        return this.mQueue;
    }

    @Deprecated
    public MessageQueue getQueue() {
        return getMessageQueue();
    }

    public Message next() {
        while (!this.mLooperBlocked) {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        checkReleased();
        return this.mQueue.next();
    }

    public void release() {
        synchronized (sHeldLoopers) {
            sHeldLoopers.remove(this.mLooper);
        }
        checkReleased();
        this.mReleased = true;
        this.mExecuteQueue.add(new MessageExecution());
    }

    public void execute(Message message) {
        checkReleased();
        if (Looper.myLooper() == this.mLooper) {
            message.target.dispatchMessage(message);
            return;
        }
        MessageExecution messageExecution = new MessageExecution();
        messageExecution.m = message;
        synchronized (messageExecution) {
            this.mExecuteQueue.add(messageExecution);
            try {
                messageExecution.wait();
            } catch (InterruptedException e) {
            }
            if (messageExecution.response != null) {
                throw new RuntimeException(messageExecution.response);
            }
        }
    }

    public void recycle(Message message) {
        checkReleased();
        message.recycleUnchecked();
    }

    public boolean hasMessages(Handler handler, Object obj, int i) {
        checkReleased();
        return this.mQueue.hasMessages(handler, i, obj);
    }

    public boolean hasMessages(Handler handler, Object obj, Runnable runnable) {
        checkReleased();
        return this.mQueue.hasMessages(handler, runnable, obj);
    }

    private void checkReleased() {
        if (this.mReleased) {
            throw new RuntimeException("release() has already be called");
        }
    }

    private class LooperHolder implements Runnable {
        private LooperHolder() {
        }

        @Override
        public void run() {
            synchronized (TestLooperManager.this) {
                TestLooperManager.this.mLooperBlocked = true;
                TestLooperManager.this.notify();
            }
            while (!TestLooperManager.this.mReleased) {
                try {
                    MessageExecution messageExecution = (MessageExecution) TestLooperManager.this.mExecuteQueue.take();
                    if (messageExecution.m != null) {
                        processMessage(messageExecution);
                    }
                } catch (InterruptedException e) {
                }
            }
            synchronized (TestLooperManager.this) {
                TestLooperManager.this.mLooperBlocked = false;
            }
        }

        private void processMessage(MessageExecution messageExecution) {
            synchronized (messageExecution) {
                try {
                    messageExecution.m.target.dispatchMessage(messageExecution.m);
                    messageExecution.response = null;
                } catch (Throwable th) {
                    messageExecution.response = th;
                }
                messageExecution.notifyAll();
            }
        }
    }

    private static class MessageExecution {
        private Message m;
        private Throwable response;

        private MessageExecution() {
        }
    }
}
