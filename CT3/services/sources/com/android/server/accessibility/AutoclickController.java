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
import android.view.accessibility.AccessibilityEvent;

public class AutoclickController implements EventStreamTransformation {
    private static final String LOG_TAG = AutoclickController.class.getSimpleName();
    private ClickDelayObserver mClickDelayObserver;
    private ClickScheduler mClickScheduler;
    private final Context mContext;
    private EventStreamTransformation mNext;
    private final int mUserId;

    public AutoclickController(Context context, int userId) {
        this.mContext = context;
        this.mUserId = userId;
    }

    @Override
    public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (event.isFromSource(8194)) {
            if (this.mClickScheduler == null) {
                Handler handler = new Handler(this.mContext.getMainLooper());
                this.mClickScheduler = new ClickScheduler(handler, 600);
                this.mClickDelayObserver = new ClickDelayObserver(this.mUserId, handler);
                this.mClickDelayObserver.start(this.mContext.getContentResolver(), this.mClickScheduler);
            }
            handleMouseMotion(event, policyFlags);
        } else if (this.mClickScheduler != null) {
            this.mClickScheduler.cancel();
        }
        if (this.mNext == null) {
            return;
        }
        this.mNext.onMotionEvent(event, rawEvent, policyFlags);
    }

    @Override
    public void onKeyEvent(KeyEvent event, int policyFlags) {
        if (this.mClickScheduler != null) {
            if (KeyEvent.isModifierKey(event.getKeyCode())) {
                this.mClickScheduler.updateMetaState(event.getMetaState());
            } else {
                this.mClickScheduler.cancel();
            }
        }
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
        if (inputSource == 8194 && this.mClickScheduler != null) {
            this.mClickScheduler.cancel();
        }
        if (this.mNext == null) {
            return;
        }
        this.mNext.clearEvents(inputSource);
    }

    @Override
    public void onDestroy() {
        if (this.mClickDelayObserver != null) {
            this.mClickDelayObserver.stop();
            this.mClickDelayObserver = null;
        }
        if (this.mClickScheduler == null) {
            return;
        }
        this.mClickScheduler.cancel();
        this.mClickScheduler = null;
    }

    private void handleMouseMotion(MotionEvent event, int policyFlags) {
        switch (event.getActionMasked()) {
            case 7:
                if (event.getPointerCount() == 1) {
                    this.mClickScheduler.update(event, policyFlags);
                } else {
                    this.mClickScheduler.cancel();
                }
                break;
            case 8:
            default:
                this.mClickScheduler.cancel();
                break;
            case 9:
            case 10:
                break;
        }
    }

    private static final class ClickDelayObserver extends ContentObserver {
        private final Uri mAutoclickDelaySettingUri;
        private ClickScheduler mClickScheduler;
        private ContentResolver mContentResolver;
        private final int mUserId;

        public ClickDelayObserver(int userId, Handler handler) {
            super(handler);
            this.mAutoclickDelaySettingUri = Settings.Secure.getUriFor("accessibility_autoclick_delay");
            this.mUserId = userId;
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
        public void onChange(boolean selfChange, Uri uri) {
            if (!this.mAutoclickDelaySettingUri.equals(uri)) {
                return;
            }
            int delay = Settings.Secure.getIntForUser(this.mContentResolver, "accessibility_autoclick_delay", 600, this.mUserId);
            this.mClickScheduler.updateDelay(delay);
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

        public ClickScheduler(Handler handler, int delay) {
            this.mHandler = handler;
            resetInternalState();
            this.mDelay = delay;
            this.mAnchorCoords = new MotionEvent.PointerCoords();
        }

        @Override
        public void run() {
            long now = SystemClock.uptimeMillis();
            if (now < this.mScheduledClickTime) {
                this.mHandler.postDelayed(this, this.mScheduledClickTime - now);
            } else {
                sendClick();
                resetInternalState();
            }
        }

        public void update(MotionEvent event, int policyFlags) {
            this.mMetaState = event.getMetaState();
            boolean moved = detectMovement(event);
            cacheLastEvent(event, policyFlags, this.mLastMotionEvent != null ? moved : true);
            if (!moved) {
                return;
            }
            rescheduleClick(this.mDelay);
        }

        public void cancel() {
            if (!this.mActive) {
                return;
            }
            resetInternalState();
            this.mHandler.removeCallbacks(this);
        }

        public void updateMetaState(int state) {
            this.mMetaState = state;
        }

        public void updateDelay(int delay) {
            this.mDelay = delay;
        }

        private void rescheduleClick(int delay) {
            long clickTime = SystemClock.uptimeMillis() + ((long) delay);
            if (this.mActive && clickTime > this.mScheduledClickTime) {
                this.mScheduledClickTime = clickTime;
                return;
            }
            if (this.mActive) {
                this.mHandler.removeCallbacks(this);
            }
            this.mActive = true;
            this.mScheduledClickTime = clickTime;
            this.mHandler.postDelayed(this, delay);
        }

        private void cacheLastEvent(MotionEvent event, int policyFlags, boolean useAsAnchor) {
            if (this.mLastMotionEvent != null) {
                this.mLastMotionEvent.recycle();
            }
            this.mLastMotionEvent = MotionEvent.obtain(event);
            this.mEventPolicyFlags = policyFlags;
            if (!useAsAnchor) {
                return;
            }
            int pointerIndex = this.mLastMotionEvent.getActionIndex();
            this.mLastMotionEvent.getPointerCoords(pointerIndex, this.mAnchorCoords);
        }

        private void resetInternalState() {
            this.mActive = false;
            if (this.mLastMotionEvent != null) {
                this.mLastMotionEvent.recycle();
                this.mLastMotionEvent = null;
            }
            this.mScheduledClickTime = -1L;
        }

        private boolean detectMovement(MotionEvent event) {
            if (this.mLastMotionEvent == null) {
                return false;
            }
            int pointerIndex = event.getActionIndex();
            float deltaX = this.mAnchorCoords.x - event.getX(pointerIndex);
            float deltaY = this.mAnchorCoords.y - event.getY(pointerIndex);
            double delta = Math.hypot(deltaX, deltaY);
            return delta > MOVEMENT_SLOPE;
        }

        private void sendClick() {
            if (this.mLastMotionEvent == null || AutoclickController.this.mNext == null) {
                return;
            }
            int pointerIndex = this.mLastMotionEvent.getActionIndex();
            if (this.mTempPointerProperties == null) {
                this.mTempPointerProperties = new MotionEvent.PointerProperties[1];
                this.mTempPointerProperties[0] = new MotionEvent.PointerProperties();
            }
            this.mLastMotionEvent.getPointerProperties(pointerIndex, this.mTempPointerProperties[0]);
            if (this.mTempPointerCoords == null) {
                this.mTempPointerCoords = new MotionEvent.PointerCoords[1];
                this.mTempPointerCoords[0] = new MotionEvent.PointerCoords();
            }
            this.mLastMotionEvent.getPointerCoords(pointerIndex, this.mTempPointerCoords[0]);
            long now = SystemClock.uptimeMillis();
            MotionEvent downEvent = MotionEvent.obtain(now, now, 0, 1, this.mTempPointerProperties, this.mTempPointerCoords, this.mMetaState, 1, 1.0f, 1.0f, this.mLastMotionEvent.getDeviceId(), 0, this.mLastMotionEvent.getSource(), this.mLastMotionEvent.getFlags());
            MotionEvent upEvent = MotionEvent.obtain(downEvent);
            upEvent.setAction(1);
            AutoclickController.this.mNext.onMotionEvent(downEvent, downEvent, this.mEventPolicyFlags);
            downEvent.recycle();
            AutoclickController.this.mNext.onMotionEvent(upEvent, upEvent, this.mEventPolicyFlags);
            upEvent.recycle();
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ClickScheduler: { active=").append(this.mActive);
            builder.append(", delay=").append(this.mDelay);
            builder.append(", scheduledClickTime=").append(this.mScheduledClickTime);
            builder.append(", anchor={x:").append(this.mAnchorCoords.x);
            builder.append(", y:").append(this.mAnchorCoords.y).append("}");
            builder.append(", metastate=").append(this.mMetaState);
            builder.append(", policyFlags=").append(this.mEventPolicyFlags);
            builder.append(", lastMotionEvent=").append(this.mLastMotionEvent);
            builder.append(" }");
            return builder.toString();
        }
    }
}
