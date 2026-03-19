package com.android.server.accessibility;

import android.R;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.MathUtils;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;

class MagnificationGestureHandler implements EventStreamTransformation {
    private static final boolean DEBUG_DETECTING = false;
    private static final boolean DEBUG_PANNING = false;
    private static final boolean DEBUG_STATE_TRANSITIONS = false;
    private static final String LOG_TAG = "MagnificationEventHandler";
    private static final float MAX_SCALE = 5.0f;
    private static final float MIN_SCALE = 2.0f;
    private static final int STATE_DELEGATING = 1;
    private static final int STATE_DETECTING = 2;
    private static final int STATE_MAGNIFIED_INTERACTION = 4;
    private static final int STATE_VIEWPORT_DRAGGING = 3;
    private int mCurrentState;
    private long mDelegatingStateDownTime;
    private final boolean mDetectControlGestures;
    private final DetectingStateHandler mDetectingStateHandler;
    private final MagnificationController mMagnificationController;
    private final MagnifiedContentInteractionStateHandler mMagnifiedContentInteractionStateHandler;
    private EventStreamTransformation mNext;
    private int mPreviousState;
    private final StateViewportDraggingHandler mStateViewportDraggingHandler = new StateViewportDraggingHandler(this, null);
    private MotionEvent.PointerCoords[] mTempPointerCoords;
    private MotionEvent.PointerProperties[] mTempPointerProperties;
    private boolean mTranslationEnabledBeforePan;

    private interface MotionEventHandler {
        void clear();

        void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i);
    }

    public MagnificationGestureHandler(Context context, AccessibilityManagerService ams, boolean detectControlGestures) {
        this.mMagnificationController = ams.getMagnificationController();
        this.mDetectingStateHandler = new DetectingStateHandler(context);
        this.mMagnifiedContentInteractionStateHandler = new MagnifiedContentInteractionStateHandler(context);
        this.mDetectControlGestures = detectControlGestures;
        transitionToState(2);
    }

    @Override
    public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (!event.isFromSource(4098)) {
            if (this.mNext != null) {
                this.mNext.onMotionEvent(event, rawEvent, policyFlags);
            }
        } else {
            if (!this.mDetectControlGestures) {
                if (this.mNext != null) {
                    dispatchTransformedEvent(event, rawEvent, policyFlags);
                    return;
                }
                return;
            }
            this.mMagnifiedContentInteractionStateHandler.onMotionEvent(event, rawEvent, policyFlags);
            switch (this.mCurrentState) {
                case 1:
                    handleMotionEventStateDelegating(event, rawEvent, policyFlags);
                    return;
                case 2:
                    this.mDetectingStateHandler.onMotionEvent(event, rawEvent, policyFlags);
                    return;
                case 3:
                    this.mStateViewportDraggingHandler.onMotionEvent(event, rawEvent, policyFlags);
                    return;
                case 4:
                    return;
                default:
                    throw new IllegalStateException("Unknown state: " + this.mCurrentState);
            }
        }
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
        if (inputSource == 4098) {
            clear();
        }
        if (this.mNext == null) {
            return;
        }
        this.mNext.clearEvents(inputSource);
    }

    @Override
    public void onDestroy() {
        clear();
    }

    private void clear() {
        this.mCurrentState = 2;
        this.mDetectingStateHandler.clear();
        this.mStateViewportDraggingHandler.clear();
        this.mMagnifiedContentInteractionStateHandler.clear();
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
        if (this.mNext == null) {
            return;
        }
        event.setDownTime(this.mDelegatingStateDownTime);
        dispatchTransformedEvent(event, rawEvent, policyFlags);
    }

    private void dispatchTransformedEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        float eventX = event.getX();
        float eventY = event.getY();
        if (this.mMagnificationController.isMagnifying() && this.mMagnificationController.magnificationRegionContains(eventX, eventY)) {
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
        this.mNext.onMotionEvent(event, rawEvent, policyFlags);
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

    private final class MagnifiedContentInteractionStateHandler extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener, MotionEventHandler {
        private final GestureDetector mGestureDetector;
        private float mInitialScaleFactor = -1.0f;
        private final ScaleGestureDetector mScaleGestureDetector;
        private boolean mScaling;
        private final float mScalingThreshold;

        public MagnifiedContentInteractionStateHandler(Context context) {
            TypedValue scaleValue = new TypedValue();
            context.getResources().getValue(R.dimen.accessibility_magnification_thumbnail_container_stroke_width, scaleValue, false);
            this.mScalingThreshold = scaleValue.getFloat();
            this.mScaleGestureDetector = new ScaleGestureDetector(context, this);
            this.mScaleGestureDetector.setQuickScaleEnabled(false);
            this.mGestureDetector = new GestureDetector(context, this);
        }

        @Override
        public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            this.mScaleGestureDetector.onTouchEvent(event);
            this.mGestureDetector.onTouchEvent(event);
            if (MagnificationGestureHandler.this.mCurrentState != 4 || event.getActionMasked() != 1) {
                return;
            }
            clear();
            MagnificationGestureHandler.this.mMagnificationController.persistScale();
            if (MagnificationGestureHandler.this.mPreviousState == 3) {
                MagnificationGestureHandler.this.transitionToState(3);
            } else {
                MagnificationGestureHandler.this.transitionToState(2);
            }
        }

        @Override
        public boolean onScroll(MotionEvent first, MotionEvent second, float distanceX, float distanceY) {
            if (MagnificationGestureHandler.this.mCurrentState != 4) {
                return true;
            }
            MagnificationGestureHandler.this.mMagnificationController.offsetMagnifiedRegionCenter(distanceX, distanceY, 0);
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale;
            if (!this.mScaling) {
                if (this.mInitialScaleFactor < 0.0f) {
                    this.mInitialScaleFactor = detector.getScaleFactor();
                } else {
                    float deltaScale = detector.getScaleFactor() - this.mInitialScaleFactor;
                    if (Math.abs(deltaScale) > this.mScalingThreshold) {
                        this.mScaling = true;
                        return true;
                    }
                }
                return false;
            }
            float initialScale = MagnificationGestureHandler.this.mMagnificationController.getScale();
            float targetScale = initialScale * detector.getScaleFactor();
            if (targetScale > MagnificationGestureHandler.MAX_SCALE && targetScale > initialScale) {
                scale = MagnificationGestureHandler.MAX_SCALE;
            } else if (targetScale < MagnificationGestureHandler.MIN_SCALE && targetScale < initialScale) {
                scale = MagnificationGestureHandler.MIN_SCALE;
            } else {
                scale = targetScale;
            }
            float pivotX = detector.getFocusX();
            float pivotY = detector.getFocusY();
            MagnificationGestureHandler.this.mMagnificationController.setScale(scale, pivotX, pivotY, false, 0);
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return MagnificationGestureHandler.this.mCurrentState == 4;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            clear();
        }

        @Override
        public void clear() {
            this.mInitialScaleFactor = -1.0f;
            this.mScaling = false;
        }
    }

    private final class StateViewportDraggingHandler implements MotionEventHandler {
        private boolean mLastMoveOutsideMagnifiedRegion;

        StateViewportDraggingHandler(MagnificationGestureHandler this$0, StateViewportDraggingHandler stateViewportDraggingHandler) {
            this();
        }

        private StateViewportDraggingHandler() {
        }

        @Override
        public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            int action = event.getActionMasked();
            switch (action) {
                case 0:
                    throw new IllegalArgumentException("Unexpected event type: ACTION_DOWN");
                case 1:
                    if (!MagnificationGestureHandler.this.mTranslationEnabledBeforePan) {
                        MagnificationGestureHandler.this.mMagnificationController.reset(true);
                    }
                    clear();
                    MagnificationGestureHandler.this.transitionToState(2);
                    return;
                case 2:
                    if (event.getPointerCount() != 1) {
                        throw new IllegalStateException("Should have one pointer down.");
                    }
                    float eventX = event.getX();
                    float eventY = event.getY();
                    if (MagnificationGestureHandler.this.mMagnificationController.magnificationRegionContains(eventX, eventY)) {
                        if (this.mLastMoveOutsideMagnifiedRegion) {
                            this.mLastMoveOutsideMagnifiedRegion = false;
                            MagnificationGestureHandler.this.mMagnificationController.setCenter(eventX, eventY, true, 0);
                            return;
                        } else {
                            MagnificationGestureHandler.this.mMagnificationController.setCenter(eventX, eventY, false, 0);
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
                    MagnificationGestureHandler.this.transitionToState(4);
                    return;
                case 6:
                    throw new IllegalArgumentException("Unexpected event type: ACTION_POINTER_UP");
            }
        }

        @Override
        public void clear() {
            this.mLastMoveOutsideMagnifiedRegion = false;
        }
    }

    private final class DetectingStateHandler implements MotionEventHandler {
        private static final int ACTION_TAP_COUNT = 3;
        private static final int MESSAGE_ON_ACTION_TAP_AND_HOLD = 1;
        private static final int MESSAGE_TRANSITION_TO_DELEGATING_STATE = 2;
        private MotionEventInfo mDelayedEventQueue;
        private MotionEvent mLastDownEvent;
        private MotionEvent mLastTapUpEvent;
        private final int mMultiTapDistanceSlop;
        private final int mMultiTapTimeSlop;
        private int mTapCount;
        private final int mTapDistanceSlop;
        private final int mTapTimeSlop = ViewConfiguration.getJumpTapTimeout();
        private final Handler mHandler = new Handler() {
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
                        MagnificationGestureHandler.this.transitionToState(1);
                        DetectingStateHandler.this.sendDelayedMotionEvents();
                        DetectingStateHandler.this.clear();
                        return;
                    default:
                        throw new IllegalArgumentException("Unknown message type: " + type);
                }
            }
        };

        public DetectingStateHandler(Context context) {
            this.mMultiTapTimeSlop = ViewConfiguration.getDoubleTapTimeout() + context.getResources().getInteger(R.integer.config_dreamsBatteryLevelMinimumWhenNotPowered);
            this.mTapDistanceSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            this.mMultiTapDistanceSlop = ViewConfiguration.get(context).getScaledDoubleTapSlop();
        }

        @Override
        public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
            cacheDelayedMotionEvent(event, rawEvent, policyFlags);
            int action = event.getActionMasked();
            switch (action) {
                case 0:
                    this.mHandler.removeMessages(2);
                    if (!MagnificationGestureHandler.this.mMagnificationController.magnificationRegionContains(event.getX(), event.getY())) {
                        transitionToDelegatingStateAndClear();
                    } else {
                        if (this.mTapCount == 2 && this.mLastDownEvent != null && GestureUtils.isMultiTap(this.mLastDownEvent, event, this.mMultiTapTimeSlop, this.mMultiTapDistanceSlop, 0)) {
                            Message message = this.mHandler.obtainMessage(1, policyFlags, 0, event);
                            this.mHandler.sendMessageDelayed(message, ViewConfiguration.getLongPressTimeout());
                        } else if (this.mTapCount < 3) {
                            Message message2 = this.mHandler.obtainMessage(2);
                            this.mHandler.sendMessageDelayed(message2, this.mMultiTapTimeSlop);
                        }
                        clearLastDownEvent();
                        this.mLastDownEvent = MotionEvent.obtain(event);
                    }
                    break;
                case 1:
                    if (this.mLastDownEvent != null) {
                        this.mHandler.removeMessages(1);
                        if (!MagnificationGestureHandler.this.mMagnificationController.magnificationRegionContains(event.getX(), event.getY())) {
                            transitionToDelegatingStateAndClear();
                        } else if (!GestureUtils.isTap(this.mLastDownEvent, event, this.mTapTimeSlop, this.mTapDistanceSlop, 0)) {
                            transitionToDelegatingStateAndClear();
                        } else if (this.mLastTapUpEvent != null && !GestureUtils.isMultiTap(this.mLastTapUpEvent, event, this.mMultiTapTimeSlop, this.mMultiTapDistanceSlop, 0)) {
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
                        break;
                    }
                    break;
                case 2:
                    if (this.mLastDownEvent != null && this.mTapCount < 2) {
                        double distance = GestureUtils.computeDistance(this.mLastDownEvent, event, 0);
                        if (Math.abs(distance) > this.mTapDistanceSlop) {
                            transitionToDelegatingStateAndClear();
                        }
                        break;
                    }
                    break;
                case 5:
                    if (MagnificationGestureHandler.this.mMagnificationController.isMagnifying()) {
                        MagnificationGestureHandler.this.transitionToState(4);
                        clear();
                    } else {
                        transitionToDelegatingStateAndClear();
                    }
                    break;
            }
        }

        @Override
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
            if (this.mLastTapUpEvent == null) {
                return;
            }
            this.mLastTapUpEvent.recycle();
            this.mLastTapUpEvent = null;
        }

        private void clearLastDownEvent() {
            if (this.mLastDownEvent == null) {
                return;
            }
            this.mLastDownEvent.recycle();
            this.mLastDownEvent = null;
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
                MagnificationGestureHandler.this.onMotionEvent(info.mEvent, info.mRawEvent, info.mPolicyFlags);
                info.recycle();
            }
        }

        private void clearDelayedMotionEvents() {
            while (this.mDelayedEventQueue != null) {
                MotionEventInfo info = this.mDelayedEventQueue;
                this.mDelayedEventQueue = info.mNext;
                info.recycle();
            }
        }

        private void transitionToDelegatingStateAndClear() {
            MagnificationGestureHandler.this.transitionToState(1);
            sendDelayedMotionEvents();
            clear();
        }

        private void onActionTap(MotionEvent up, int policyFlags) {
            if (!MagnificationGestureHandler.this.mMagnificationController.isMagnifying()) {
                float targetScale = MagnificationGestureHandler.this.mMagnificationController.getPersistedScale();
                float scale = MathUtils.constrain(targetScale, MagnificationGestureHandler.MIN_SCALE, MagnificationGestureHandler.MAX_SCALE);
                MagnificationGestureHandler.this.mMagnificationController.setScaleAndCenter(scale, up.getX(), up.getY(), true, 0);
                return;
            }
            MagnificationGestureHandler.this.mMagnificationController.reset(true);
        }

        private void onActionTapAndHold(MotionEvent down, int policyFlags) {
            clear();
            MagnificationGestureHandler.this.mTranslationEnabledBeforePan = MagnificationGestureHandler.this.mMagnificationController.isMagnifying();
            float targetScale = MagnificationGestureHandler.this.mMagnificationController.getPersistedScale();
            float scale = MathUtils.constrain(targetScale, MagnificationGestureHandler.MIN_SCALE, MagnificationGestureHandler.MAX_SCALE);
            MagnificationGestureHandler.this.mMagnificationController.setScaleAndCenter(scale, down.getX(), down.getY(), true, 0);
            MagnificationGestureHandler.this.transitionToState(3);
        }
    }

    private static final class MotionEventInfo {
        private static final int MAX_POOL_SIZE = 10;
        private static final Object sLock = new Object();
        private static MotionEventInfo sPool;
        private static int sPoolSize;
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
        }
    }
}
