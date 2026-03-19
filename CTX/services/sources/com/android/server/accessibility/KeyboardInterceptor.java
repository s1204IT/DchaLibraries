package com.android.server.accessibility;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Pools;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.server.policy.WindowManagerPolicy;

public class KeyboardInterceptor extends BaseEventStreamTransformation implements Handler.Callback {
    private static final String LOG_TAG = "KeyboardInterceptor";
    private static final int MESSAGE_PROCESS_QUEUED_EVENTS = 1;
    private final AccessibilityManagerService mAms;
    private KeyEventHolder mEventQueueEnd;
    private KeyEventHolder mEventQueueStart;
    private final Handler mHandler;
    private final WindowManagerPolicy mPolicy;

    @Override
    public EventStreamTransformation getNext() {
        return super.getNext();
    }

    @Override
    public void setNext(EventStreamTransformation eventStreamTransformation) {
        super.setNext(eventStreamTransformation);
    }

    public KeyboardInterceptor(AccessibilityManagerService accessibilityManagerService, WindowManagerPolicy windowManagerPolicy) {
        this.mAms = accessibilityManagerService;
        this.mPolicy = windowManagerPolicy;
        this.mHandler = new Handler(this);
    }

    public KeyboardInterceptor(AccessibilityManagerService accessibilityManagerService, WindowManagerPolicy windowManagerPolicy, Handler handler) {
        this.mAms = accessibilityManagerService;
        this.mPolicy = windowManagerPolicy;
        this.mHandler = handler;
    }

    @Override
    public void onKeyEvent(KeyEvent keyEvent, int i) {
        long eventDelay = getEventDelay(keyEvent, i);
        if (eventDelay < 0) {
            return;
        }
        if (eventDelay > 0 || this.mEventQueueStart != null) {
            addEventToQueue(keyEvent, i, eventDelay);
        } else {
            this.mAms.notifyKeyEvent(keyEvent, i);
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message.what != 1) {
            Slog.e(LOG_TAG, "Unexpected message type");
            return false;
        }
        processQueuedEvents();
        if (this.mEventQueueStart != null) {
            scheduleProcessQueuedEvents();
        }
        return true;
    }

    private void addEventToQueue(KeyEvent keyEvent, int i, long j) {
        long jUptimeMillis = SystemClock.uptimeMillis() + j;
        if (this.mEventQueueStart == null) {
            KeyEventHolder keyEventHolderObtain = KeyEventHolder.obtain(keyEvent, i, jUptimeMillis);
            this.mEventQueueStart = keyEventHolderObtain;
            this.mEventQueueEnd = keyEventHolderObtain;
            scheduleProcessQueuedEvents();
            return;
        }
        KeyEventHolder keyEventHolderObtain2 = KeyEventHolder.obtain(keyEvent, i, jUptimeMillis);
        keyEventHolderObtain2.next = this.mEventQueueStart;
        this.mEventQueueStart.previous = keyEventHolderObtain2;
        this.mEventQueueStart = keyEventHolderObtain2;
    }

    private void scheduleProcessQueuedEvents() {
        if (!this.mHandler.sendEmptyMessageAtTime(1, this.mEventQueueEnd.dispatchTime)) {
            Slog.e(LOG_TAG, "Failed to schedule key event");
        }
    }

    private void processQueuedEvents() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        while (this.mEventQueueEnd != null && this.mEventQueueEnd.dispatchTime <= jUptimeMillis) {
            long eventDelay = getEventDelay(this.mEventQueueEnd.event, this.mEventQueueEnd.policyFlags);
            if (eventDelay > 0) {
                this.mEventQueueEnd.dispatchTime = jUptimeMillis + eventDelay;
                return;
            }
            if (eventDelay == 0) {
                this.mAms.notifyKeyEvent(this.mEventQueueEnd.event, this.mEventQueueEnd.policyFlags);
            }
            KeyEventHolder keyEventHolder = this.mEventQueueEnd;
            this.mEventQueueEnd = this.mEventQueueEnd.previous;
            if (this.mEventQueueEnd != null) {
                this.mEventQueueEnd.next = null;
            }
            keyEventHolder.recycle();
            if (this.mEventQueueEnd == null) {
                this.mEventQueueStart = null;
            }
        }
    }

    private long getEventDelay(KeyEvent keyEvent, int i) {
        int keyCode = keyEvent.getKeyCode();
        if (keyCode == 25 || keyCode == 24) {
            return this.mPolicy.interceptKeyBeforeDispatching(null, keyEvent, i);
        }
        return 0L;
    }

    private static class KeyEventHolder {
        private static final int MAX_POOL_SIZE = 32;
        private static final Pools.SimplePool<KeyEventHolder> sPool = new Pools.SimplePool<>(32);
        public long dispatchTime;
        public KeyEvent event;
        public KeyEventHolder next;
        public int policyFlags;
        public KeyEventHolder previous;

        private KeyEventHolder() {
        }

        public static KeyEventHolder obtain(KeyEvent keyEvent, int i, long j) {
            KeyEventHolder keyEventHolder = (KeyEventHolder) sPool.acquire();
            if (keyEventHolder == null) {
                keyEventHolder = new KeyEventHolder();
            }
            keyEventHolder.event = KeyEvent.obtain(keyEvent);
            keyEventHolder.policyFlags = i;
            keyEventHolder.dispatchTime = j;
            return keyEventHolder;
        }

        public void recycle() {
            this.event.recycle();
            this.event = null;
            this.policyFlags = 0;
            this.dispatchTime = 0L;
            this.next = null;
            this.previous = null;
            sPool.release(this);
        }
    }
}
