package com.android.server.accessibility;

import android.R;
import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GesturePoint;
import android.gesture.GestureStroke;
import android.gesture.Prediction;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TouchExplorer implements EventStreamTransformation {
    private static final int ALL_POINTER_ID_BITS = -1;
    private static final int CLICK_LOCATION_ACCESSIBILITY_FOCUS = 1;
    private static final int CLICK_LOCATION_LAST_TOUCH_EXPLORED = 2;
    private static final int CLICK_LOCATION_NONE = 0;
    private static final boolean DEBUG = false;
    private static final int EXIT_GESTURE_DETECTION_TIMEOUT = 2000;
    private static final int GESTURE_DETECTION_VELOCITY_DIP = 1000;
    private static final int INVALID_POINTER_ID = -1;
    private static final String LOG_TAG = "TouchExplorer";
    private static final float MAX_DRAGGING_ANGLE_COS = 0.52532196f;
    private static final int MAX_POINTER_COUNT = 32;
    private static final int MIN_POINTER_DISTANCE_TO_USE_MIDDLE_LOCATION_DIP = 200;
    private static final float MIN_PREDICTION_SCORE = 2.0f;
    private static final int STATE_DELEGATING = 4;
    private static final int STATE_DRAGGING = 2;
    private static final int STATE_GESTURE_DETECTING = 5;
    private static final int STATE_TOUCH_EXPLORING = 1;
    private static final int TOUCH_TOLERANCE = 3;
    private final AccessibilityManagerService mAms;
    private final Context mContext;
    private final DoubleTapDetector mDoubleTapDetector;
    private final int mDoubleTapSlop;
    private int mDraggingPointerId;
    private final ExitGestureDetectionModeDelayed mExitGestureDetectionModeDelayed;
    private GestureLibrary mGestureLibrary;
    private final Handler mHandler;
    private int mLastTouchedWindowId;
    private int mLongPressingPointerDeltaX;
    private int mLongPressingPointerDeltaY;
    private EventStreamTransformation mNext;
    private final PerformLongPressDelayed mPerformLongPressDelayed;
    private float mPreviousX;
    private float mPreviousY;
    private final int mScaledGestureDetectionVelocity;
    private final int mScaledMinPointerDistanceToUseMiddleLocation;
    private final SendHoverEnterAndMoveDelayed mSendHoverEnterAndMoveDelayed;
    private final SendHoverExitDelayed mSendHoverExitDelayed;
    private final SendAccessibilityEventDelayed mSendTouchExplorationEndDelayed;
    private final SendAccessibilityEventDelayed mSendTouchInteractionEndDelayed;
    private boolean mTouchExplorationInProgress;
    private final int mTouchSlop;
    private int mCurrentState = 1;
    private final VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private final Rect mTempRect = new Rect();
    private final Point mTempPoint = new Point();
    private final ArrayList<GesturePoint> mStrokeBuffer = new ArrayList<>(100);
    private int mLongPressingPointerId = -1;
    private final ReceivedPointerTracker mReceivedPointerTracker = new ReceivedPointerTracker();
    private final InjectedPointerTracker mInjectedPointerTracker = new InjectedPointerTracker();
    private final int mTapTimeout = ViewConfiguration.getTapTimeout();
    private final int mDetermineUserIntentTimeout = ViewConfiguration.getDoubleTapTimeout();
    private final int mDoubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();

    public TouchExplorer(Context context, AccessibilityManagerService service) {
        this.mContext = context;
        this.mAms = service;
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mDoubleTapSlop = ViewConfiguration.get(context).getScaledDoubleTapSlop();
        this.mHandler = new Handler(context.getMainLooper());
        this.mPerformLongPressDelayed = new PerformLongPressDelayed();
        this.mExitGestureDetectionModeDelayed = new ExitGestureDetectionModeDelayed();
        this.mGestureLibrary = GestureLibraries.fromRawResource(context, R.raw.color_fade_frag);
        this.mGestureLibrary.setOrientationStyle(8);
        this.mGestureLibrary.setSequenceType(2);
        this.mGestureLibrary.load();
        this.mSendHoverEnterAndMoveDelayed = new SendHoverEnterAndMoveDelayed();
        this.mSendHoverExitDelayed = new SendHoverExitDelayed();
        this.mSendTouchExplorationEndDelayed = new SendAccessibilityEventDelayed(1024, this.mDetermineUserIntentTimeout);
        this.mSendTouchInteractionEndDelayed = new SendAccessibilityEventDelayed(2097152, this.mDetermineUserIntentTimeout);
        this.mDoubleTapDetector = new DoubleTapDetector();
        float density = context.getResources().getDisplayMetrics().density;
        this.mScaledMinPointerDistanceToUseMiddleLocation = (int) (200.0f * density);
        this.mScaledGestureDetectionVelocity = (int) (1000.0f * density);
    }

    @Override
    public void clear() {
        MotionEvent event = this.mReceivedPointerTracker.getLastReceivedEvent();
        if (event != null) {
            clear(this.mReceivedPointerTracker.getLastReceivedEvent(), 33554432);
        }
    }

    @Override
    public void onDestroy() {
    }

    private void clear(MotionEvent event, int policyFlags) {
        switch (this.mCurrentState) {
            case 1:
                sendHoverExitAndTouchExplorationGestureEndIfNeeded(policyFlags);
                break;
            case 2:
                this.mDraggingPointerId = -1;
                sendUpForInjectedDownPointers(event, policyFlags);
                break;
            case 4:
                sendUpForInjectedDownPointers(event, policyFlags);
                break;
            case 5:
                this.mStrokeBuffer.clear();
                break;
        }
        this.mSendHoverEnterAndMoveDelayed.cancel();
        this.mSendHoverExitDelayed.cancel();
        this.mPerformLongPressDelayed.cancel();
        this.mExitGestureDetectionModeDelayed.cancel();
        this.mSendTouchExplorationEndDelayed.cancel();
        this.mSendTouchInteractionEndDelayed.cancel();
        this.mReceivedPointerTracker.clear();
        this.mInjectedPointerTracker.clear();
        this.mDoubleTapDetector.clear();
        this.mLongPressingPointerId = -1;
        this.mLongPressingPointerDeltaX = 0;
        this.mLongPressingPointerDeltaY = 0;
        this.mCurrentState = 1;
        if (this.mNext != null) {
            this.mNext.clear();
        }
        this.mTouchExplorationInProgress = DEBUG;
        this.mAms.onTouchInteractionEnd();
    }

    @Override
    public void setNext(EventStreamTransformation next) {
        this.mNext = next;
    }

    @Override
    public void onMotionEvent(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        this.mReceivedPointerTracker.onMotionEvent(rawEvent);
        switch (this.mCurrentState) {
            case 1:
                handleMotionEventStateTouchExploring(event, rawEvent, policyFlags);
                return;
            case 2:
                handleMotionEventStateDragging(event, policyFlags);
                return;
            case 3:
            default:
                throw new IllegalStateException("Illegal state: " + this.mCurrentState);
            case 4:
                handleMotionEventStateDelegating(event, policyFlags);
                return;
            case 5:
                handleMotionEventGestureDetecting(rawEvent, policyFlags);
                return;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        if (this.mSendTouchExplorationEndDelayed.isPending() && eventType == 256) {
            this.mSendTouchExplorationEndDelayed.cancel();
            sendAccessibilityEvent(1024);
        }
        if (this.mSendTouchInteractionEndDelayed.isPending() && eventType == 256) {
            this.mSendTouchInteractionEndDelayed.cancel();
            sendAccessibilityEvent(2097152);
        }
        switch (eventType) {
            case 32:
            case 32768:
                if (this.mInjectedPointerTracker.mLastInjectedHoverEventForClick != null) {
                    this.mInjectedPointerTracker.mLastInjectedHoverEventForClick.recycle();
                    this.mInjectedPointerTracker.mLastInjectedHoverEventForClick = null;
                }
                this.mLastTouchedWindowId = -1;
                break;
            case 128:
            case PackageManagerService.DumpState.DUMP_VERIFIERS:
                this.mLastTouchedWindowId = event.getWindowId();
                break;
        }
        if (this.mNext != null) {
            this.mNext.onAccessibilityEvent(event);
        }
    }

    private void handleMotionEventStateTouchExploring(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        ReceivedPointerTracker receivedTracker = this.mReceivedPointerTracker;
        this.mVelocityTracker.addMovement(rawEvent);
        this.mDoubleTapDetector.onMotionEvent(event, policyFlags);
        switch (event.getActionMasked()) {
            case 0:
                this.mAms.onTouchInteractionStart();
                handleMotionEventGestureDetecting(rawEvent, policyFlags);
                sendAccessibilityEvent(1048576);
                this.mSendHoverEnterAndMoveDelayed.cancel();
                this.mSendHoverExitDelayed.cancel();
                this.mPerformLongPressDelayed.cancel();
                if (this.mSendTouchExplorationEndDelayed.isPending()) {
                    this.mSendTouchExplorationEndDelayed.forceSendAndRemove();
                }
                if (this.mSendTouchInteractionEndDelayed.isPending()) {
                    this.mSendTouchInteractionEndDelayed.forceSendAndRemove();
                }
                if (this.mDoubleTapDetector.firstTapDetected()) {
                    this.mPerformLongPressDelayed.post(event, policyFlags);
                } else if (!this.mTouchExplorationInProgress) {
                    if (!this.mSendHoverEnterAndMoveDelayed.isPending()) {
                        int pointerIdBits = 1 << receivedTracker.getPrimaryPointerId();
                        this.mSendHoverEnterAndMoveDelayed.post(event, true, pointerIdBits, policyFlags);
                    } else {
                        this.mSendHoverEnterAndMoveDelayed.addEvent(event);
                    }
                }
                break;
            case 1:
                this.mAms.onTouchInteractionEnd();
                this.mStrokeBuffer.clear();
                int pointerIdBits2 = 1 << event.getPointerId(event.getActionIndex());
                this.mPerformLongPressDelayed.cancel();
                this.mVelocityTracker.clear();
                if (this.mSendHoverEnterAndMoveDelayed.isPending()) {
                    this.mSendHoverExitDelayed.post(event, pointerIdBits2, policyFlags);
                } else {
                    sendHoverExitAndTouchExplorationGestureEndIfNeeded(policyFlags);
                }
                if (!this.mSendTouchInteractionEndDelayed.isPending()) {
                    this.mSendTouchInteractionEndDelayed.post();
                }
                break;
            case 2:
                int pointerId = receivedTracker.getPrimaryPointerId();
                int pointerIndex = event.findPointerIndex(pointerId);
                int pointerIdBits3 = 1 << pointerId;
                switch (event.getPointerCount()) {
                    case 1:
                        if (this.mSendHoverEnterAndMoveDelayed.isPending()) {
                            handleMotionEventGestureDetecting(rawEvent, policyFlags);
                            this.mSendHoverEnterAndMoveDelayed.addEvent(event);
                            float deltaX = receivedTracker.getReceivedPointerDownX(pointerId) - rawEvent.getX(pointerIndex);
                            float deltaY = receivedTracker.getReceivedPointerDownY(pointerId) - rawEvent.getY(pointerIndex);
                            double moveDelta = Math.hypot(deltaX, deltaY);
                            if (moveDelta > this.mDoubleTapSlop) {
                                this.mVelocityTracker.computeCurrentVelocity(1000);
                                float maxAbsVelocity = Math.max(Math.abs(this.mVelocityTracker.getXVelocity(pointerId)), Math.abs(this.mVelocityTracker.getYVelocity(pointerId)));
                                if (maxAbsVelocity > this.mScaledGestureDetectionVelocity) {
                                    this.mCurrentState = 5;
                                    this.mVelocityTracker.clear();
                                    this.mSendHoverEnterAndMoveDelayed.cancel();
                                    this.mSendHoverExitDelayed.cancel();
                                    this.mPerformLongPressDelayed.cancel();
                                    this.mExitGestureDetectionModeDelayed.post();
                                    sendAccessibilityEvent(262144);
                                } else {
                                    this.mSendHoverEnterAndMoveDelayed.forceSendAndRemove();
                                    this.mSendHoverExitDelayed.cancel();
                                    this.mPerformLongPressDelayed.cancel();
                                    sendMotionEvent(event, 7, pointerIdBits3, policyFlags);
                                }
                            }
                        } else {
                            if (this.mPerformLongPressDelayed.isPending()) {
                                float deltaX2 = receivedTracker.getReceivedPointerDownX(pointerId) - rawEvent.getX(pointerIndex);
                                float deltaY2 = receivedTracker.getReceivedPointerDownY(pointerId) - rawEvent.getY(pointerIndex);
                                double moveDelta2 = Math.hypot(deltaX2, deltaY2);
                                if (moveDelta2 > this.mTouchSlop) {
                                    this.mPerformLongPressDelayed.cancel();
                                }
                            }
                            if (this.mTouchExplorationInProgress) {
                                sendTouchExplorationGestureStartAndHoverEnterIfNeeded(policyFlags);
                                sendMotionEvent(event, 7, pointerIdBits3, policyFlags);
                            }
                        }
                        break;
                    case 2:
                        if (this.mSendHoverEnterAndMoveDelayed.isPending()) {
                            this.mSendHoverEnterAndMoveDelayed.cancel();
                            this.mSendHoverExitDelayed.cancel();
                            this.mPerformLongPressDelayed.cancel();
                        } else {
                            this.mPerformLongPressDelayed.cancel();
                            if (this.mTouchExplorationInProgress) {
                                float deltaX3 = receivedTracker.getReceivedPointerDownX(pointerId) - rawEvent.getX(pointerIndex);
                                float deltaY3 = receivedTracker.getReceivedPointerDownY(pointerId) - rawEvent.getY(pointerIndex);
                                double moveDelta3 = Math.hypot(deltaX3, deltaY3);
                                if (moveDelta3 >= this.mDoubleTapSlop) {
                                    sendHoverExitAndTouchExplorationGestureEndIfNeeded(policyFlags);
                                }
                            }
                        }
                        this.mStrokeBuffer.clear();
                        if (isDraggingGesture(event)) {
                            this.mCurrentState = 2;
                            this.mDraggingPointerId = pointerId;
                            event.setEdgeFlags(receivedTracker.getLastReceivedDownEdgeFlags());
                            sendMotionEvent(event, 0, pointerIdBits3, policyFlags);
                        } else {
                            this.mCurrentState = 4;
                            sendDownForAllNotInjectedPointers(event, policyFlags);
                        }
                        this.mVelocityTracker.clear();
                        break;
                    default:
                        if (this.mSendHoverEnterAndMoveDelayed.isPending()) {
                            this.mSendHoverEnterAndMoveDelayed.cancel();
                            this.mSendHoverExitDelayed.cancel();
                            this.mPerformLongPressDelayed.cancel();
                        } else {
                            this.mPerformLongPressDelayed.cancel();
                            sendHoverExitAndTouchExplorationGestureEndIfNeeded(policyFlags);
                        }
                        this.mCurrentState = 4;
                        sendDownForAllNotInjectedPointers(event, policyFlags);
                        this.mVelocityTracker.clear();
                        break;
                }
                break;
            case 3:
                clear(event, policyFlags);
                break;
            case 5:
                this.mSendHoverEnterAndMoveDelayed.cancel();
                this.mSendHoverExitDelayed.cancel();
                this.mPerformLongPressDelayed.cancel();
                break;
        }
    }

    private void handleMotionEventStateDragging(MotionEvent event, int policyFlags) {
        int pointerIdBits = 1 << this.mDraggingPointerId;
        switch (event.getActionMasked()) {
            case 0:
                throw new IllegalStateException("Dragging state can be reached only if two pointers are already down");
            case 1:
                this.mAms.onTouchInteractionEnd();
                sendAccessibilityEvent(2097152);
                int pointerId = event.getPointerId(event.getActionIndex());
                if (pointerId == this.mDraggingPointerId) {
                    this.mDraggingPointerId = -1;
                    sendMotionEvent(event, 1, pointerIdBits, policyFlags);
                }
                this.mCurrentState = 1;
                return;
            case 2:
                switch (event.getPointerCount()) {
                    case 1:
                        return;
                    case 2:
                        if (isDraggingGesture(event)) {
                            float firstPtrX = event.getX(0);
                            float firstPtrY = event.getY(0);
                            float secondPtrX = event.getX(1);
                            float secondPtrY = event.getY(1);
                            float deltaX = firstPtrX - secondPtrX;
                            float deltaY = firstPtrY - secondPtrY;
                            double distance = Math.hypot(deltaX, deltaY);
                            if (distance > this.mScaledMinPointerDistanceToUseMiddleLocation) {
                                event.setLocation(deltaX / MIN_PREDICTION_SCORE, deltaY / MIN_PREDICTION_SCORE);
                            }
                            sendMotionEvent(event, 2, pointerIdBits, policyFlags);
                            return;
                        }
                        this.mCurrentState = 4;
                        sendMotionEvent(event, 1, pointerIdBits, policyFlags);
                        sendDownForAllNotInjectedPointers(event, policyFlags);
                        return;
                    default:
                        this.mCurrentState = 4;
                        sendMotionEvent(event, 1, pointerIdBits, policyFlags);
                        sendDownForAllNotInjectedPointers(event, policyFlags);
                        return;
                }
            case 3:
                clear(event, policyFlags);
                return;
            case 4:
            default:
                return;
            case 5:
                this.mCurrentState = 4;
                if (this.mDraggingPointerId != -1) {
                    sendMotionEvent(event, 1, pointerIdBits, policyFlags);
                }
                sendDownForAllNotInjectedPointers(event, policyFlags);
                return;
            case 6:
                int pointerId2 = event.getPointerId(event.getActionIndex());
                if (pointerId2 == this.mDraggingPointerId) {
                    this.mDraggingPointerId = -1;
                    sendMotionEvent(event, 1, pointerIdBits, policyFlags);
                    return;
                }
                return;
        }
    }

    private void handleMotionEventStateDelegating(MotionEvent event, int policyFlags) {
        switch (event.getActionMasked()) {
            case 0:
                throw new IllegalStateException("Delegating state can only be reached if there is at least one pointer down!");
            case 1:
                if (this.mLongPressingPointerId >= 0) {
                    event = offsetEvent(event, -this.mLongPressingPointerDeltaX, -this.mLongPressingPointerDeltaY);
                    this.mLongPressingPointerId = -1;
                    this.mLongPressingPointerDeltaX = 0;
                    this.mLongPressingPointerDeltaY = 0;
                }
                sendMotionEvent(event, event.getAction(), -1, policyFlags);
                this.mAms.onTouchInteractionEnd();
                sendAccessibilityEvent(2097152);
                this.mCurrentState = 1;
                return;
            case 2:
            default:
                sendMotionEvent(event, event.getAction(), -1, policyFlags);
                return;
            case 3:
                clear(event, policyFlags);
                return;
        }
    }

    private void handleMotionEventGestureDetecting(MotionEvent event, int policyFlags) {
        switch (event.getActionMasked()) {
            case 0:
                float x = event.getX();
                float y = event.getY();
                this.mPreviousX = x;
                this.mPreviousY = y;
                this.mStrokeBuffer.add(new GesturePoint(x, y, event.getEventTime()));
                break;
            case 1:
                this.mAms.onTouchInteractionEnd();
                sendAccessibilityEvent(524288);
                sendAccessibilityEvent(2097152);
                this.mStrokeBuffer.add(new GesturePoint(event.getX(), event.getY(), event.getEventTime()));
                Gesture gesture = new Gesture();
                gesture.addStroke(new GestureStroke(this.mStrokeBuffer));
                ArrayList<Prediction> predictions = this.mGestureLibrary.recognize(gesture);
                if (!predictions.isEmpty()) {
                    Prediction bestPrediction = predictions.get(0);
                    if (bestPrediction.score >= 2.0d) {
                        try {
                            int gestureId = Integer.parseInt(bestPrediction.name);
                            this.mAms.onGesture(gestureId);
                        } catch (NumberFormatException e) {
                            Slog.w(LOG_TAG, "Non numeric gesture id:" + bestPrediction.name);
                        }
                    }
                }
                this.mStrokeBuffer.clear();
                this.mExitGestureDetectionModeDelayed.cancel();
                this.mCurrentState = 1;
                break;
            case 2:
                float x2 = event.getX();
                float y2 = event.getY();
                float dX = Math.abs(x2 - this.mPreviousX);
                float dY = Math.abs(y2 - this.mPreviousY);
                if (dX >= 3.0f || dY >= 3.0f) {
                    this.mPreviousX = x2;
                    this.mPreviousY = y2;
                    this.mStrokeBuffer.add(new GesturePoint(x2, y2, event.getEventTime()));
                }
                break;
            case 3:
                clear(event, policyFlags);
                break;
        }
    }

    private void sendAccessibilityEvent(int type) {
        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(this.mContext);
        if (accessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain(type);
            event.setWindowId(this.mAms.getActiveWindowId());
            accessibilityManager.sendAccessibilityEvent(event);
            switch (type) {
                case 512:
                    this.mTouchExplorationInProgress = true;
                    break;
                case 1024:
                    this.mTouchExplorationInProgress = DEBUG;
                    break;
            }
        }
    }

    private void sendDownForAllNotInjectedPointers(MotionEvent prototype, int policyFlags) {
        InjectedPointerTracker injectedPointers = this.mInjectedPointerTracker;
        int pointerIdBits = 0;
        int pointerCount = prototype.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            int pointerId = prototype.getPointerId(i);
            if (!injectedPointers.isInjectedPointerDown(pointerId)) {
                pointerIdBits |= 1 << pointerId;
                int action = computeInjectionAction(0, i);
                sendMotionEvent(prototype, action, pointerIdBits, policyFlags);
            }
        }
    }

    private void sendHoverExitAndTouchExplorationGestureEndIfNeeded(int policyFlags) {
        MotionEvent event = this.mInjectedPointerTracker.getLastInjectedHoverEvent();
        if (event != null && event.getActionMasked() != 10) {
            int pointerIdBits = event.getPointerIdBits();
            if (!this.mSendTouchExplorationEndDelayed.isPending()) {
                this.mSendTouchExplorationEndDelayed.post();
            }
            sendMotionEvent(event, 10, pointerIdBits, policyFlags);
        }
    }

    private void sendTouchExplorationGestureStartAndHoverEnterIfNeeded(int policyFlags) {
        MotionEvent event = this.mInjectedPointerTracker.getLastInjectedHoverEvent();
        if (event != null && event.getActionMasked() == 10) {
            int pointerIdBits = event.getPointerIdBits();
            sendAccessibilityEvent(512);
            sendMotionEvent(event, 9, pointerIdBits, policyFlags);
        }
    }

    private void sendUpForInjectedDownPointers(MotionEvent prototype, int policyFlags) {
        InjectedPointerTracker injectedTracked = this.mInjectedPointerTracker;
        int pointerIdBits = 0;
        int pointerCount = prototype.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            int pointerId = prototype.getPointerId(i);
            if (injectedTracked.isInjectedPointerDown(pointerId)) {
                pointerIdBits |= 1 << pointerId;
                int action = computeInjectionAction(1, i);
                sendMotionEvent(prototype, action, pointerIdBits, policyFlags);
            }
        }
    }

    private void sendActionDownAndUp(MotionEvent prototype, int policyFlags, boolean targetAccessibilityFocus) {
        int pointerId = prototype.getPointerId(prototype.getActionIndex());
        int pointerIdBits = 1 << pointerId;
        prototype.setTargetAccessibilityFocus(targetAccessibilityFocus);
        sendMotionEvent(prototype, 0, pointerIdBits, policyFlags);
        prototype.setTargetAccessibilityFocus(targetAccessibilityFocus);
        sendMotionEvent(prototype, 1, pointerIdBits, policyFlags);
    }

    private void sendMotionEvent(MotionEvent prototype, int action, int pointerIdBits, int policyFlags) {
        MotionEvent event;
        prototype.setAction(action);
        if (pointerIdBits == -1) {
            event = prototype;
        } else {
            event = prototype.split(pointerIdBits);
        }
        if (action == 0) {
            event.setDownTime(event.getEventTime());
        } else {
            event.setDownTime(this.mInjectedPointerTracker.getLastInjectedDownEventTime());
        }
        if (this.mLongPressingPointerId >= 0) {
            event = offsetEvent(event, -this.mLongPressingPointerDeltaX, -this.mLongPressingPointerDeltaY);
        }
        int policyFlags2 = policyFlags | 1073741824;
        if (this.mNext != null) {
            this.mNext.onMotionEvent(event, null, policyFlags2);
        }
        this.mInjectedPointerTracker.onMotionEvent(event);
        if (event != prototype) {
            event.recycle();
        }
    }

    private MotionEvent offsetEvent(MotionEvent event, int offsetX, int offsetY) {
        if (offsetX != 0 || offsetY != 0) {
            int remappedIndex = event.findPointerIndex(this.mLongPressingPointerId);
            int pointerCount = event.getPointerCount();
            MotionEvent.PointerProperties[] props = MotionEvent.PointerProperties.createArray(pointerCount);
            MotionEvent.PointerCoords[] coords = MotionEvent.PointerCoords.createArray(pointerCount);
            for (int i = 0; i < pointerCount; i++) {
                event.getPointerProperties(i, props[i]);
                event.getPointerCoords(i, coords[i]);
                if (i == remappedIndex) {
                    coords[i].x += offsetX;
                    coords[i].y += offsetY;
                }
            }
            return MotionEvent.obtain(event.getDownTime(), event.getEventTime(), event.getAction(), event.getPointerCount(), props, coords, event.getMetaState(), event.getButtonState(), 1.0f, 1.0f, event.getDeviceId(), event.getEdgeFlags(), event.getSource(), event.getFlags());
        }
        return event;
    }

    private int computeInjectionAction(int actionMasked, int pointerIndex) {
        switch (actionMasked) {
            case 0:
            case 5:
                InjectedPointerTracker injectedTracker = this.mInjectedPointerTracker;
                if (injectedTracker.getInjectedPointerDownCount() == 0) {
                    return 0;
                }
                return (pointerIndex << 8) | 5;
            case 6:
                InjectedPointerTracker injectedTracker2 = this.mInjectedPointerTracker;
                if (injectedTracker2.getInjectedPointerDownCount() == 1) {
                    return 1;
                }
                return (pointerIndex << 8) | 6;
            default:
                return actionMasked;
        }
    }

    private class DoubleTapDetector {
        private MotionEvent mDownEvent;
        private MotionEvent mFirstTapEvent;

        private DoubleTapDetector() {
        }

        public void onMotionEvent(MotionEvent event, int policyFlags) {
            int actionIndex = event.getActionIndex();
            int action = event.getActionMasked();
            switch (action) {
                case 0:
                case 5:
                    if (this.mFirstTapEvent != null && !GestureUtils.isSamePointerContext(this.mFirstTapEvent, event)) {
                        clear();
                    }
                    this.mDownEvent = MotionEvent.obtain(event);
                    break;
                case 1:
                case 6:
                    if (this.mDownEvent != null) {
                        if (GestureUtils.isSamePointerContext(this.mDownEvent, event)) {
                            if (GestureUtils.isTap(this.mDownEvent, event, TouchExplorer.this.mTapTimeout, TouchExplorer.this.mTouchSlop, actionIndex)) {
                                if (this.mFirstTapEvent != null && !GestureUtils.isTimedOut(this.mFirstTapEvent, event, TouchExplorer.this.mDoubleTapTimeout)) {
                                    if (GestureUtils.isMultiTap(this.mFirstTapEvent, event, TouchExplorer.this.mDoubleTapTimeout, TouchExplorer.this.mDoubleTapSlop, actionIndex)) {
                                        onDoubleTap(event, policyFlags);
                                        this.mFirstTapEvent.recycle();
                                        this.mFirstTapEvent = null;
                                        this.mDownEvent.recycle();
                                        this.mDownEvent = null;
                                    } else {
                                        this.mFirstTapEvent.recycle();
                                        this.mFirstTapEvent = null;
                                    }
                                } else {
                                    this.mFirstTapEvent = MotionEvent.obtain(event);
                                    this.mDownEvent.recycle();
                                    this.mDownEvent = null;
                                }
                            } else if (this.mFirstTapEvent != null) {
                                this.mFirstTapEvent.recycle();
                                this.mFirstTapEvent = null;
                            }
                            this.mDownEvent.recycle();
                            this.mDownEvent = null;
                        } else {
                            clear();
                        }
                    }
                    break;
            }
        }

        public void onDoubleTap(MotionEvent secondTapUp, int policyFlags) {
            if (secondTapUp.getPointerCount() <= 2) {
                TouchExplorer.this.mSendHoverEnterAndMoveDelayed.cancel();
                TouchExplorer.this.mSendHoverExitDelayed.cancel();
                TouchExplorer.this.mPerformLongPressDelayed.cancel();
                if (TouchExplorer.this.mSendTouchExplorationEndDelayed.isPending()) {
                    TouchExplorer.this.mSendTouchExplorationEndDelayed.forceSendAndRemove();
                }
                if (TouchExplorer.this.mSendTouchInteractionEndDelayed.isPending()) {
                    TouchExplorer.this.mSendTouchInteractionEndDelayed.forceSendAndRemove();
                }
                int pointerId = secondTapUp.getPointerId(secondTapUp.getActionIndex());
                int pointerIndex = secondTapUp.findPointerIndex(pointerId);
                Point clickLocation = TouchExplorer.this.mTempPoint;
                int result = TouchExplorer.this.computeClickLocation(clickLocation);
                if (result != 0) {
                    MotionEvent.PointerProperties[] properties = {new MotionEvent.PointerProperties()};
                    secondTapUp.getPointerProperties(pointerIndex, properties[0]);
                    MotionEvent.PointerCoords[] coords = {new MotionEvent.PointerCoords()};
                    coords[0].x = clickLocation.x;
                    coords[0].y = clickLocation.y;
                    MotionEvent event = MotionEvent.obtain(secondTapUp.getDownTime(), secondTapUp.getEventTime(), 0, 1, properties, coords, 0, 0, 1.0f, 1.0f, secondTapUp.getDeviceId(), 0, secondTapUp.getSource(), secondTapUp.getFlags());
                    boolean targetAccessibilityFocus = result == 1 ? true : TouchExplorer.DEBUG;
                    TouchExplorer.this.sendActionDownAndUp(event, policyFlags, targetAccessibilityFocus);
                    event.recycle();
                }
            }
        }

        public void clear() {
            if (this.mDownEvent != null) {
                this.mDownEvent.recycle();
                this.mDownEvent = null;
            }
            if (this.mFirstTapEvent != null) {
                this.mFirstTapEvent.recycle();
                this.mFirstTapEvent = null;
            }
        }

        public boolean firstTapDetected() {
            if (this.mFirstTapEvent == null || SystemClock.uptimeMillis() - this.mFirstTapEvent.getEventTime() >= TouchExplorer.this.mDoubleTapTimeout) {
                return TouchExplorer.DEBUG;
            }
            return true;
        }
    }

    private boolean isDraggingGesture(MotionEvent event) {
        ReceivedPointerTracker receivedTracker = this.mReceivedPointerTracker;
        float firstPtrX = event.getX(0);
        float firstPtrY = event.getY(0);
        float secondPtrX = event.getX(1);
        float secondPtrY = event.getY(1);
        float firstPtrDownX = receivedTracker.getReceivedPointerDownX(0);
        float firstPtrDownY = receivedTracker.getReceivedPointerDownY(0);
        float secondPtrDownX = receivedTracker.getReceivedPointerDownX(1);
        float secondPtrDownY = receivedTracker.getReceivedPointerDownY(1);
        return GestureUtils.isDraggingGesture(firstPtrDownX, firstPtrDownY, secondPtrDownX, secondPtrDownY, firstPtrX, firstPtrY, secondPtrX, secondPtrY, MAX_DRAGGING_ANGLE_COS);
    }

    private int computeClickLocation(Point outLocation) {
        MotionEvent lastExploreEvent = this.mInjectedPointerTracker.getLastInjectedHoverEventForClick();
        if (lastExploreEvent != null) {
            int lastExplorePointerIndex = lastExploreEvent.getActionIndex();
            outLocation.x = (int) lastExploreEvent.getX(lastExplorePointerIndex);
            outLocation.y = (int) lastExploreEvent.getY(lastExplorePointerIndex);
            if (!this.mAms.accessibilityFocusOnlyInActiveWindow() || this.mLastTouchedWindowId == this.mAms.getActiveWindowId()) {
                return this.mAms.getAccessibilityFocusClickPointInScreen(outLocation) ? 1 : 2;
            }
        }
        return !this.mAms.getAccessibilityFocusClickPointInScreen(outLocation) ? 0 : 1;
    }

    private static String getStateSymbolicName(int state) {
        switch (state) {
            case 1:
                return "STATE_TOUCH_EXPLORING";
            case 2:
                return "STATE_DRAGGING";
            case 3:
            default:
                throw new IllegalArgumentException("Unknown state: " + state);
            case 4:
                return "STATE_DELEGATING";
            case 5:
                return "STATE_GESTURE_DETECTING";
        }
    }

    private final class ExitGestureDetectionModeDelayed implements Runnable {
        private ExitGestureDetectionModeDelayed() {
        }

        public void post() {
            TouchExplorer.this.mHandler.postDelayed(this, 2000L);
        }

        public void cancel() {
            TouchExplorer.this.mHandler.removeCallbacks(this);
        }

        @Override
        public void run() {
            TouchExplorer.this.sendAccessibilityEvent(524288);
            TouchExplorer.this.sendAccessibilityEvent(512);
            TouchExplorer.this.clear();
        }
    }

    private final class PerformLongPressDelayed implements Runnable {
        private MotionEvent mEvent;
        private int mPolicyFlags;

        private PerformLongPressDelayed() {
        }

        public void post(MotionEvent prototype, int policyFlags) {
            this.mEvent = MotionEvent.obtain(prototype);
            this.mPolicyFlags = policyFlags;
            TouchExplorer.this.mHandler.postDelayed(this, ViewConfiguration.getLongPressTimeout());
        }

        public void cancel() {
            if (this.mEvent != null) {
                TouchExplorer.this.mHandler.removeCallbacks(this);
                clear();
            }
        }

        private boolean isPending() {
            return TouchExplorer.this.mHandler.hasCallbacks(this);
        }

        @Override
        public void run() {
            if (TouchExplorer.this.mReceivedPointerTracker.getLastReceivedEvent().getPointerCount() != 0) {
                int pointerId = this.mEvent.getPointerId(this.mEvent.getActionIndex());
                int pointerIndex = this.mEvent.findPointerIndex(pointerId);
                Point clickLocation = TouchExplorer.this.mTempPoint;
                int result = TouchExplorer.this.computeClickLocation(clickLocation);
                if (result != 0) {
                    TouchExplorer.this.mLongPressingPointerId = pointerId;
                    TouchExplorer.this.mLongPressingPointerDeltaX = ((int) this.mEvent.getX(pointerIndex)) - clickLocation.x;
                    TouchExplorer.this.mLongPressingPointerDeltaY = ((int) this.mEvent.getY(pointerIndex)) - clickLocation.y;
                    TouchExplorer.this.sendHoverExitAndTouchExplorationGestureEndIfNeeded(this.mPolicyFlags);
                    TouchExplorer.this.mCurrentState = 4;
                    TouchExplorer.this.sendDownForAllNotInjectedPointers(this.mEvent, this.mPolicyFlags);
                    clear();
                }
            }
        }

        private void clear() {
            this.mEvent.recycle();
            this.mEvent = null;
            this.mPolicyFlags = 0;
        }
    }

    class SendHoverEnterAndMoveDelayed implements Runnable {
        private final String LOG_TAG_SEND_HOVER_DELAYED = "SendHoverEnterAndMoveDelayed";
        private final List<MotionEvent> mEvents = new ArrayList();
        private int mPointerIdBits;
        private int mPolicyFlags;

        SendHoverEnterAndMoveDelayed() {
        }

        public void post(MotionEvent event, boolean touchExplorationInProgress, int pointerIdBits, int policyFlags) {
            cancel();
            addEvent(event);
            this.mPointerIdBits = pointerIdBits;
            this.mPolicyFlags = policyFlags;
            TouchExplorer.this.mHandler.postDelayed(this, TouchExplorer.this.mDetermineUserIntentTimeout);
        }

        public void addEvent(MotionEvent event) {
            this.mEvents.add(MotionEvent.obtain(event));
        }

        public void cancel() {
            if (isPending()) {
                TouchExplorer.this.mHandler.removeCallbacks(this);
                clear();
            }
        }

        private boolean isPending() {
            return TouchExplorer.this.mHandler.hasCallbacks(this);
        }

        private void clear() {
            this.mPointerIdBits = -1;
            this.mPolicyFlags = 0;
            int eventCount = this.mEvents.size();
            for (int i = eventCount - 1; i >= 0; i--) {
                this.mEvents.remove(i).recycle();
            }
        }

        public void forceSendAndRemove() {
            if (isPending()) {
                run();
                cancel();
            }
        }

        @Override
        public void run() {
            TouchExplorer.this.sendAccessibilityEvent(512);
            if (!this.mEvents.isEmpty()) {
                TouchExplorer.this.sendMotionEvent(this.mEvents.get(0), 9, this.mPointerIdBits, this.mPolicyFlags);
                int eventCount = this.mEvents.size();
                for (int i = 1; i < eventCount; i++) {
                    TouchExplorer.this.sendMotionEvent(this.mEvents.get(i), 7, this.mPointerIdBits, this.mPolicyFlags);
                }
            }
            clear();
        }
    }

    class SendHoverExitDelayed implements Runnable {
        private final String LOG_TAG_SEND_HOVER_DELAYED = "SendHoverExitDelayed";
        private int mPointerIdBits;
        private int mPolicyFlags;
        private MotionEvent mPrototype;

        SendHoverExitDelayed() {
        }

        public void post(MotionEvent prototype, int pointerIdBits, int policyFlags) {
            cancel();
            this.mPrototype = MotionEvent.obtain(prototype);
            this.mPointerIdBits = pointerIdBits;
            this.mPolicyFlags = policyFlags;
            TouchExplorer.this.mHandler.postDelayed(this, TouchExplorer.this.mDetermineUserIntentTimeout);
        }

        public void cancel() {
            if (isPending()) {
                TouchExplorer.this.mHandler.removeCallbacks(this);
                clear();
            }
        }

        private boolean isPending() {
            return TouchExplorer.this.mHandler.hasCallbacks(this);
        }

        private void clear() {
            this.mPrototype.recycle();
            this.mPrototype = null;
            this.mPointerIdBits = -1;
            this.mPolicyFlags = 0;
        }

        public void forceSendAndRemove() {
            if (isPending()) {
                run();
                cancel();
            }
        }

        @Override
        public void run() {
            TouchExplorer.this.sendMotionEvent(this.mPrototype, 10, this.mPointerIdBits, this.mPolicyFlags);
            if (!TouchExplorer.this.mSendTouchExplorationEndDelayed.isPending()) {
                TouchExplorer.this.mSendTouchExplorationEndDelayed.cancel();
                TouchExplorer.this.mSendTouchExplorationEndDelayed.post();
            }
            if (TouchExplorer.this.mSendTouchInteractionEndDelayed.isPending()) {
                TouchExplorer.this.mSendTouchInteractionEndDelayed.cancel();
                TouchExplorer.this.mSendTouchInteractionEndDelayed.post();
            }
            clear();
        }
    }

    private class SendAccessibilityEventDelayed implements Runnable {
        private final int mDelay;
        private final int mEventType;

        public SendAccessibilityEventDelayed(int eventType, int delay) {
            this.mEventType = eventType;
            this.mDelay = delay;
        }

        public void cancel() {
            TouchExplorer.this.mHandler.removeCallbacks(this);
        }

        public void post() {
            TouchExplorer.this.mHandler.postDelayed(this, this.mDelay);
        }

        public boolean isPending() {
            return TouchExplorer.this.mHandler.hasCallbacks(this);
        }

        public void forceSendAndRemove() {
            if (isPending()) {
                run();
                cancel();
            }
        }

        @Override
        public void run() {
            TouchExplorer.this.sendAccessibilityEvent(this.mEventType);
        }
    }

    public String toString() {
        return LOG_TAG;
    }

    class InjectedPointerTracker {
        private static final String LOG_TAG_INJECTED_POINTER_TRACKER = "InjectedPointerTracker";
        private int mInjectedPointersDown;
        private long mLastInjectedDownEventTime;
        private MotionEvent mLastInjectedHoverEvent;
        private MotionEvent mLastInjectedHoverEventForClick;

        InjectedPointerTracker() {
        }

        public void onMotionEvent(MotionEvent event) {
            int action = event.getActionMasked();
            switch (action) {
                case 0:
                case 5:
                    int pointerId = event.getPointerId(event.getActionIndex());
                    int pointerFlag = 1 << pointerId;
                    this.mInjectedPointersDown |= pointerFlag;
                    this.mLastInjectedDownEventTime = event.getDownTime();
                    break;
                case 1:
                case 6:
                    int pointerId2 = event.getPointerId(event.getActionIndex());
                    int pointerFlag2 = 1 << pointerId2;
                    this.mInjectedPointersDown &= pointerFlag2 ^ (-1);
                    if (this.mInjectedPointersDown == 0) {
                        this.mLastInjectedDownEventTime = 0L;
                    }
                    break;
                case 7:
                case 9:
                case 10:
                    if (this.mLastInjectedHoverEvent != null) {
                        this.mLastInjectedHoverEvent.recycle();
                    }
                    this.mLastInjectedHoverEvent = MotionEvent.obtain(event);
                    if (this.mLastInjectedHoverEventForClick != null) {
                        this.mLastInjectedHoverEventForClick.recycle();
                    }
                    this.mLastInjectedHoverEventForClick = MotionEvent.obtain(event);
                    break;
            }
        }

        public void clear() {
            this.mInjectedPointersDown = 0;
        }

        public long getLastInjectedDownEventTime() {
            return this.mLastInjectedDownEventTime;
        }

        public int getInjectedPointerDownCount() {
            return Integer.bitCount(this.mInjectedPointersDown);
        }

        public int getInjectedPointersDown() {
            return this.mInjectedPointersDown;
        }

        public boolean isInjectedPointerDown(int pointerId) {
            int pointerFlag = 1 << pointerId;
            if ((this.mInjectedPointersDown & pointerFlag) != 0) {
                return true;
            }
            return TouchExplorer.DEBUG;
        }

        public MotionEvent getLastInjectedHoverEvent() {
            return this.mLastInjectedHoverEvent;
        }

        public MotionEvent getLastInjectedHoverEventForClick() {
            return this.mLastInjectedHoverEventForClick;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("=========================");
            builder.append("\nDown pointers #");
            builder.append(Integer.bitCount(this.mInjectedPointersDown));
            builder.append(" [ ");
            for (int i = 0; i < 32; i++) {
                if ((this.mInjectedPointersDown & i) != 0) {
                    builder.append(i);
                    builder.append(" ");
                }
            }
            builder.append("]");
            builder.append("\n=========================");
            return builder.toString();
        }
    }

    class ReceivedPointerTracker {
        private static final String LOG_TAG_RECEIVED_POINTER_TRACKER = "ReceivedPointerTracker";
        private int mLastReceivedDownEdgeFlags;
        private MotionEvent mLastReceivedEvent;
        private long mLastReceivedUpPointerDownTime;
        private float mLastReceivedUpPointerDownX;
        private float mLastReceivedUpPointerDownY;
        private int mPrimaryPointerId;
        private int mReceivedPointersDown;
        private final float[] mReceivedPointerDownX = new float[32];
        private final float[] mReceivedPointerDownY = new float[32];
        private final long[] mReceivedPointerDownTime = new long[32];

        ReceivedPointerTracker() {
        }

        public void clear() {
            Arrays.fill(this.mReceivedPointerDownX, 0.0f);
            Arrays.fill(this.mReceivedPointerDownY, 0.0f);
            Arrays.fill(this.mReceivedPointerDownTime, 0L);
            this.mReceivedPointersDown = 0;
            this.mPrimaryPointerId = 0;
            this.mLastReceivedUpPointerDownTime = 0L;
            this.mLastReceivedUpPointerDownX = 0.0f;
            this.mLastReceivedUpPointerDownY = 0.0f;
        }

        public void onMotionEvent(MotionEvent event) {
            if (this.mLastReceivedEvent != null) {
                this.mLastReceivedEvent.recycle();
            }
            this.mLastReceivedEvent = MotionEvent.obtain(event);
            int action = event.getActionMasked();
            switch (action) {
                case 0:
                    handleReceivedPointerDown(event.getActionIndex(), event);
                    break;
                case 1:
                    handleReceivedPointerUp(event.getActionIndex(), event);
                    break;
                case 5:
                    handleReceivedPointerDown(event.getActionIndex(), event);
                    break;
                case 6:
                    handleReceivedPointerUp(event.getActionIndex(), event);
                    break;
            }
        }

        public MotionEvent getLastReceivedEvent() {
            return this.mLastReceivedEvent;
        }

        public int getReceivedPointerDownCount() {
            return Integer.bitCount(this.mReceivedPointersDown);
        }

        public boolean isReceivedPointerDown(int pointerId) {
            int pointerFlag = 1 << pointerId;
            if ((this.mReceivedPointersDown & pointerFlag) != 0) {
                return true;
            }
            return TouchExplorer.DEBUG;
        }

        public float getReceivedPointerDownX(int pointerId) {
            return this.mReceivedPointerDownX[pointerId];
        }

        public float getReceivedPointerDownY(int pointerId) {
            return this.mReceivedPointerDownY[pointerId];
        }

        public long getReceivedPointerDownTime(int pointerId) {
            return this.mReceivedPointerDownTime[pointerId];
        }

        public int getPrimaryPointerId() {
            if (this.mPrimaryPointerId == -1) {
                this.mPrimaryPointerId = findPrimaryPointerId();
            }
            return this.mPrimaryPointerId;
        }

        public long getLastReceivedUpPointerDownTime() {
            return this.mLastReceivedUpPointerDownTime;
        }

        public float getLastReceivedUpPointerDownX() {
            return this.mLastReceivedUpPointerDownX;
        }

        public float getLastReceivedUpPointerDownY() {
            return this.mLastReceivedUpPointerDownY;
        }

        public int getLastReceivedDownEdgeFlags() {
            return this.mLastReceivedDownEdgeFlags;
        }

        private void handleReceivedPointerDown(int pointerIndex, MotionEvent event) {
            int pointerId = event.getPointerId(pointerIndex);
            int pointerFlag = 1 << pointerId;
            this.mLastReceivedUpPointerDownTime = 0L;
            this.mLastReceivedUpPointerDownX = 0.0f;
            this.mLastReceivedUpPointerDownX = 0.0f;
            this.mLastReceivedDownEdgeFlags = event.getEdgeFlags();
            this.mReceivedPointersDown |= pointerFlag;
            this.mReceivedPointerDownX[pointerId] = event.getX(pointerIndex);
            this.mReceivedPointerDownY[pointerId] = event.getY(pointerIndex);
            this.mReceivedPointerDownTime[pointerId] = event.getEventTime();
            this.mPrimaryPointerId = pointerId;
        }

        private void handleReceivedPointerUp(int pointerIndex, MotionEvent event) {
            int pointerId = event.getPointerId(pointerIndex);
            int pointerFlag = 1 << pointerId;
            this.mLastReceivedUpPointerDownTime = getReceivedPointerDownTime(pointerId);
            this.mLastReceivedUpPointerDownX = this.mReceivedPointerDownX[pointerId];
            this.mLastReceivedUpPointerDownY = this.mReceivedPointerDownY[pointerId];
            this.mReceivedPointersDown &= pointerFlag ^ (-1);
            this.mReceivedPointerDownX[pointerId] = 0.0f;
            this.mReceivedPointerDownY[pointerId] = 0.0f;
            this.mReceivedPointerDownTime[pointerId] = 0;
            if (this.mPrimaryPointerId == pointerId) {
                this.mPrimaryPointerId = -1;
            }
        }

        private int findPrimaryPointerId() {
            int primaryPointerId = -1;
            long minDownTime = JobStatus.NO_LATEST_RUNTIME;
            int pointerIdBits = this.mReceivedPointersDown;
            while (pointerIdBits > 0) {
                int pointerId = Integer.numberOfTrailingZeros(pointerIdBits);
                pointerIdBits &= (1 << pointerId) ^ (-1);
                long downPointerTime = this.mReceivedPointerDownTime[pointerId];
                if (downPointerTime < minDownTime) {
                    minDownTime = downPointerTime;
                    primaryPointerId = pointerId;
                }
            }
            return primaryPointerId;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("=========================");
            builder.append("\nDown pointers #");
            builder.append(getReceivedPointerDownCount());
            builder.append(" [ ");
            for (int i = 0; i < 32; i++) {
                if (isReceivedPointerDown(i)) {
                    builder.append(i);
                    builder.append(" ");
                }
            }
            builder.append("]");
            builder.append("\nPrimary pointer id [ ");
            builder.append(getPrimaryPointerId());
            builder.append(" ]");
            builder.append("\n=========================");
            return builder.toString();
        }
    }
}
