package com.android.server.accessibility;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.android.server.usb.descriptors.UsbACInterface;

public class AutoclickController extends BaseEventStreamTransformation {
    private static final String LOG_TAG = AutoclickController.class.getSimpleName();
    private ClickDelayObserver mClickDelayObserver;
    private ClickScheduler mClickScheduler;
    private final Context mContext;
    private final int mUserId;

    @Override
    public EventStreamTransformation getNext() {
        return super.getNext();
    }

    @Override
    public void setNext(EventStreamTransformation eventStreamTransformation) {
        super.setNext(eventStreamTransformation);
    }

    public AutoclickController(Context context, int i) {
        this.mContext = context;
        this.mUserId = i;
    }

    @Override
    public void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
        if (motionEvent.isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
            if (this.mClickScheduler == null) {
                Handler handler = new Handler(this.mContext.getMainLooper());
                this.mClickScheduler = new ClickScheduler(handler, 600);
                this.mClickDelayObserver = new ClickDelayObserver(this.mUserId, handler);
                this.mClickDelayObserver.start(this.mContext.getContentResolver(), this.mClickScheduler);
            }
            handleMouseMotion(motionEvent, i);
        } else if (this.mClickScheduler != null) {
            this.mClickScheduler.cancel();
        }
        super.onMotionEvent(motionEvent, motionEvent2, i);
    }

    @Override
    public void onKeyEvent(KeyEvent keyEvent, int i) {
        if (this.mClickScheduler != null) {
            if (KeyEvent.isModifierKey(keyEvent.getKeyCode())) {
                this.mClickScheduler.updateMetaState(keyEvent.getMetaState());
            } else {
                this.mClickScheduler.cancel();
            }
        }
        super.onKeyEvent(keyEvent, i);
    }

    @Override
    public void clearEvents(int i) {
        if (i == 8194 && this.mClickScheduler != null) {
            this.mClickScheduler.cancel();
        }
        super.clearEvents(i);
    }

    @Override
    public void onDestroy() {
        if (this.mClickDelayObserver != null) {
            this.mClickDelayObserver.stop();
            this.mClickDelayObserver = null;
        }
        if (this.mClickScheduler != null) {
            this.mClickScheduler.cancel();
            this.mClickScheduler = null;
        }
    }

    private void handleMouseMotion(MotionEvent motionEvent, int i) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 7) {
            if (motionEvent.getPointerCount() == 1) {
                this.mClickScheduler.update(motionEvent, i);
                return;
            } else {
                this.mClickScheduler.cancel();
            }
        }
        switch (actionMasked) {
            case 9:
            case 10:
                break;
            default:
                this.mClickScheduler.cancel();
                break;
        }
    }

    private static final class ClickDelayObserver extends ContentObserver {
        private final Uri mAutoclickDelaySettingUri;
        private ClickScheduler mClickScheduler;
        private ContentResolver mContentResolver;
        private final int mUserId;

        public ClickDelayObserver(int i, Handler handler) {
            super(handler);
            this.mAutoclickDelaySettingUri = Settings.Secure.getUriFor("accessibility_autoclick_delay");
            this.mUserId = i;
        }

        public void start(ContentResolver contentResolver, ClickScheduler clickScheduler) {
            if (this.mContentResolver != null || this.mClickScheduler != null) {
                throw new IllegalStateException("Observer already started.");
            }
            if (contentResolver == null) {
                throw new NullPointerException("contentResolver not set.");
            }
            if (clickScheduler == null) {
                throw new NullPointerException("clickScheduler not set.");
            }
            this.mContentResolver = contentResolver;
            this.mClickScheduler = clickScheduler;
            this.mContentResolver.registerContentObserver(this.mAutoclickDelaySettingUri, false, this, this.mUserId);
            onChange(true, this.mAutoclickDelaySettingUri);
        }

        public void stop() {
            if (this.mContentResolver == null || this.mClickScheduler == null) {
                throw new IllegalStateException("ClickDelayObserver not started.");
            }
            this.mContentResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            if (this.mAutoclickDelaySettingUri.equals(uri)) {
                this.mClickScheduler.updateDelay(Settings.Secure.getIntForUser(this.mContentResolver, "accessibility_autoclick_delay", 600, this.mUserId));
            }
        }
    }

    private final class ClickScheduler implements Runnable {
        private static final double MOVEMENT_SLOPE = 20.0d;
        private boolean mActive;
        private MotionEvent.PointerCoords mAnchorCoords;
        private int mDelay;
        private int mEventPolicyFlags;
        private Handler mHandler;
        private MotionEvent mLastMotionEvent = null;
        private int mMetaState;
        private long mScheduledClickTime;
        private MotionEvent.PointerCoords[] mTempPointerCoords;
        private MotionEvent.PointerProperties[] mTempPointerProperties;

        public ClickScheduler(Handler handler, int i) {
            this.mHandler = handler;
            resetInternalState();
            this.mDelay = i;
            this.mAnchorCoords = new MotionEvent.PointerCoords();
        }

        @Override
        public void run() {
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (jUptimeMillis < this.mScheduledClickTime) {
                this.mHandler.postDelayed(this, this.mScheduledClickTime - jUptimeMillis);
            } else {
                sendClick();
                resetInternalState();
            }
        }

        public void update(MotionEvent motionEvent, int i) {
            this.mMetaState = motionEvent.getMetaState();
            boolean zDetectMovement = detectMovement(motionEvent);
            cacheLastEvent(motionEvent, i, this.mLastMotionEvent == null || zDetectMovement);
            if (zDetectMovement) {
                rescheduleClick(this.mDelay);
            }
        }

        public void cancel() {
            if (!this.mActive) {
                return;
            }
            resetInternalState();
            this.mHandler.removeCallbacks(this);
        }

        public void updateMetaState(int i) {
            this.mMetaState = i;
        }

        public void updateDelay(int i) {
            this.mDelay = i;
        }

        private void rescheduleClick(int i) {
            long j = i;
            long jUptimeMillis = SystemClock.uptimeMillis() + j;
            if (this.mActive && jUptimeMillis > this.mScheduledClickTime) {
                this.mScheduledClickTime = jUptimeMillis;
                return;
            }
            if (this.mActive) {
                this.mHandler.removeCallbacks(this);
            }
            this.mActive = true;
            this.mScheduledClickTime = jUptimeMillis;
            this.mHandler.postDelayed(this, j);
        }

        private void cacheLastEvent(MotionEvent motionEvent, int i, boolean z) {
            if (this.mLastMotionEvent != null) {
                this.mLastMotionEvent.recycle();
            }
            this.mLastMotionEvent = MotionEvent.obtain(motionEvent);
            this.mEventPolicyFlags = i;
            if (z) {
                this.mLastMotionEvent.getPointerCoords(this.mLastMotionEvent.getActionIndex(), this.mAnchorCoords);
            }
        }

        private void resetInternalState() {
            this.mActive = false;
            if (this.mLastMotionEvent != null) {
                this.mLastMotionEvent.recycle();
                this.mLastMotionEvent = null;
            }
            this.mScheduledClickTime = -1L;
        }

        private boolean detectMovement(MotionEvent motionEvent) {
            if (this.mLastMotionEvent == null) {
                return false;
            }
            int actionIndex = motionEvent.getActionIndex();
            return Math.hypot((double) (this.mAnchorCoords.x - motionEvent.getX(actionIndex)), (double) (this.mAnchorCoords.y - motionEvent.getY(actionIndex))) > MOVEMENT_SLOPE;
        }

        private void sendClick() {
            if (this.mLastMotionEvent == null || AutoclickController.this.getNext() == null) {
                return;
            }
            int actionIndex = this.mLastMotionEvent.getActionIndex();
            if (this.mTempPointerProperties == null) {
                this.mTempPointerProperties = new MotionEvent.PointerProperties[1];
                this.mTempPointerProperties[0] = new MotionEvent.PointerProperties();
            }
            this.mLastMotionEvent.getPointerProperties(actionIndex, this.mTempPointerProperties[0]);
            if (this.mTempPointerCoords == null) {
                this.mTempPointerCoords = new MotionEvent.PointerCoords[1];
                this.mTempPointerCoords[0] = new MotionEvent.PointerCoords();
            }
            this.mLastMotionEvent.getPointerCoords(actionIndex, this.mTempPointerCoords[0]);
            long jUptimeMillis = SystemClock.uptimeMillis();
            MotionEvent motionEventObtain = MotionEvent.obtain(jUptimeMillis, jUptimeMillis, 0, 1, this.mTempPointerProperties, this.mTempPointerCoords, this.mMetaState, 1, 1.0f, 1.0f, this.mLastMotionEvent.getDeviceId(), 0, this.mLastMotionEvent.getSource(), this.mLastMotionEvent.getFlags());
            MotionEvent motionEventObtain2 = MotionEvent.obtain(motionEventObtain);
            motionEventObtain2.setAction(1);
            AutoclickController.super.onMotionEvent(motionEventObtain, motionEventObtain, this.mEventPolicyFlags);
            motionEventObtain.recycle();
            AutoclickController.super.onMotionEvent(motionEventObtain2, motionEventObtain2, this.mEventPolicyFlags);
            motionEventObtain2.recycle();
        }

        public String toString() {
            return "ClickScheduler: { active=" + this.mActive + ", delay=" + this.mDelay + ", scheduledClickTime=" + this.mScheduledClickTime + ", anchor={x:" + this.mAnchorCoords.x + ", y:" + this.mAnchorCoords.y + "}, metastate=" + this.mMetaState + ", policyFlags=" + this.mEventPolicyFlags + ", lastMotionEvent=" + this.mLastMotionEvent + " }";
        }
    }
}
