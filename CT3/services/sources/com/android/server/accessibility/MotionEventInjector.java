package com.android.server.accessibility;

import android.accessibilityservice.IAccessibilityServiceClient;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import com.android.internal.os.SomeArgs;
import java.util.List;

public class MotionEventInjector implements EventStreamTransformation {
    private static final String LOG_TAG = "MotionEventInjector";
    private static final int MAX_POINTERS = 11;
    private static final int MESSAGE_INJECT_EVENTS = 2;
    private static final int MESSAGE_SEND_MOTION_EVENT = 1;
    private final Handler mHandler;
    private EventStreamTransformation mNext;
    private int mSequenceForCurrentGesture;
    private IAccessibilityServiceClient mServiceInterfaceForCurrentGesture;
    private final SparseArray<Boolean> mOpenGesturesInProgress = new SparseArray<>();
    private MotionEvent.PointerProperties[] mPointerProperties = new MotionEvent.PointerProperties[11];
    private MotionEvent.PointerCoords[] mPointerCoords = new MotionEvent.PointerCoords[11];
    private int mSourceOfInjectedGesture = 0;
    private boolean mIsDestroyed = false;

    public MotionEventInjector(Looper looper) {
        this.mHandler = new Handler(looper, new Callback(this, null));
    }

    public void injectEvents(List<MotionEvent> events, IAccessibilityServiceClient serviceInterface, int sequence) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = events;
        args.arg2 = serviceInterface;
        args.argi1 = sequence;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, args));
    }

    @Override
    public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        cancelAnyPendingInjectedEvents();
        sendMotionEventToNext(event, rawEvent, policyFlags);
    }

    @Override
    public void onKeyEvent(KeyEvent event, int policyFlags) {
        if (this.mNext == null) {
            return;
        }
        this.mNext.onKeyEvent(event, policyFlags);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (this.mNext == null) {
            return;
        }
        this.mNext.onAccessibilityEvent(event);
    }

    @Override
    public void setNext(EventStreamTransformation next) {
        this.mNext = next;
    }

    @Override
    public void clearEvents(int inputSource) {
        if (this.mHandler.hasMessages(1)) {
            return;
        }
        this.mOpenGesturesInProgress.put(inputSource, false);
    }

    @Override
    public void onDestroy() {
        cancelAnyPendingInjectedEvents();
        this.mIsDestroyed = true;
    }

    private void injectEventsMainThread(List<MotionEvent> events, IAccessibilityServiceClient serviceInterface, int sequence) {
        if (this.mIsDestroyed) {
            try {
                serviceInterface.onPerformGestureResult(sequence, false);
                return;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error sending status with mIsDestroyed to " + serviceInterface, re);
                return;
            }
        }
        cancelAnyPendingInjectedEvents();
        this.mSourceOfInjectedGesture = events.get(0).getSource();
        cancelAnyGestureInProgress(this.mSourceOfInjectedGesture);
        this.mServiceInterfaceForCurrentGesture = serviceInterface;
        this.mSequenceForCurrentGesture = sequence;
        if (this.mNext == null) {
            notifyService(false);
            return;
        }
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < events.size(); i++) {
            MotionEvent event = events.get(i);
            int numPointers = event.getPointerCount();
            if (numPointers > this.mPointerCoords.length) {
                this.mPointerCoords = new MotionEvent.PointerCoords[numPointers];
                this.mPointerProperties = new MotionEvent.PointerProperties[numPointers];
            }
            for (int j = 0; j < numPointers; j++) {
                if (this.mPointerCoords[j] == null) {
                    this.mPointerCoords[j] = new MotionEvent.PointerCoords();
                    this.mPointerProperties[j] = new MotionEvent.PointerProperties();
                }
                event.getPointerCoords(j, this.mPointerCoords[j]);
                event.getPointerProperties(j, this.mPointerProperties[j]);
            }
            MotionEvent offsetEvent = MotionEvent.obtain(event.getDownTime() + startTime, event.getEventTime() + startTime, event.getAction(), numPointers, this.mPointerProperties, this.mPointerCoords, event.getMetaState(), event.getButtonState(), event.getXPrecision(), event.getYPrecision(), event.getDeviceId(), event.getEdgeFlags(), event.getSource(), event.getFlags());
            Message message = this.mHandler.obtainMessage(1, offsetEvent);
            this.mHandler.sendMessageDelayed(message, event.getEventTime());
        }
    }

    private void sendMotionEventToNext(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (this.mNext == null) {
            return;
        }
        this.mNext.onMotionEvent(event, rawEvent, policyFlags);
        if (event.getActionMasked() == 0) {
            this.mOpenGesturesInProgress.put(event.getSource(), true);
        }
        if (event.getActionMasked() != 1 && event.getActionMasked() != 3) {
            return;
        }
        this.mOpenGesturesInProgress.put(event.getSource(), false);
    }

    private void cancelAnyGestureInProgress(int source) {
        if (this.mNext == null || !this.mOpenGesturesInProgress.get(source, false).booleanValue()) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        MotionEvent cancelEvent = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
        sendMotionEventToNext(cancelEvent, cancelEvent, 1073741824);
    }

    private void cancelAnyPendingInjectedEvents() {
        if (!this.mHandler.hasMessages(1)) {
            return;
        }
        cancelAnyGestureInProgress(this.mSourceOfInjectedGesture);
        this.mHandler.removeMessages(1);
        notifyService(false);
    }

    private void notifyService(boolean success) {
        try {
            this.mServiceInterfaceForCurrentGesture.onPerformGestureResult(this.mSequenceForCurrentGesture, success);
        } catch (RemoteException re) {
            Slog.e(LOG_TAG, "Error sending motion event injection status to " + this.mServiceInterfaceForCurrentGesture, re);
        }
    }

    private class Callback implements Handler.Callback {
        Callback(MotionEventInjector this$0, Callback callback) {
            this();
        }

        private Callback() {
        }

        @Override
        public boolean handleMessage(Message message) {
            if (message.what == 2) {
                SomeArgs args = (SomeArgs) message.obj;
                MotionEventInjector.this.injectEventsMainThread((List) args.arg1, (IAccessibilityServiceClient) args.arg2, args.argi1);
                args.recycle();
                return true;
            }
            if (message.what != 1) {
                throw new IllegalArgumentException("Unknown message: " + message.what);
            }
            MotionEvent motionEvent = (MotionEvent) message.obj;
            MotionEventInjector.this.sendMotionEventToNext(motionEvent, motionEvent, 1073741824);
            if (!MotionEventInjector.this.mHandler.hasMessages(1)) {
                MotionEventInjector.this.notifyService(true);
            }
            return true;
        }
    }
}
