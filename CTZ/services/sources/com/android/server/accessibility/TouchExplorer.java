package com.android.server.accessibility;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.server.accessibility.AccessibilityGestureDetector;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.usb.descriptors.UsbACInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TouchExplorer extends BaseEventStreamTransformation implements AccessibilityGestureDetector.Listener {
    private static final int ALL_POINTER_ID_BITS = -1;
    private static final int CLICK_LOCATION_ACCESSIBILITY_FOCUS = 1;
    private static final int CLICK_LOCATION_LAST_TOUCH_EXPLORED = 2;
    private static final int CLICK_LOCATION_NONE = 0;
    private static final boolean DEBUG = false;
    private static final int EXIT_GESTURE_DETECTION_TIMEOUT = 2000;
    private static final int INVALID_POINTER_ID = -1;
    private static final String LOG_TAG = "TouchExplorer";
    private static final float MAX_DRAGGING_ANGLE_COS = 0.52532196f;
    private static final int MAX_POINTER_COUNT = 32;
    private static final int MIN_POINTER_DISTANCE_TO_USE_MIDDLE_LOCATION_DIP = 200;
    private static final int STATE_DELEGATING = 4;
    private static final int STATE_DRAGGING = 2;
    private static final int STATE_GESTURE_DETECTING = 5;
    private static final int STATE_TOUCH_EXPLORING = 1;
    private final AccessibilityManagerService mAms;
    private final Context mContext;
    private final int mDoubleTapSlop;
    private int mDraggingPointerId;
    private final AccessibilityGestureDetector mGestureDetector;
    private final Handler mHandler;
    private int mLastTouchedWindowId;
    private int mLongPressingPointerDeltaX;
    private int mLongPressingPointerDeltaY;
    private final int mScaledMinPointerDistanceToUseMiddleLocation;
    private boolean mTouchExplorationInProgress;
    private int mCurrentState = 1;
    private final Point mTempPoint = new Point();
    private int mLongPressingPointerId = -1;
    private final ReceivedPointerTracker mReceivedPointerTracker = new ReceivedPointerTracker();
    private final InjectedPointerTracker mInjectedPointerTracker = new InjectedPointerTracker();
    private final int mDetermineUserIntentTimeout = ViewConfiguration.getDoubleTapTimeout();
    private final ExitGestureDetectionModeDelayed mExitGestureDetectionModeDelayed = new ExitGestureDetectionModeDelayed();
    private final SendHoverEnterAndMoveDelayed mSendHoverEnterAndMoveDelayed = new SendHoverEnterAndMoveDelayed();
    private final SendHoverExitDelayed mSendHoverExitDelayed = new SendHoverExitDelayed();
    private final SendAccessibilityEventDelayed mSendTouchExplorationEndDelayed = new SendAccessibilityEventDelayed(1024, this.mDetermineUserIntentTimeout);
    private final SendAccessibilityEventDelayed mSendTouchInteractionEndDelayed = new SendAccessibilityEventDelayed(DumpState.DUMP_COMPILER_STATS, this.mDetermineUserIntentTimeout);

    public TouchExplorer(Context context, AccessibilityManagerService accessibilityManagerService) {
        this.mContext = context;
        this.mAms = accessibilityManagerService;
        this.mDoubleTapSlop = ViewConfiguration.get(context).getScaledDoubleTapSlop();
        this.mHandler = new Handler(context.getMainLooper());
        this.mGestureDetector = new AccessibilityGestureDetector(context, this);
        this.mScaledMinPointerDistanceToUseMiddleLocation = (int) (200.0f * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public void clearEvents(int i) {
        if (i == 4098) {
            clear();
        }
        super.clearEvents(i);
    }

    @Override
    public void onDestroy() {
        clear();
    }

    private void clear() {
        if (this.mReceivedPointerTracker.getLastReceivedEvent() != null) {
            clear(this.mReceivedPointerTracker.getLastReceivedEvent(), 33554432);
        }
    }

    private void clear(MotionEvent motionEvent, int i) {
        int i2 = this.mCurrentState;
        if (i2 != 4) {
            switch (i2) {
                case 1:
                    sendHoverExitAndTouchExplorationGestureEndIfNeeded(i);
                    break;
                case 2:
                    this.mDraggingPointerId = -1;
                    sendUpForInjectedDownPointers(motionEvent, i);
                    break;
            }
        } else {
            sendUpForInjectedDownPointers(motionEvent, i);
        }
        this.mSendHoverEnterAndMoveDelayed.cancel();
        this.mSendHoverExitDelayed.cancel();
        this.mExitGestureDetectionModeDelayed.cancel();
        this.mSendTouchExplorationEndDelayed.cancel();
        this.mSendTouchInteractionEndDelayed.cancel();
        this.mReceivedPointerTracker.clear();
        this.mInjectedPointerTracker.clear();
        this.mGestureDetector.clear();
        this.mLongPressingPointerId = -1;
        this.mLongPressingPointerDeltaX = 0;
        this.mLongPressingPointerDeltaY = 0;
        this.mCurrentState = 1;
        this.mTouchExplorationInProgress = false;
        this.mAms.onTouchInteractionEnd();
    }

    @Override
    public void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
        if (!motionEvent.isFromSource(UsbACInterface.FORMAT_II_AC3)) {
            super.onMotionEvent(motionEvent, motionEvent2, i);
        }
        this.mReceivedPointerTracker.onMotionEvent(motionEvent2);
        if (this.mGestureDetector.onMotionEvent(motionEvent2, i)) {
            return;
        }
        if (motionEvent.getActionMasked() == 3) {
            clear(motionEvent, i);
            return;
        }
        switch (this.mCurrentState) {
            case 1:
                handleMotionEventStateTouchExploring(motionEvent, motionEvent2, i);
                break;
            case 2:
                handleMotionEventStateDragging(motionEvent, i);
                break;
            case 3:
            default:
                Slog.e(LOG_TAG, "Illegal state: " + this.mCurrentState);
                clear(motionEvent, i);
                break;
            case 4:
                handleMotionEventStateDelegating(motionEvent, i);
                break;
            case 5:
                break;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        int eventType = accessibilityEvent.getEventType();
        if (this.mSendTouchExplorationEndDelayed.isPending() && eventType == 256) {
            this.mSendTouchExplorationEndDelayed.cancel();
            sendAccessibilityEvent(1024);
        }
        if (this.mSendTouchInteractionEndDelayed.isPending() && eventType == 256) {
            this.mSendTouchInteractionEndDelayed.cancel();
            sendAccessibilityEvent(DumpState.DUMP_COMPILER_STATS);
        }
        if (eventType == 32) {
            if (this.mInjectedPointerTracker.mLastInjectedHoverEventForClick != null) {
                this.mInjectedPointerTracker.mLastInjectedHoverEventForClick.recycle();
                this.mInjectedPointerTracker.mLastInjectedHoverEventForClick = null;
            }
            this.mLastTouchedWindowId = -1;
        } else if (eventType == 128 || eventType == 256) {
            this.mLastTouchedWindowId = accessibilityEvent.getWindowId();
        } else if (eventType == 32768) {
        }
        super.onAccessibilityEvent(accessibilityEvent);
    }

    @Override
    public void onDoubleTapAndHold(MotionEvent motionEvent, int i) {
        if (this.mCurrentState != 1 || this.mReceivedPointerTracker.getLastReceivedEvent().getPointerCount() == 0) {
            return;
        }
        int actionIndex = motionEvent.getActionIndex();
        int pointerId = motionEvent.getPointerId(actionIndex);
        Point point = this.mTempPoint;
        if (computeClickLocation(point) == 0) {
            return;
        }
        this.mLongPressingPointerId = pointerId;
        this.mLongPressingPointerDeltaX = ((int) motionEvent.getX(actionIndex)) - point.x;
        this.mLongPressingPointerDeltaY = ((int) motionEvent.getY(actionIndex)) - point.y;
        sendHoverExitAndTouchExplorationGestureEndIfNeeded(i);
        this.mCurrentState = 4;
        sendDownForAllNotInjectedPointers(motionEvent, i);
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent, int i) {
        if (this.mCurrentState != 1) {
            return false;
        }
        this.mSendHoverEnterAndMoveDelayed.cancel();
        this.mSendHoverExitDelayed.cancel();
        if (this.mSendTouchExplorationEndDelayed.isPending()) {
            this.mSendTouchExplorationEndDelayed.forceSendAndRemove();
        }
        if (this.mSendTouchInteractionEndDelayed.isPending()) {
            this.mSendTouchInteractionEndDelayed.forceSendAndRemove();
        }
        if (this.mAms.performActionOnAccessibilityFocusedItem(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)) {
            return true;
        }
        Slog.e(LOG_TAG, "ACTION_CLICK failed. Dispatching motion events to simulate click.");
        int actionIndex = motionEvent.getActionIndex();
        motionEvent.getPointerId(actionIndex);
        int iComputeClickLocation = computeClickLocation(this.mTempPoint);
        if (iComputeClickLocation == 0) {
            return true;
        }
        MotionEvent.PointerProperties[] pointerPropertiesArr = {new MotionEvent.PointerProperties()};
        motionEvent.getPointerProperties(actionIndex, pointerPropertiesArr[0]);
        MotionEvent.PointerCoords[] pointerCoordsArr = {new MotionEvent.PointerCoords()};
        pointerCoordsArr[0].x = r5.x;
        pointerCoordsArr[0].y = r5.y;
        MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent.getDownTime(), motionEvent.getEventTime(), 0, 1, pointerPropertiesArr, pointerCoordsArr, 0, 0, 1.0f, 1.0f, motionEvent.getDeviceId(), 0, motionEvent.getSource(), motionEvent.getFlags());
        sendActionDownAndUp(motionEventObtain, i, iComputeClickLocation == 1);
        motionEventObtain.recycle();
        return true;
    }

    @Override
    public boolean onGestureStarted() {
        this.mCurrentState = 5;
        this.mSendHoverEnterAndMoveDelayed.cancel();
        this.mSendHoverExitDelayed.cancel();
        this.mExitGestureDetectionModeDelayed.post();
        sendAccessibilityEvent(DumpState.DUMP_DOMAIN_PREFERRED);
        return false;
    }

    @Override
    public boolean onGestureCompleted(int i) {
        if (this.mCurrentState != 5) {
            return false;
        }
        endGestureDetection();
        this.mAms.onGesture(i);
        return true;
    }

    @Override
    public boolean onGestureCancelled(MotionEvent motionEvent, int i) {
        if (this.mCurrentState == 5) {
            endGestureDetection();
            return true;
        }
        if (this.mCurrentState == 1 && motionEvent.getActionMasked() == 2) {
            int primaryPointerId = 1 << this.mReceivedPointerTracker.getPrimaryPointerId();
            this.mSendHoverEnterAndMoveDelayed.addEvent(motionEvent);
            this.mSendHoverEnterAndMoveDelayed.forceSendAndRemove();
            this.mSendHoverExitDelayed.cancel();
            sendMotionEvent(motionEvent, 7, primaryPointerId, i);
            return true;
        }
        return false;
    }

    private void handleMotionEventStateTouchExploring(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
        ReceivedPointerTracker receivedPointerTracker = this.mReceivedPointerTracker;
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 5) {
            switch (actionMasked) {
                case 0:
                    this.mAms.onTouchInteractionStart();
                    sendAccessibilityEvent(DumpState.DUMP_DEXOPT);
                    this.mSendHoverEnterAndMoveDelayed.cancel();
                    this.mSendHoverExitDelayed.cancel();
                    if (this.mSendTouchExplorationEndDelayed.isPending()) {
                        this.mSendTouchExplorationEndDelayed.forceSendAndRemove();
                    }
                    if (this.mSendTouchInteractionEndDelayed.isPending()) {
                        this.mSendTouchInteractionEndDelayed.forceSendAndRemove();
                    }
                    if (!this.mGestureDetector.firstTapDetected() && !this.mTouchExplorationInProgress) {
                        if (!this.mSendHoverEnterAndMoveDelayed.isPending()) {
                            this.mSendHoverEnterAndMoveDelayed.post(motionEvent, true, 1 << receivedPointerTracker.getPrimaryPointerId(), i);
                        } else {
                            this.mSendHoverEnterAndMoveDelayed.addEvent(motionEvent);
                        }
                        break;
                    }
                    break;
                case 1:
                    this.mAms.onTouchInteractionEnd();
                    int pointerId = 1 << motionEvent.getPointerId(motionEvent.getActionIndex());
                    if (this.mSendHoverEnterAndMoveDelayed.isPending()) {
                        this.mSendHoverExitDelayed.post(motionEvent, pointerId, i);
                    } else {
                        sendHoverExitAndTouchExplorationGestureEndIfNeeded(i);
                    }
                    if (!this.mSendTouchInteractionEndDelayed.isPending()) {
                        this.mSendTouchInteractionEndDelayed.post();
                    }
                    break;
                case 2:
                    int primaryPointerId = receivedPointerTracker.getPrimaryPointerId();
                    int iFindPointerIndex = motionEvent.findPointerIndex(primaryPointerId);
                    int i2 = 1 << primaryPointerId;
                    switch (motionEvent.getPointerCount()) {
                        case 1:
                            if (this.mSendHoverEnterAndMoveDelayed.isPending()) {
                                this.mSendHoverEnterAndMoveDelayed.addEvent(motionEvent);
                            } else if (this.mTouchExplorationInProgress) {
                                sendTouchExplorationGestureStartAndHoverEnterIfNeeded(i);
                                sendMotionEvent(motionEvent, 7, i2, i);
                            }
                            break;
                        case 2:
                            if (this.mSendHoverEnterAndMoveDelayed.isPending()) {
                                this.mSendHoverEnterAndMoveDelayed.cancel();
                                this.mSendHoverExitDelayed.cancel();
                            } else if (this.mTouchExplorationInProgress) {
                                if (Math.hypot(receivedPointerTracker.getReceivedPointerDownX(primaryPointerId) - motionEvent2.getX(iFindPointerIndex), receivedPointerTracker.getReceivedPointerDownY(primaryPointerId) - motionEvent2.getY(iFindPointerIndex)) >= this.mDoubleTapSlop) {
                                    sendHoverExitAndTouchExplorationGestureEndIfNeeded(i);
                                }
                            }
                            if (isDraggingGesture(motionEvent)) {
                                this.mCurrentState = 2;
                                this.mDraggingPointerId = primaryPointerId;
                                motionEvent.setEdgeFlags(receivedPointerTracker.getLastReceivedDownEdgeFlags());
                                sendMotionEvent(motionEvent, 0, i2, i);
                            } else {
                                this.mCurrentState = 4;
                                sendDownForAllNotInjectedPointers(motionEvent, i);
                            }
                            break;
                        default:
                            if (this.mSendHoverEnterAndMoveDelayed.isPending()) {
                                this.mSendHoverEnterAndMoveDelayed.cancel();
                                this.mSendHoverExitDelayed.cancel();
                            } else {
                                sendHoverExitAndTouchExplorationGestureEndIfNeeded(i);
                            }
                            this.mCurrentState = 4;
                            sendDownForAllNotInjectedPointers(motionEvent, i);
                            break;
                    }
                    break;
            }
        }
        this.mSendHoverEnterAndMoveDelayed.cancel();
        this.mSendHoverExitDelayed.cancel();
    }

    private void handleMotionEventStateDragging(MotionEvent motionEvent, int i) {
        int i2;
        if (motionEvent.findPointerIndex(this.mDraggingPointerId) == -1) {
            Slog.e(LOG_TAG, "mDraggingPointerId doesn't match any pointers on current event. mDraggingPointerId: " + Integer.toString(this.mDraggingPointerId) + ", Event: " + motionEvent);
            this.mDraggingPointerId = -1;
            i2 = 0;
        } else {
            i2 = 1 << this.mDraggingPointerId;
        }
        switch (motionEvent.getActionMasked()) {
            case 0:
                Slog.e(LOG_TAG, "Dragging state can be reached only if two pointers are already down");
                clear(motionEvent, i);
                break;
            case 1:
                this.mAms.onTouchInteractionEnd();
                sendAccessibilityEvent(DumpState.DUMP_COMPILER_STATS);
                if (motionEvent.getPointerId(motionEvent.getActionIndex()) == this.mDraggingPointerId) {
                    this.mDraggingPointerId = -1;
                    sendMotionEvent(motionEvent, 1, i2, i);
                }
                this.mCurrentState = 1;
                break;
            case 2:
                if (this.mDraggingPointerId != -1) {
                    switch (motionEvent.getPointerCount()) {
                        case 1:
                            break;
                        case 2:
                            if (isDraggingGesture(motionEvent)) {
                                float x = motionEvent.getX(0);
                                float y = motionEvent.getY(0);
                                float x2 = x - motionEvent.getX(1);
                                float y2 = y - motionEvent.getY(1);
                                if (Math.hypot(x2, y2) > this.mScaledMinPointerDistanceToUseMiddleLocation) {
                                    motionEvent.setLocation(x2 / 2.0f, y2 / 2.0f);
                                }
                                sendMotionEvent(motionEvent, 2, i2, i);
                            } else {
                                this.mCurrentState = 4;
                                sendMotionEvent(motionEvent, 1, i2, i);
                                sendDownForAllNotInjectedPointers(motionEvent, i);
                            }
                            break;
                        default:
                            this.mCurrentState = 4;
                            sendMotionEvent(motionEvent, 1, i2, i);
                            sendDownForAllNotInjectedPointers(motionEvent, i);
                            break;
                    }
                }
                break;
            case 5:
                this.mCurrentState = 4;
                if (this.mDraggingPointerId != -1) {
                    sendMotionEvent(motionEvent, 1, i2, i);
                }
                sendDownForAllNotInjectedPointers(motionEvent, i);
                break;
            case 6:
                if (motionEvent.getPointerId(motionEvent.getActionIndex()) == this.mDraggingPointerId) {
                    this.mDraggingPointerId = -1;
                    sendMotionEvent(motionEvent, 1, i2, i);
                }
                break;
        }
    }

    private void handleMotionEventStateDelegating(MotionEvent motionEvent, int i) {
        switch (motionEvent.getActionMasked()) {
            case 0:
                Slog.e(LOG_TAG, "Delegating state can only be reached if there is at least one pointer down!");
                clear(motionEvent, i);
                break;
            case 1:
                if (this.mLongPressingPointerId >= 0) {
                    motionEvent = offsetEvent(motionEvent, -this.mLongPressingPointerDeltaX, -this.mLongPressingPointerDeltaY);
                    this.mLongPressingPointerId = -1;
                    this.mLongPressingPointerDeltaX = 0;
                    this.mLongPressingPointerDeltaY = 0;
                }
                sendMotionEvent(motionEvent, motionEvent.getAction(), -1, i);
                this.mAms.onTouchInteractionEnd();
                sendAccessibilityEvent(DumpState.DUMP_COMPILER_STATS);
                this.mCurrentState = 1;
                break;
            default:
                sendMotionEvent(motionEvent, motionEvent.getAction(), -1, i);
                break;
        }
    }

    private void endGestureDetection() {
        this.mAms.onTouchInteractionEnd();
        sendAccessibilityEvent(DumpState.DUMP_FROZEN);
        sendAccessibilityEvent(DumpState.DUMP_COMPILER_STATS);
        this.mExitGestureDetectionModeDelayed.cancel();
        this.mCurrentState = 1;
    }

    private void sendAccessibilityEvent(int i) {
        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(this.mContext);
        if (accessibilityManager.isEnabled()) {
            AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(i);
            accessibilityEventObtain.setWindowId(this.mAms.getActiveWindowId());
            accessibilityManager.sendAccessibilityEvent(accessibilityEventObtain);
            if (i == 512) {
                this.mTouchExplorationInProgress = true;
            } else if (i == 1024) {
                this.mTouchExplorationInProgress = false;
            }
        }
    }

    private void sendDownForAllNotInjectedPointers(MotionEvent motionEvent, int i) {
        InjectedPointerTracker injectedPointerTracker = this.mInjectedPointerTracker;
        int pointerCount = motionEvent.getPointerCount();
        int i2 = 0;
        for (int i3 = 0; i3 < pointerCount; i3++) {
            int pointerId = motionEvent.getPointerId(i3);
            if (!injectedPointerTracker.isInjectedPointerDown(pointerId)) {
                i2 |= 1 << pointerId;
                sendMotionEvent(motionEvent, computeInjectionAction(0, i3), i2, i);
            }
        }
    }

    private void sendHoverExitAndTouchExplorationGestureEndIfNeeded(int i) {
        MotionEvent lastInjectedHoverEvent = this.mInjectedPointerTracker.getLastInjectedHoverEvent();
        if (lastInjectedHoverEvent != null && lastInjectedHoverEvent.getActionMasked() != 10) {
            int pointerIdBits = lastInjectedHoverEvent.getPointerIdBits();
            if (!this.mSendTouchExplorationEndDelayed.isPending()) {
                this.mSendTouchExplorationEndDelayed.post();
            }
            sendMotionEvent(lastInjectedHoverEvent, 10, pointerIdBits, i);
        }
    }

    private void sendTouchExplorationGestureStartAndHoverEnterIfNeeded(int i) {
        MotionEvent lastInjectedHoverEvent = this.mInjectedPointerTracker.getLastInjectedHoverEvent();
        if (lastInjectedHoverEvent != null && lastInjectedHoverEvent.getActionMasked() == 10) {
            int pointerIdBits = lastInjectedHoverEvent.getPointerIdBits();
            sendAccessibilityEvent(512);
            sendMotionEvent(lastInjectedHoverEvent, 9, pointerIdBits, i);
        }
    }

    private void sendUpForInjectedDownPointers(MotionEvent motionEvent, int i) {
        InjectedPointerTracker injectedPointerTracker = this.mInjectedPointerTracker;
        int pointerCount = motionEvent.getPointerCount();
        int i2 = 0;
        for (int i3 = 0; i3 < pointerCount; i3++) {
            int pointerId = motionEvent.getPointerId(i3);
            if (injectedPointerTracker.isInjectedPointerDown(pointerId)) {
                i2 |= 1 << pointerId;
                sendMotionEvent(motionEvent, computeInjectionAction(1, i3), i2, i);
            }
        }
    }

    private void sendActionDownAndUp(MotionEvent motionEvent, int i, boolean z) {
        int pointerId = 1 << motionEvent.getPointerId(motionEvent.getActionIndex());
        motionEvent.setTargetAccessibilityFocus(z);
        sendMotionEvent(motionEvent, 0, pointerId, i);
        motionEvent.setTargetAccessibilityFocus(z);
        sendMotionEvent(motionEvent, 1, pointerId, i);
    }

    private void sendMotionEvent(MotionEvent motionEvent, int i, int i2, int i3) {
        MotionEvent motionEventSplit;
        motionEvent.setAction(i);
        if (i2 != -1) {
            try {
                motionEventSplit = motionEvent.split(i2);
            } catch (IllegalArgumentException e) {
                Slog.e(LOG_TAG, "sendMotionEvent: Failed to split motion event: " + e);
                return;
            }
        } else {
            motionEventSplit = motionEvent;
        }
        if (i == 0) {
            motionEventSplit.setDownTime(motionEventSplit.getEventTime());
        } else {
            motionEventSplit.setDownTime(this.mInjectedPointerTracker.getLastInjectedDownEventTime());
        }
        if (this.mLongPressingPointerId >= 0) {
            motionEventSplit = offsetEvent(motionEventSplit, -this.mLongPressingPointerDeltaX, -this.mLongPressingPointerDeltaY);
        }
        super.onMotionEvent(motionEventSplit, null, 1073741824 | i3);
        this.mInjectedPointerTracker.onMotionEvent(motionEventSplit);
        if (motionEventSplit != motionEvent) {
            motionEventSplit.recycle();
        }
    }

    private MotionEvent offsetEvent(MotionEvent motionEvent, int i, int i2) {
        if (i == 0 && i2 == 0) {
            return motionEvent;
        }
        int iFindPointerIndex = motionEvent.findPointerIndex(this.mLongPressingPointerId);
        int pointerCount = motionEvent.getPointerCount();
        MotionEvent.PointerProperties[] pointerPropertiesArrCreateArray = MotionEvent.PointerProperties.createArray(pointerCount);
        MotionEvent.PointerCoords[] pointerCoordsArrCreateArray = MotionEvent.PointerCoords.createArray(pointerCount);
        for (int i3 = 0; i3 < pointerCount; i3++) {
            motionEvent.getPointerProperties(i3, pointerPropertiesArrCreateArray[i3]);
            motionEvent.getPointerCoords(i3, pointerCoordsArrCreateArray[i3]);
            if (i3 == iFindPointerIndex) {
                pointerCoordsArrCreateArray[i3].x += i;
                pointerCoordsArrCreateArray[i3].y += i2;
            }
        }
        return MotionEvent.obtain(motionEvent.getDownTime(), motionEvent.getEventTime(), motionEvent.getAction(), motionEvent.getPointerCount(), pointerPropertiesArrCreateArray, pointerCoordsArrCreateArray, motionEvent.getMetaState(), motionEvent.getButtonState(), 1.0f, 1.0f, motionEvent.getDeviceId(), motionEvent.getEdgeFlags(), motionEvent.getSource(), motionEvent.getFlags());
    }

    private int computeInjectionAction(int i, int i2) {
        if (i != 0) {
            switch (i) {
                case 5:
                    break;
                case 6:
                    if (this.mInjectedPointerTracker.getInjectedPointerDownCount() == 1) {
                        return 1;
                    }
                    return (i2 << 8) | 6;
                default:
                    return i;
            }
        }
        if (this.mInjectedPointerTracker.getInjectedPointerDownCount() == 0) {
            return 0;
        }
        return (i2 << 8) | 5;
    }

    private boolean isDraggingGesture(MotionEvent motionEvent) {
        ReceivedPointerTracker receivedPointerTracker = this.mReceivedPointerTracker;
        return GestureUtils.isDraggingGesture(receivedPointerTracker.getReceivedPointerDownX(0), receivedPointerTracker.getReceivedPointerDownY(0), receivedPointerTracker.getReceivedPointerDownX(1), receivedPointerTracker.getReceivedPointerDownY(1), motionEvent.getX(0), motionEvent.getY(0), motionEvent.getX(1), motionEvent.getY(1), MAX_DRAGGING_ANGLE_COS);
    }

    private int computeClickLocation(Point point) {
        MotionEvent lastInjectedHoverEventForClick = this.mInjectedPointerTracker.getLastInjectedHoverEventForClick();
        if (lastInjectedHoverEventForClick != null) {
            int actionIndex = lastInjectedHoverEventForClick.getActionIndex();
            point.x = (int) lastInjectedHoverEventForClick.getX(actionIndex);
            point.y = (int) lastInjectedHoverEventForClick.getY(actionIndex);
            if (!this.mAms.accessibilityFocusOnlyInActiveWindow() || this.mLastTouchedWindowId == this.mAms.getActiveWindowId()) {
                if (this.mAms.getAccessibilityFocusClickPointInScreen(point)) {
                    return 1;
                }
                return 2;
            }
        }
        if (this.mAms.getAccessibilityFocusClickPointInScreen(point)) {
            return 1;
        }
        return 0;
    }

    private static String getStateSymbolicName(int i) {
        switch (i) {
            case 1:
                return "STATE_TOUCH_EXPLORING";
            case 2:
                return "STATE_DRAGGING";
            case 3:
            default:
                return "Unknown state: " + i;
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
            TouchExplorer.this.sendAccessibilityEvent(DumpState.DUMP_FROZEN);
            TouchExplorer.this.sendAccessibilityEvent(512);
            TouchExplorer.this.clear();
        }
    }

    class SendHoverEnterAndMoveDelayed implements Runnable {
        private final String LOG_TAG_SEND_HOVER_DELAYED = "SendHoverEnterAndMoveDelayed";
        private final List<MotionEvent> mEvents = new ArrayList();
        private int mPointerIdBits;
        private int mPolicyFlags;

        SendHoverEnterAndMoveDelayed() {
        }

        public void post(MotionEvent motionEvent, boolean z, int i, int i2) {
            cancel();
            addEvent(motionEvent);
            this.mPointerIdBits = i;
            this.mPolicyFlags = i2;
            TouchExplorer.this.mHandler.postDelayed(this, TouchExplorer.this.mDetermineUserIntentTimeout);
        }

        public void addEvent(MotionEvent motionEvent) {
            this.mEvents.add(MotionEvent.obtain(motionEvent));
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
            for (int size = this.mEvents.size() - 1; size >= 0; size--) {
                this.mEvents.remove(size).recycle();
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
                int size = this.mEvents.size();
                for (int i = 1; i < size; i++) {
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

        public void post(MotionEvent motionEvent, int i, int i2) {
            cancel();
            this.mPrototype = MotionEvent.obtain(motionEvent);
            this.mPointerIdBits = i;
            this.mPolicyFlags = i2;
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

        public SendAccessibilityEventDelayed(int i, int i2) {
            this.mEventType = i;
            this.mDelay = i2;
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

        public void onMotionEvent(MotionEvent motionEvent) {
            switch (motionEvent.getActionMasked()) {
                case 0:
                case 5:
                    this.mInjectedPointersDown = (1 << motionEvent.getPointerId(motionEvent.getActionIndex())) | this.mInjectedPointersDown;
                    this.mLastInjectedDownEventTime = motionEvent.getDownTime();
                    break;
                case 1:
                case 6:
                    int pointerId = 1 << motionEvent.getPointerId(motionEvent.getActionIndex());
                    this.mInjectedPointersDown = (~pointerId) & this.mInjectedPointersDown;
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
                    this.mLastInjectedHoverEvent = MotionEvent.obtain(motionEvent);
                    if (this.mLastInjectedHoverEventForClick != null) {
                        this.mLastInjectedHoverEventForClick.recycle();
                    }
                    this.mLastInjectedHoverEventForClick = MotionEvent.obtain(motionEvent);
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

        public boolean isInjectedPointerDown(int i) {
            return ((1 << i) & this.mInjectedPointersDown) != 0;
        }

        public MotionEvent getLastInjectedHoverEvent() {
            return this.mLastInjectedHoverEvent;
        }

        public MotionEvent getLastInjectedHoverEventForClick() {
            return this.mLastInjectedHoverEventForClick;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=========================");
            sb.append("\nDown pointers #");
            sb.append(Integer.bitCount(this.mInjectedPointersDown));
            sb.append(" [ ");
            for (int i = 0; i < 32; i++) {
                if ((this.mInjectedPointersDown & i) != 0) {
                    sb.append(i);
                    sb.append(" ");
                }
            }
            sb.append("]");
            sb.append("\n=========================");
            return sb.toString();
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

        public void onMotionEvent(MotionEvent motionEvent) {
            if (this.mLastReceivedEvent != null) {
                this.mLastReceivedEvent.recycle();
            }
            this.mLastReceivedEvent = MotionEvent.obtain(motionEvent);
            switch (motionEvent.getActionMasked()) {
                case 0:
                    handleReceivedPointerDown(motionEvent.getActionIndex(), motionEvent);
                    break;
                case 1:
                    handleReceivedPointerUp(motionEvent.getActionIndex(), motionEvent);
                    break;
                case 5:
                    handleReceivedPointerDown(motionEvent.getActionIndex(), motionEvent);
                    break;
                case 6:
                    handleReceivedPointerUp(motionEvent.getActionIndex(), motionEvent);
                    break;
            }
        }

        public MotionEvent getLastReceivedEvent() {
            return this.mLastReceivedEvent;
        }

        public int getReceivedPointerDownCount() {
            return Integer.bitCount(this.mReceivedPointersDown);
        }

        public boolean isReceivedPointerDown(int i) {
            return ((1 << i) & this.mReceivedPointersDown) != 0;
        }

        public float getReceivedPointerDownX(int i) {
            return this.mReceivedPointerDownX[i];
        }

        public float getReceivedPointerDownY(int i) {
            return this.mReceivedPointerDownY[i];
        }

        public long getReceivedPointerDownTime(int i) {
            return this.mReceivedPointerDownTime[i];
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

        private void handleReceivedPointerDown(int i, MotionEvent motionEvent) {
            int pointerId = motionEvent.getPointerId(i);
            this.mLastReceivedUpPointerDownTime = 0L;
            this.mLastReceivedUpPointerDownX = 0.0f;
            this.mLastReceivedUpPointerDownX = 0.0f;
            this.mLastReceivedDownEdgeFlags = motionEvent.getEdgeFlags();
            this.mReceivedPointersDown = (1 << pointerId) | this.mReceivedPointersDown;
            this.mReceivedPointerDownX[pointerId] = motionEvent.getX(i);
            this.mReceivedPointerDownY[pointerId] = motionEvent.getY(i);
            this.mReceivedPointerDownTime[pointerId] = motionEvent.getEventTime();
            this.mPrimaryPointerId = pointerId;
        }

        private void handleReceivedPointerUp(int i, MotionEvent motionEvent) {
            int pointerId = motionEvent.getPointerId(i);
            this.mLastReceivedUpPointerDownTime = getReceivedPointerDownTime(pointerId);
            this.mLastReceivedUpPointerDownX = this.mReceivedPointerDownX[pointerId];
            this.mLastReceivedUpPointerDownY = this.mReceivedPointerDownY[pointerId];
            this.mReceivedPointersDown = (~(1 << pointerId)) & this.mReceivedPointersDown;
            this.mReceivedPointerDownX[pointerId] = 0.0f;
            this.mReceivedPointerDownY[pointerId] = 0.0f;
            this.mReceivedPointerDownTime[pointerId] = 0;
            if (this.mPrimaryPointerId == pointerId) {
                this.mPrimaryPointerId = -1;
            }
        }

        private int findPrimaryPointerId() {
            int i = this.mReceivedPointersDown;
            int i2 = -1;
            long j = JobStatus.NO_LATEST_RUNTIME;
            while (i > 0) {
                int iNumberOfTrailingZeros = Integer.numberOfTrailingZeros(i);
                i &= ~(1 << iNumberOfTrailingZeros);
                long j2 = this.mReceivedPointerDownTime[iNumberOfTrailingZeros];
                if (j2 < j) {
                    i2 = iNumberOfTrailingZeros;
                    j = j2;
                }
            }
            return i2;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=========================");
            sb.append("\nDown pointers #");
            sb.append(getReceivedPointerDownCount());
            sb.append(" [ ");
            for (int i = 0; i < 32; i++) {
                if (isReceivedPointerDown(i)) {
                    sb.append(i);
                    sb.append(" ");
                }
            }
            sb.append("]");
            sb.append("\nPrimary pointer id [ ");
            sb.append(getPrimaryPointerId());
            sb.append(" ]");
            sb.append("\n=========================");
            return sb.toString();
        }
    }
}
