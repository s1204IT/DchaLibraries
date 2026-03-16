package com.android.server.accessibility;

import android.R;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Property;
import android.view.GestureDetector;
import android.view.MagnificationSpec;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.view.WindowManagerInternal;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import com.android.internal.os.SomeArgs;
import com.android.server.LocalServices;
import java.util.Locale;

public final class ScreenMagnifier implements WindowManagerInternal.MagnificationCallbacks, EventStreamTransformation {
    private static final boolean DEBUG_DETECTING = false;
    private static final boolean DEBUG_MAGNIFICATION_CONTROLLER = false;
    private static final boolean DEBUG_PANNING = false;
    private static final boolean DEBUG_SCALING = false;
    private static final boolean DEBUG_SET_MAGNIFICATION_SPEC = false;
    private static final boolean DEBUG_STATE_TRANSITIONS = false;
    private static final float DEFAULT_MAGNIFICATION_SCALE = 2.0f;
    private static final int DEFAULT_SCREEN_MAGNIFICATION_AUTO_UPDATE = 1;
    private static final int MESSAGE_ON_MAGNIFIED_BOUNDS_CHANGED = 1;
    private static final int MESSAGE_ON_RECTANGLE_ON_SCREEN_REQUESTED = 2;
    private static final int MESSAGE_ON_ROTATION_CHANGED = 4;
    private static final int MESSAGE_ON_USER_CONTEXT_CHANGED = 3;
    private static final int MULTI_TAP_TIME_SLOP_ADJUSTMENT = 50;
    private static final int STATE_DELEGATING = 1;
    private static final int STATE_DETECTING = 2;
    private static final int STATE_MAGNIFIED_INTERACTION = 4;
    private static final int STATE_VIEWPORT_DRAGGING = 3;
    private final AccessibilityManagerService mAms;
    private final Context mContext;
    private int mCurrentState;
    private long mDelegatingStateDownTime;
    private final DetectingStateHandler mDetectingStateHandler;
    private final long mLongAnimationDuration;
    private final MagnificationController mMagnificationController;
    private final MagnifiedContentInteractonStateHandler mMagnifiedContentInteractonStateHandler;
    private final int mMultiTapDistanceSlop;
    private EventStreamTransformation mNext;
    private int mPreviousState;
    private final ScreenStateObserver mScreenStateObserver;
    private final StateViewportDraggingHandler mStateViewportDraggingHandler;
    private final int mTapDistanceSlop;
    private MotionEvent.PointerCoords[] mTempPointerCoords;
    private MotionEvent.PointerProperties[] mTempPointerProperties;
    private boolean mTranslationEnabledBeforePan;
    private boolean mUpdateMagnificationSpecOnNextBoundsChange;
    private static final String LOG_TAG = ScreenMagnifier.class.getSimpleName();
    private static final int MY_PID = Process.myPid();
    private final Rect mTempRect = new Rect();
    private final Rect mTempRect1 = new Rect();
    private final int mTapTimeSlop = ViewConfiguration.getTapTimeout();
    private final int mMultiTapTimeSlop = ViewConfiguration.getDoubleTapTimeout() - 50;
    private final Region mMagnifiedBounds = new Region();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    Region bounds = (Region) message.obj;
                    ScreenMagnifier.this.handleOnMagnifiedBoundsChanged(bounds);
                    bounds.recycle();
                    break;
                case 2:
                    SomeArgs args = (SomeArgs) message.obj;
                    int left = args.argi1;
                    int top = args.argi2;
                    int right = args.argi3;
                    int bottom = args.argi4;
                    ScreenMagnifier.this.handleOnRectangleOnScreenRequested(left, top, right, bottom);
                    args.recycle();
                    break;
                case 3:
                    ScreenMagnifier.this.handleOnUserContextChanged();
                    break;
                case 4:
                    int rotation = message.arg1;
                    ScreenMagnifier.this.handleOnRotationChanged(rotation);
                    break;
            }
        }
    };
    private final WindowManagerInternal mWindowManager = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);

    public ScreenMagnifier(Context context, int displayId, AccessibilityManagerService service) {
        this.mContext = context;
        this.mAms = service;
        this.mLongAnimationDuration = context.getResources().getInteger(R.integer.config_longAnimTime);
        this.mTapDistanceSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mMultiTapDistanceSlop = ViewConfiguration.get(context).getScaledDoubleTapSlop();
        this.mDetectingStateHandler = new DetectingStateHandler();
        this.mStateViewportDraggingHandler = new StateViewportDraggingHandler();
        this.mMagnifiedContentInteractonStateHandler = new MagnifiedContentInteractonStateHandler(context);
        this.mMagnificationController = new MagnificationController(this.mLongAnimationDuration);
        this.mScreenStateObserver = new ScreenStateObserver(context, this.mMagnificationController);
        this.mWindowManager.setMagnificationCallbacks(this);
        transitionToState(2);
    }

    public void onMagnifedBoundsChanged(Region bounds) {
        Region newBounds = Region.obtain(bounds);
        this.mHandler.obtainMessage(1, newBounds).sendToTarget();
        if (MY_PID != Binder.getCallingPid()) {
            bounds.recycle();
        }
    }

    private void handleOnMagnifiedBoundsChanged(Region bounds) {
        if (this.mUpdateMagnificationSpecOnNextBoundsChange) {
            this.mUpdateMagnificationSpecOnNextBoundsChange = false;
            MagnificationSpec spec = this.mMagnificationController.getMagnificationSpec();
            Rect magnifiedFrame = this.mTempRect;
            this.mMagnifiedBounds.getBounds(magnifiedFrame);
            float scale = spec.scale;
            float centerX = ((-spec.offsetX) + (magnifiedFrame.width() / 2)) / scale;
            float centerY = ((-spec.offsetY) + (magnifiedFrame.height() / 2)) / scale;
            this.mMagnificationController.setScaleAndMagnifiedRegionCenter(scale, centerX, centerY, false);
        }
        this.mMagnifiedBounds.set(bounds);
        this.mAms.onMagnificationStateChanged();
    }

    public void onRectangleOnScreenRequested(int left, int top, int right, int bottom) {
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = left;
        args.argi2 = top;
        args.argi3 = right;
        args.argi4 = bottom;
        this.mHandler.obtainMessage(2, args).sendToTarget();
    }

    private void handleOnRectangleOnScreenRequested(int left, int top, int right, int bottom) {
        float scrollX;
        float scrollY;
        Rect magnifiedFrame = this.mTempRect;
        this.mMagnifiedBounds.getBounds(magnifiedFrame);
        if (magnifiedFrame.intersects(left, top, right, bottom)) {
            Rect magnifFrameInScreenCoords = this.mTempRect1;
            getMagnifiedFrameInContentCoords(magnifFrameInScreenCoords);
            if (right - left > magnifFrameInScreenCoords.width()) {
                int direction = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault());
                if (direction == 0) {
                    scrollX = left - magnifFrameInScreenCoords.left;
                } else {
                    scrollX = right - magnifFrameInScreenCoords.right;
                }
            } else if (left < magnifFrameInScreenCoords.left) {
                scrollX = left - magnifFrameInScreenCoords.left;
            } else if (right > magnifFrameInScreenCoords.right) {
                scrollX = right - magnifFrameInScreenCoords.right;
            } else {
                scrollX = 0.0f;
            }
            if (bottom - top > magnifFrameInScreenCoords.height() || top < magnifFrameInScreenCoords.top) {
                scrollY = top - magnifFrameInScreenCoords.top;
            } else if (bottom > magnifFrameInScreenCoords.bottom) {
                scrollY = bottom - magnifFrameInScreenCoords.bottom;
            } else {
                scrollY = 0.0f;
            }
            float scale = this.mMagnificationController.getScale();
            this.mMagnificationController.offsetMagnifiedRegionCenter(scrollX * scale, scrollY * scale);
        }
    }

    public void onRotationChanged(int rotation) {
        this.mHandler.obtainMessage(4, rotation, 0).sendToTarget();
    }

    private void handleOnRotationChanged(int rotation) {
        resetMagnificationIfNeeded();
        if (this.mMagnificationController.isMagnifying()) {
            this.mUpdateMagnificationSpecOnNextBoundsChange = true;
        }
    }

    public void onUserContextChanged() {
        this.mHandler.sendEmptyMessage(3);
    }

    private void handleOnUserContextChanged() {
        resetMagnificationIfNeeded();
    }

    private void getMagnifiedFrameInContentCoords(Rect rect) {
        MagnificationSpec spec = this.mMagnificationController.getMagnificationSpec();
        this.mMagnifiedBounds.getBounds(rect);
        rect.offset((int) (-spec.offsetX), (int) (-spec.offsetY));
        rect.scale(1.0f / spec.scale);
    }

    private void resetMagnificationIfNeeded() {
        if (this.mMagnificationController.isMagnifying() && isScreenMagnificationAutoUpdateEnabled(this.mContext)) {
            this.mMagnificationController.reset(true);
        }
    }

    @Override
    public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        this.mMagnifiedContentInteractonStateHandler.onMotionEvent(event);
        switch (this.mCurrentState) {
            case 1:
                handleMotionEventStateDelegating(event, rawEvent, policyFlags);
                return;
            case 2:
                this.mDetectingStateHandler.onMotionEvent(event, rawEvent, policyFlags);
                return;
            case 3:
                this.mStateViewportDraggingHandler.onMotionEvent(event, policyFlags);
                return;
            case 4:
                return;
            default:
                throw new IllegalStateException("Unknown state: " + this.mCurrentState);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (this.mNext != null) {
            this.mNext.onAccessibilityEvent(event);
        }
    }

    @Override
    public void setNext(EventStreamTransformation next) {
        this.mNext = next;
    }

    @Override
    public void clear() {
        this.mCurrentState = 2;
        this.mDetectingStateHandler.clear();
        this.mStateViewportDraggingHandler.clear();
        this.mMagnifiedContentInteractonStateHandler.clear();
        if (this.mNext != null) {
            this.mNext.clear();
        }
    }

    @Override
    public void onDestroy() {
        this.mScreenStateObserver.destroy();
        this.mWindowManager.setMagnificationCallbacks((WindowManagerInternal.MagnificationCallbacks) null);
    }

    private void handleMotionEventStateDelegating(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        switch (event.getActionMasked()) {
            case 0:
                this.mDelegatingStateDownTime = event.getDownTime();
                break;
            case 1:
                if (this.mDetectingStateHandler.mDelayedEventQueue == null) {
                    transitionToState(2);
                }
                break;
        }
        if (this.mNext != null) {
            float eventX = event.getX();
            float eventY = event.getY();
            if (this.mMagnificationController.isMagnifying() && this.mMagnifiedBounds.contains((int) eventX, (int) eventY)) {
                float scale = this.mMagnificationController.getScale();
                float scaledOffsetX = this.mMagnificationController.getOffsetX();
                float scaledOffsetY = this.mMagnificationController.getOffsetY();
                int pointerCount = event.getPointerCount();
                MotionEvent.PointerCoords[] coords = getTempPointerCoordsWithMinSize(pointerCount);
                MotionEvent.PointerProperties[] properties = getTempPointerPropertiesWithMinSize(pointerCount);
                for (int i = 0; i < pointerCount; i++) {
                    event.getPointerCoords(i, coords[i]);
                    coords[i].x = (coords[i].x - scaledOffsetX) / scale;
                    coords[i].y = (coords[i].y - scaledOffsetY) / scale;
                    event.getPointerProperties(i, properties[i]);
                }
                event = MotionEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(), pointerCount, properties, coords, 0, 0, 1.0f, 1.0f, event.getDeviceId(), 0, event.getSource(), event.getFlags());
            }
            event.setDownTime(this.mDelegatingStateDownTime);
            this.mNext.onMotionEvent(event, rawEvent, policyFlags);
        }
    }

    private MotionEvent.PointerCoords[] getTempPointerCoordsWithMinSize(int size) {
        int oldSize = this.mTempPointerCoords != null ? this.mTempPointerCoords.length : 0;
        if (oldSize < size) {
            MotionEvent.PointerCoords[] oldTempPointerCoords = this.mTempPointerCoords;
            this.mTempPointerCoords = new MotionEvent.PointerCoords[size];
            if (oldTempPointerCoords != null) {
                System.arraycopy(oldTempPointerCoords, 0, this.mTempPointerCoords, 0, oldSize);
            }
        }
        for (int i = oldSize; i < size; i++) {
            this.mTempPointerCoords[i] = new MotionEvent.PointerCoords();
        }
        return this.mTempPointerCoords;
    }

    private MotionEvent.PointerProperties[] getTempPointerPropertiesWithMinSize(int size) {
        int oldSize = this.mTempPointerProperties != null ? this.mTempPointerProperties.length : 0;
        if (oldSize < size) {
            MotionEvent.PointerProperties[] oldTempPointerProperties = this.mTempPointerProperties;
            this.mTempPointerProperties = new MotionEvent.PointerProperties[size];
            if (oldTempPointerProperties != null) {
                System.arraycopy(oldTempPointerProperties, 0, this.mTempPointerProperties, 0, oldSize);
            }
        }
        for (int i = oldSize; i < size; i++) {
            this.mTempPointerProperties[i] = new MotionEvent.PointerProperties();
        }
        return this.mTempPointerProperties;
    }

    private void transitionToState(int state) {
        this.mPreviousState = this.mCurrentState;
        this.mCurrentState = state;
    }

    private final class MagnifiedContentInteractonStateHandler extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener {
        private static final float MAX_SCALE = 5.0f;
        private static final float MIN_SCALE = 1.3f;
        private static final float SCALING_THRESHOLD = 0.3f;
        private final GestureDetector mGestureDetector;
        private float mInitialScaleFactor = -1.0f;
        private final ScaleGestureDetector mScaleGestureDetector;
        private boolean mScaling;

        public MagnifiedContentInteractonStateHandler(Context context) {
            this.mScaleGestureDetector = new ScaleGestureDetector(context, this);
            this.mScaleGestureDetector.setQuickScaleEnabled(false);
            this.mGestureDetector = new GestureDetector(context, this);
        }

        public void onMotionEvent(MotionEvent event) {
            this.mScaleGestureDetector.onTouchEvent(event);
            this.mGestureDetector.onTouchEvent(event);
            if (ScreenMagnifier.this.mCurrentState == 4 && event.getActionMasked() == 1) {
                clear();
                float scale = Math.min(Math.max(ScreenMagnifier.this.mMagnificationController.getScale(), MIN_SCALE), MAX_SCALE);
                if (scale != ScreenMagnifier.this.getPersistedScale()) {
                    ScreenMagnifier.this.persistScale(scale);
                }
                if (ScreenMagnifier.this.mPreviousState == 3) {
                    ScreenMagnifier.this.transitionToState(3);
                } else {
                    ScreenMagnifier.this.transitionToState(2);
                }
            }
        }

        @Override
        public boolean onScroll(MotionEvent first, MotionEvent second, float distanceX, float distanceY) {
            if (ScreenMagnifier.this.mCurrentState == 4) {
                ScreenMagnifier.this.mMagnificationController.offsetMagnifiedRegionCenter(distanceX, distanceY);
            }
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (this.mScaling) {
                float newScale = ScreenMagnifier.this.mMagnificationController.getScale() * detector.getScaleFactor();
                float normalizedNewScale = Math.min(Math.max(newScale, MIN_SCALE), MAX_SCALE);
                ScreenMagnifier.this.mMagnificationController.setScale(normalizedNewScale, detector.getFocusX(), detector.getFocusY(), false);
                return true;
            }
            if (this.mInitialScaleFactor < 0.0f) {
                this.mInitialScaleFactor = detector.getScaleFactor();
            } else {
                float deltaScale = detector.getScaleFactor() - this.mInitialScaleFactor;
                if (Math.abs(deltaScale) > SCALING_THRESHOLD) {
                    this.mScaling = true;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return ScreenMagnifier.this.mCurrentState == 4;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            clear();
        }

        private void clear() {
            this.mInitialScaleFactor = -1.0f;
            this.mScaling = false;
        }
    }

    private final class StateViewportDraggingHandler {
        private boolean mLastMoveOutsideMagnifiedRegion;

        private StateViewportDraggingHandler() {
        }

        private void onMotionEvent(MotionEvent event, int policyFlags) {
            int action = event.getActionMasked();
            switch (action) {
                case 0:
                    throw new IllegalArgumentException("Unexpected event type: ACTION_DOWN");
                case 1:
                    if (!ScreenMagnifier.this.mTranslationEnabledBeforePan) {
                        ScreenMagnifier.this.mMagnificationController.reset(true);
                    }
                    clear();
                    ScreenMagnifier.this.transitionToState(2);
                    return;
                case 2:
                    if (event.getPointerCount() != 1) {
                        throw new IllegalStateException("Should have one pointer down.");
                    }
                    float eventX = event.getX();
                    float eventY = event.getY();
                    if (ScreenMagnifier.this.mMagnifiedBounds.contains((int) eventX, (int) eventY)) {
                        if (!this.mLastMoveOutsideMagnifiedRegion) {
                            ScreenMagnifier.this.mMagnificationController.setMagnifiedRegionCenter(eventX, eventY, false);
                            return;
                        } else {
                            this.mLastMoveOutsideMagnifiedRegion = false;
                            ScreenMagnifier.this.mMagnificationController.setMagnifiedRegionCenter(eventX, eventY, true);
                            return;
                        }
                    }
                    this.mLastMoveOutsideMagnifiedRegion = true;
                    return;
                case 3:
                case 4:
                default:
                    return;
                case 5:
                    clear();
                    ScreenMagnifier.this.transitionToState(4);
                    return;
                case 6:
                    throw new IllegalArgumentException("Unexpected event type: ACTION_POINTER_UP");
            }
        }

        public void clear() {
            this.mLastMoveOutsideMagnifiedRegion = false;
        }
    }

    private final class DetectingStateHandler {
        private static final int ACTION_TAP_COUNT = 3;
        private static final int MESSAGE_ON_ACTION_TAP_AND_HOLD = 1;
        private static final int MESSAGE_TRANSITION_TO_DELEGATING_STATE = 2;
        private MotionEventInfo mDelayedEventQueue;
        private final Handler mHandler;
        private MotionEvent mLastDownEvent;
        private MotionEvent mLastTapUpEvent;
        private int mTapCount;

        private DetectingStateHandler() {
            this.mHandler = new Handler() {
                @Override
                public void handleMessage(Message message) {
                    int type = message.what;
                    switch (type) {
                        case 1:
                            MotionEvent event = (MotionEvent) message.obj;
                            int policyFlags = message.arg1;
                            DetectingStateHandler.this.onActionTapAndHold(event, policyFlags);
                            return;
                        case 2:
                            ScreenMagnifier.this.transitionToState(1);
                            DetectingStateHandler.this.sendDelayedMotionEvents();
                            DetectingStateHandler.this.clear();
                            return;
                        default:
                            throw new IllegalArgumentException("Unknown message type: " + type);
                    }
                }
            };
        }

        public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            cacheDelayedMotionEvent(event, rawEvent, policyFlags);
            int action = event.getActionMasked();
            switch (action) {
                case 0:
                    this.mHandler.removeMessages(2);
                    if (!ScreenMagnifier.this.mMagnifiedBounds.contains((int) event.getX(), (int) event.getY())) {
                        transitionToDelegatingStateAndClear();
                    } else {
                        if (this.mTapCount == 2 && this.mLastDownEvent != null && GestureUtils.isMultiTap(this.mLastDownEvent, event, ScreenMagnifier.this.mMultiTapTimeSlop, ScreenMagnifier.this.mMultiTapDistanceSlop, 0)) {
                            Message message = this.mHandler.obtainMessage(1, policyFlags, 0, event);
                            this.mHandler.sendMessageDelayed(message, ViewConfiguration.getLongPressTimeout());
                        } else if (this.mTapCount < 3) {
                            Message message2 = this.mHandler.obtainMessage(2);
                            this.mHandler.sendMessageDelayed(message2, ScreenMagnifier.this.mMultiTapTimeSlop);
                        }
                        clearLastDownEvent();
                        this.mLastDownEvent = MotionEvent.obtain(event);
                    }
                    break;
                case 1:
                    if (this.mLastDownEvent != null) {
                        this.mHandler.removeMessages(1);
                        if (ScreenMagnifier.this.mMagnifiedBounds.contains((int) event.getX(), (int) event.getY())) {
                            if (!GestureUtils.isTap(this.mLastDownEvent, event, ScreenMagnifier.this.mTapTimeSlop, ScreenMagnifier.this.mTapDistanceSlop, 0)) {
                                transitionToDelegatingStateAndClear();
                            } else if (this.mLastTapUpEvent != null && !GestureUtils.isMultiTap(this.mLastTapUpEvent, event, ScreenMagnifier.this.mMultiTapTimeSlop, ScreenMagnifier.this.mMultiTapDistanceSlop, 0)) {
                                transitionToDelegatingStateAndClear();
                            } else {
                                this.mTapCount++;
                                if (this.mTapCount == 3) {
                                    clear();
                                    onActionTap(event, policyFlags);
                                } else {
                                    clearLastTapUpEvent();
                                    this.mLastTapUpEvent = MotionEvent.obtain(event);
                                }
                            }
                        } else {
                            transitionToDelegatingStateAndClear();
                        }
                    }
                    break;
                case 2:
                    if (this.mLastDownEvent != null && this.mTapCount < 2) {
                        double distance = GestureUtils.computeDistance(this.mLastDownEvent, event, 0);
                        if (Math.abs(distance) > ScreenMagnifier.this.mTapDistanceSlop) {
                            transitionToDelegatingStateAndClear();
                        }
                        break;
                    }
                    break;
                case 5:
                    if (ScreenMagnifier.this.mMagnificationController.isMagnifying()) {
                        ScreenMagnifier.this.transitionToState(4);
                        clear();
                    } else {
                        transitionToDelegatingStateAndClear();
                    }
                    break;
            }
        }

        public void clear() {
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(2);
            clearTapDetectionState();
            clearDelayedMotionEvents();
        }

        private void clearTapDetectionState() {
            this.mTapCount = 0;
            clearLastTapUpEvent();
            clearLastDownEvent();
        }

        private void clearLastTapUpEvent() {
            if (this.mLastTapUpEvent != null) {
                this.mLastTapUpEvent.recycle();
                this.mLastTapUpEvent = null;
            }
        }

        private void clearLastDownEvent() {
            if (this.mLastDownEvent != null) {
                this.mLastDownEvent.recycle();
                this.mLastDownEvent = null;
            }
        }

        private void cacheDelayedMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            MotionEventInfo info = MotionEventInfo.obtain(event, rawEvent, policyFlags);
            if (this.mDelayedEventQueue == null) {
                this.mDelayedEventQueue = info;
                return;
            }
            MotionEventInfo tail = this.mDelayedEventQueue;
            while (tail.mNext != null) {
                tail = tail.mNext;
            }
            tail.mNext = info;
        }

        private void sendDelayedMotionEvents() {
            while (this.mDelayedEventQueue != null) {
                MotionEventInfo info = this.mDelayedEventQueue;
                this.mDelayedEventQueue = info.mNext;
                long offset = SystemClock.uptimeMillis() - info.mCachedTimeMillis;
                MotionEvent event = obtainEventWithOffsetTimeAndDownTime(info.mEvent, offset);
                MotionEvent rawEvent = obtainEventWithOffsetTimeAndDownTime(info.mRawEvent, offset);
                ScreenMagnifier.this.onMotionEvent(event, rawEvent, info.mPolicyFlags);
                event.recycle();
                rawEvent.recycle();
                info.recycle();
            }
        }

        private MotionEvent obtainEventWithOffsetTimeAndDownTime(MotionEvent event, long offset) {
            int pointerCount = event.getPointerCount();
            MotionEvent.PointerCoords[] coords = ScreenMagnifier.this.getTempPointerCoordsWithMinSize(pointerCount);
            MotionEvent.PointerProperties[] properties = ScreenMagnifier.this.getTempPointerPropertiesWithMinSize(pointerCount);
            for (int i = 0; i < pointerCount; i++) {
                event.getPointerCoords(i, coords[i]);
                event.getPointerProperties(i, properties[i]);
            }
            long downTime = event.getDownTime() + offset;
            long eventTime = event.getEventTime() + offset;
            return MotionEvent.obtain(downTime, eventTime, event.getAction(), pointerCount, properties, coords, event.getMetaState(), event.getButtonState(), 1.0f, 1.0f, event.getDeviceId(), event.getEdgeFlags(), event.getSource(), event.getFlags());
        }

        private void clearDelayedMotionEvents() {
            while (this.mDelayedEventQueue != null) {
                MotionEventInfo info = this.mDelayedEventQueue;
                this.mDelayedEventQueue = info.mNext;
                info.recycle();
            }
        }

        private void transitionToDelegatingStateAndClear() {
            ScreenMagnifier.this.transitionToState(1);
            sendDelayedMotionEvents();
            clear();
        }

        private void onActionTap(MotionEvent up, int policyFlags) {
            if (!ScreenMagnifier.this.mMagnificationController.isMagnifying()) {
                ScreenMagnifier.this.mMagnificationController.setScaleAndMagnifiedRegionCenter(ScreenMagnifier.this.getPersistedScale(), up.getX(), up.getY(), true);
            } else {
                ScreenMagnifier.this.mMagnificationController.reset(true);
            }
        }

        private void onActionTapAndHold(MotionEvent down, int policyFlags) {
            clear();
            ScreenMagnifier.this.mTranslationEnabledBeforePan = ScreenMagnifier.this.mMagnificationController.isMagnifying();
            ScreenMagnifier.this.mMagnificationController.setScaleAndMagnifiedRegionCenter(ScreenMagnifier.this.getPersistedScale(), down.getX(), down.getY(), true);
            ScreenMagnifier.this.transitionToState(3);
        }
    }

    private void persistScale(final float scale) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Settings.Secure.putFloat(ScreenMagnifier.this.mContext.getContentResolver(), "accessibility_display_magnification_scale", scale);
                return null;
            }
        }.execute(new Void[0]);
    }

    private float getPersistedScale() {
        return Settings.Secure.getFloat(this.mContext.getContentResolver(), "accessibility_display_magnification_scale", DEFAULT_MAGNIFICATION_SCALE);
    }

    private static boolean isScreenMagnificationAutoUpdateEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "accessibility_display_magnification_auto_update", 1) == 1;
    }

    private static final class MotionEventInfo {
        private static final int MAX_POOL_SIZE = 10;
        private static final Object sLock = new Object();
        private static MotionEventInfo sPool;
        private static int sPoolSize;
        public long mCachedTimeMillis;
        public MotionEvent mEvent;
        private boolean mInPool;
        private MotionEventInfo mNext;
        public int mPolicyFlags;
        public MotionEvent mRawEvent;

        private MotionEventInfo() {
        }

        public static MotionEventInfo obtain(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            MotionEventInfo info;
            synchronized (sLock) {
                if (sPoolSize > 0) {
                    sPoolSize--;
                    info = sPool;
                    sPool = info.mNext;
                    info.mNext = null;
                    info.mInPool = false;
                } else {
                    info = new MotionEventInfo();
                }
                info.initialize(event, rawEvent, policyFlags);
            }
            return info;
        }

        private void initialize(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            this.mEvent = MotionEvent.obtain(event);
            this.mRawEvent = MotionEvent.obtain(rawEvent);
            this.mPolicyFlags = policyFlags;
            this.mCachedTimeMillis = SystemClock.uptimeMillis();
        }

        public void recycle() {
            synchronized (sLock) {
                if (this.mInPool) {
                    throw new IllegalStateException("Already recycled.");
                }
                clear();
                if (sPoolSize < 10) {
                    sPoolSize++;
                    this.mNext = sPool;
                    sPool = this;
                    this.mInPool = true;
                }
            }
        }

        private void clear() {
            this.mEvent.recycle();
            this.mEvent = null;
            this.mRawEvent.recycle();
            this.mRawEvent = null;
            this.mPolicyFlags = 0;
            this.mCachedTimeMillis = 0L;
        }
    }

    private final class MagnificationController {
        private static final String PROPERTY_NAME_MAGNIFICATION_SPEC = "magnificationSpec";
        private final ValueAnimator mTransformationAnimator;
        private final MagnificationSpec mSentMagnificationSpec = MagnificationSpec.obtain();
        private final MagnificationSpec mCurrentMagnificationSpec = MagnificationSpec.obtain();
        private final Rect mTempRect = new Rect();

        public MagnificationController(long animationDuration) {
            Property<MagnificationController, MagnificationSpec> property = Property.of(MagnificationController.class, MagnificationSpec.class, PROPERTY_NAME_MAGNIFICATION_SPEC);
            TypeEvaluator<MagnificationSpec> evaluator = new TypeEvaluator<MagnificationSpec>() {
                private final MagnificationSpec mTempTransformationSpec = MagnificationSpec.obtain();

                @Override
                public MagnificationSpec evaluate(float fraction, MagnificationSpec fromSpec, MagnificationSpec toSpec) {
                    MagnificationSpec result = this.mTempTransformationSpec;
                    result.scale = fromSpec.scale + ((toSpec.scale - fromSpec.scale) * fraction);
                    result.offsetX = fromSpec.offsetX + ((toSpec.offsetX - fromSpec.offsetX) * fraction);
                    result.offsetY = fromSpec.offsetY + ((toSpec.offsetY - fromSpec.offsetY) * fraction);
                    return result;
                }
            };
            this.mTransformationAnimator = ObjectAnimator.ofObject(this, (Property<MagnificationController, V>) property, (TypeEvaluator) evaluator, (Object[]) new MagnificationSpec[]{this.mSentMagnificationSpec, this.mCurrentMagnificationSpec});
            this.mTransformationAnimator.setDuration(animationDuration);
            this.mTransformationAnimator.setInterpolator(new DecelerateInterpolator(2.5f));
        }

        public boolean isMagnifying() {
            return this.mCurrentMagnificationSpec.scale > 1.0f;
        }

        public void reset(boolean animate) {
            if (this.mTransformationAnimator.isRunning()) {
                this.mTransformationAnimator.cancel();
            }
            this.mCurrentMagnificationSpec.clear();
            if (animate) {
                animateMangificationSpec(this.mSentMagnificationSpec, this.mCurrentMagnificationSpec);
            } else {
                setMagnificationSpec(this.mCurrentMagnificationSpec);
            }
            Rect bounds = this.mTempRect;
            bounds.setEmpty();
            ScreenMagnifier.this.mAms.onMagnificationStateChanged();
        }

        public float getScale() {
            return this.mCurrentMagnificationSpec.scale;
        }

        public float getOffsetX() {
            return this.mCurrentMagnificationSpec.offsetX;
        }

        public float getOffsetY() {
            return this.mCurrentMagnificationSpec.offsetY;
        }

        public void setScale(float scale, float pivotX, float pivotY, boolean animate) {
            Rect magnifiedFrame = this.mTempRect;
            ScreenMagnifier.this.mMagnifiedBounds.getBounds(magnifiedFrame);
            MagnificationSpec spec = this.mCurrentMagnificationSpec;
            float oldScale = spec.scale;
            float oldCenterX = ((-spec.offsetX) + (magnifiedFrame.width() / 2)) / oldScale;
            float oldCenterY = ((-spec.offsetY) + (magnifiedFrame.height() / 2)) / oldScale;
            float normPivotX = ((-spec.offsetX) + pivotX) / oldScale;
            float normPivotY = ((-spec.offsetY) + pivotY) / oldScale;
            float offsetX = (oldCenterX - normPivotX) * (oldScale / scale);
            float offsetY = (oldCenterY - normPivotY) * (oldScale / scale);
            float centerX = normPivotX + offsetX;
            float centerY = normPivotY + offsetY;
            setScaleAndMagnifiedRegionCenter(scale, centerX, centerY, animate);
        }

        public void setMagnifiedRegionCenter(float centerX, float centerY, boolean animate) {
            setScaleAndMagnifiedRegionCenter(this.mCurrentMagnificationSpec.scale, centerX, centerY, animate);
        }

        public void offsetMagnifiedRegionCenter(float offsetX, float offsetY) {
            float nonNormOffsetX = this.mCurrentMagnificationSpec.offsetX - offsetX;
            this.mCurrentMagnificationSpec.offsetX = Math.min(Math.max(nonNormOffsetX, getMinOffsetX()), 0.0f);
            float nonNormOffsetY = this.mCurrentMagnificationSpec.offsetY - offsetY;
            this.mCurrentMagnificationSpec.offsetY = Math.min(Math.max(nonNormOffsetY, getMinOffsetY()), 0.0f);
            setMagnificationSpec(this.mCurrentMagnificationSpec);
        }

        public void setScaleAndMagnifiedRegionCenter(float scale, float centerX, float centerY, boolean animate) {
            if (Float.compare(this.mCurrentMagnificationSpec.scale, scale) != 0 || Float.compare(this.mCurrentMagnificationSpec.offsetX, centerX) != 0 || Float.compare(this.mCurrentMagnificationSpec.offsetY, centerY) != 0) {
                if (this.mTransformationAnimator.isRunning()) {
                    this.mTransformationAnimator.cancel();
                }
                updateMagnificationSpec(scale, centerX, centerY);
                if (animate) {
                    animateMangificationSpec(this.mSentMagnificationSpec, this.mCurrentMagnificationSpec);
                } else {
                    setMagnificationSpec(this.mCurrentMagnificationSpec);
                }
                ScreenMagnifier.this.mAms.onMagnificationStateChanged();
            }
        }

        public void updateMagnificationSpec(float scale, float magnifiedCenterX, float magnifiedCenterY) {
            Rect magnifiedFrame = this.mTempRect;
            ScreenMagnifier.this.mMagnifiedBounds.getBounds(magnifiedFrame);
            this.mCurrentMagnificationSpec.scale = scale;
            int viewportWidth = magnifiedFrame.width();
            float nonNormOffsetX = (viewportWidth / 2) - (magnifiedCenterX * scale);
            this.mCurrentMagnificationSpec.offsetX = Math.min(Math.max(nonNormOffsetX, getMinOffsetX()), 0.0f);
            int viewportHeight = magnifiedFrame.height();
            float nonNormOffsetY = (viewportHeight / 2) - (magnifiedCenterY * scale);
            this.mCurrentMagnificationSpec.offsetY = Math.min(Math.max(nonNormOffsetY, getMinOffsetY()), 0.0f);
        }

        private float getMinOffsetX() {
            Rect magnifiedFrame = this.mTempRect;
            ScreenMagnifier.this.mMagnifiedBounds.getBounds(magnifiedFrame);
            float viewportWidth = magnifiedFrame.width();
            return viewportWidth - (this.mCurrentMagnificationSpec.scale * viewportWidth);
        }

        private float getMinOffsetY() {
            Rect magnifiedFrame = this.mTempRect;
            ScreenMagnifier.this.mMagnifiedBounds.getBounds(magnifiedFrame);
            float viewportHeight = magnifiedFrame.height();
            return viewportHeight - (this.mCurrentMagnificationSpec.scale * viewportHeight);
        }

        private void animateMangificationSpec(MagnificationSpec fromSpec, MagnificationSpec toSpec) {
            this.mTransformationAnimator.setObjectValues(fromSpec, toSpec);
            this.mTransformationAnimator.start();
        }

        public MagnificationSpec getMagnificationSpec() {
            return this.mSentMagnificationSpec;
        }

        public void setMagnificationSpec(MagnificationSpec spec) {
            this.mSentMagnificationSpec.scale = spec.scale;
            this.mSentMagnificationSpec.offsetX = spec.offsetX;
            this.mSentMagnificationSpec.offsetY = spec.offsetY;
            ScreenMagnifier.this.mWindowManager.setMagnificationSpec(MagnificationSpec.obtain(spec));
        }
    }

    private final class ScreenStateObserver extends BroadcastReceiver {
        private static final int MESSAGE_ON_SCREEN_STATE_CHANGE = 1;
        private final Context mContext;
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        String action = (String) message.obj;
                        ScreenStateObserver.this.handleOnScreenStateChange(action);
                        break;
                }
            }
        };
        private final MagnificationController mMagnificationController;

        public ScreenStateObserver(Context context, MagnificationController magnificationController) {
            this.mContext = context;
            this.mMagnificationController = magnificationController;
            this.mContext.registerReceiver(this, new IntentFilter("android.intent.action.SCREEN_OFF"));
        }

        public void destroy() {
            this.mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.mHandler.obtainMessage(1, intent.getAction()).sendToTarget();
        }

        private void handleOnScreenStateChange(String action) {
            if (this.mMagnificationController.isMagnifying() && ScreenMagnifier.isScreenMagnificationAutoUpdateEnabled(this.mContext)) {
                this.mMagnificationController.reset(false);
            }
        }
    }
}
