package com.android.server.accessibility;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.MathUtils;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.UsbACInterface;
import java.util.Queue;

class MagnificationGestureHandler extends BaseEventStreamTransformation {
    private static final boolean DEBUG_ALL = false;
    private static final boolean DEBUG_DETECTING = false;
    private static final boolean DEBUG_EVENT_STREAM = false;
    private static final boolean DEBUG_PANNING_SCALING = false;
    private static final boolean DEBUG_STATE_TRANSITIONS = false;
    private static final String LOG_TAG = "MagnificationGestureHandler";
    private static final float MAX_SCALE = 5.0f;
    private static final float MIN_SCALE = 2.0f;

    @VisibleForTesting
    State mCurrentState;
    private final Queue<MotionEvent> mDebugInputEventHistory;
    private final Queue<MotionEvent> mDebugOutputEventHistory;
    final boolean mDetectShortcutTrigger;
    final boolean mDetectTripleTap;

    @VisibleForTesting
    final DetectingState mDetectingState;

    @VisibleForTesting
    final MagnificationController mMagnificationController;

    @VisibleForTesting
    final PanningScalingState mPanningScalingState;

    @VisibleForTesting
    State mPreviousState;
    private final ScreenStateReceiver mScreenStateReceiver;
    private MotionEvent.PointerCoords[] mTempPointerCoords;
    private MotionEvent.PointerProperties[] mTempPointerProperties;

    @VisibleForTesting
    final DelegatingState mDelegatingState = new DelegatingState();

    @VisibleForTesting
    final ViewportDraggingState mViewportDraggingState = new ViewportDraggingState();

    public MagnificationGestureHandler(Context context, MagnificationController magnificationController, boolean z, boolean z2) {
        this.mMagnificationController = magnificationController;
        this.mDetectingState = new DetectingState(context);
        this.mPanningScalingState = new PanningScalingState(context);
        this.mDetectTripleTap = z;
        this.mDetectShortcutTrigger = z2;
        if (this.mDetectShortcutTrigger) {
            this.mScreenStateReceiver = new ScreenStateReceiver(context, this);
            this.mScreenStateReceiver.register();
        } else {
            this.mScreenStateReceiver = null;
        }
        this.mDebugInputEventHistory = null;
        this.mDebugOutputEventHistory = null;
        transitionTo(this.mDetectingState);
    }

    @Override
    public void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
        onMotionEventInternal(motionEvent, motionEvent2, i);
    }

    private void onMotionEventInternal(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
        if ((!this.mDetectTripleTap && !this.mDetectShortcutTrigger) || !motionEvent.isFromSource(UsbACInterface.FORMAT_II_AC3)) {
            dispatchTransformedEvent(motionEvent, motionEvent2, i);
        } else {
            handleEventWith(this.mCurrentState, motionEvent, motionEvent2, i);
        }
    }

    private void handleEventWith(State state, MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
        this.mPanningScalingState.mScrollGestureDetector.onTouchEvent(motionEvent);
        this.mPanningScalingState.mScaleGestureDetector.onTouchEvent(motionEvent);
        state.onMotionEvent(motionEvent, motionEvent2, i);
    }

    @Override
    public void clearEvents(int i) {
        if (i == 4098) {
            clearAndTransitionToStateDetecting();
        }
        super.clearEvents(i);
    }

    @Override
    public void onDestroy() {
        if (this.mScreenStateReceiver != null) {
            this.mScreenStateReceiver.unregister();
        }
        clearAndTransitionToStateDetecting();
    }

    void notifyShortcutTriggered() {
        if (this.mDetectShortcutTrigger) {
            if (this.mMagnificationController.resetIfNeeded(true)) {
                clearAndTransitionToStateDetecting();
            } else {
                this.mDetectingState.toggleShortcutTriggered();
            }
        }
    }

    void clearAndTransitionToStateDetecting() {
        this.mCurrentState = this.mDetectingState;
        this.mDetectingState.clear();
        this.mViewportDraggingState.clear();
        this.mPanningScalingState.clear();
    }

    private void dispatchTransformedEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
        MotionEvent motionEventObtain = motionEvent;
        if (this.mMagnificationController.isMagnifying() && motionEventObtain.isFromSource(UsbACInterface.FORMAT_II_AC3) && this.mMagnificationController.magnificationRegionContains(motionEvent.getX(), motionEvent.getY())) {
            float scale = this.mMagnificationController.getScale();
            float offsetX = this.mMagnificationController.getOffsetX();
            float offsetY = this.mMagnificationController.getOffsetY();
            int pointerCount = motionEvent.getPointerCount();
            MotionEvent.PointerCoords[] tempPointerCoordsWithMinSize = getTempPointerCoordsWithMinSize(pointerCount);
            MotionEvent.PointerProperties[] tempPointerPropertiesWithMinSize = getTempPointerPropertiesWithMinSize(pointerCount);
            for (int i2 = 0; i2 < pointerCount; i2++) {
                motionEventObtain.getPointerCoords(i2, tempPointerCoordsWithMinSize[i2]);
                tempPointerCoordsWithMinSize[i2].x = (tempPointerCoordsWithMinSize[i2].x - offsetX) / scale;
                tempPointerCoordsWithMinSize[i2].y = (tempPointerCoordsWithMinSize[i2].y - offsetY) / scale;
                motionEventObtain.getPointerProperties(i2, tempPointerPropertiesWithMinSize[i2]);
            }
            motionEventObtain = MotionEvent.obtain(motionEvent.getDownTime(), motionEvent.getEventTime(), motionEvent.getAction(), pointerCount, tempPointerPropertiesWithMinSize, tempPointerCoordsWithMinSize, 0, 0, 1.0f, 1.0f, motionEvent.getDeviceId(), 0, motionEvent.getSource(), motionEvent.getFlags());
        }
        super.onMotionEvent(motionEventObtain, motionEvent2, i);
    }

    private static void storeEventInto(Queue<MotionEvent> queue, MotionEvent motionEvent) {
        queue.add(MotionEvent.obtain(motionEvent));
        while (!queue.isEmpty() && motionEvent.getEventTime() - queue.peek().getEventTime() > 5000) {
            queue.remove().recycle();
        }
    }

    private MotionEvent.PointerCoords[] getTempPointerCoordsWithMinSize(int i) {
        int length = this.mTempPointerCoords != null ? this.mTempPointerCoords.length : 0;
        if (length < i) {
            MotionEvent.PointerCoords[] pointerCoordsArr = this.mTempPointerCoords;
            this.mTempPointerCoords = new MotionEvent.PointerCoords[i];
            if (pointerCoordsArr != null) {
                System.arraycopy(pointerCoordsArr, 0, this.mTempPointerCoords, 0, length);
            }
        }
        while (length < i) {
            this.mTempPointerCoords[length] = new MotionEvent.PointerCoords();
            length++;
        }
        return this.mTempPointerCoords;
    }

    private MotionEvent.PointerProperties[] getTempPointerPropertiesWithMinSize(int i) {
        int length = this.mTempPointerProperties != null ? this.mTempPointerProperties.length : 0;
        if (length < i) {
            MotionEvent.PointerProperties[] pointerPropertiesArr = this.mTempPointerProperties;
            this.mTempPointerProperties = new MotionEvent.PointerProperties[i];
            if (pointerPropertiesArr != null) {
                System.arraycopy(pointerPropertiesArr, 0, this.mTempPointerProperties, 0, length);
            }
        }
        while (length < i) {
            this.mTempPointerProperties[length] = new MotionEvent.PointerProperties();
            length++;
        }
        return this.mTempPointerProperties;
    }

    private void transitionTo(State state) {
        this.mPreviousState = this.mCurrentState;
        this.mCurrentState = state;
    }

    interface State {
        void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i);

        default void clear() {
        }

        default String name() {
            return getClass().getSimpleName();
        }

        static String nameOf(State state) {
            return state != null ? state.name() : "null";
        }
    }

    final class PanningScalingState extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener, State {
        float mInitialScaleFactor = -1.0f;
        private final ScaleGestureDetector mScaleGestureDetector;
        boolean mScaling;
        final float mScalingThreshold;
        private final GestureDetector mScrollGestureDetector;

        public PanningScalingState(Context context) {
            TypedValue typedValue = new TypedValue();
            context.getResources().getValue(R.dimen.alert_dialog_button_bar_width, typedValue, false);
            this.mScalingThreshold = typedValue.getFloat();
            this.mScaleGestureDetector = new ScaleGestureDetector(context, this, Handler.getMain());
            this.mScaleGestureDetector.setQuickScaleEnabled(false);
            this.mScrollGestureDetector = new GestureDetector(context, this, Handler.getMain());
        }

        @Override
        public void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 6 && motionEvent.getPointerCount() == 2 && MagnificationGestureHandler.this.mPreviousState == MagnificationGestureHandler.this.mViewportDraggingState) {
                persistScaleAndTransitionTo(MagnificationGestureHandler.this.mViewportDraggingState);
            } else if (actionMasked == 1 || actionMasked == 3) {
                persistScaleAndTransitionTo(MagnificationGestureHandler.this.mDetectingState);
            }
        }

        public void persistScaleAndTransitionTo(State state) {
            MagnificationGestureHandler.this.mMagnificationController.persistScale();
            clear();
            MagnificationGestureHandler.this.transitionTo(state);
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            if (MagnificationGestureHandler.this.mCurrentState != MagnificationGestureHandler.this.mPanningScalingState) {
                return true;
            }
            MagnificationGestureHandler.this.mMagnificationController.offsetMagnifiedRegion(f, f2, 0);
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            if (this.mScaling) {
                float scale = MagnificationGestureHandler.this.mMagnificationController.getScale();
                float scaleFactor = scaleGestureDetector.getScaleFactor() * scale;
                MagnificationGestureHandler.this.mMagnificationController.setScale((scaleFactor <= 5.0f || scaleFactor <= scale) ? (scaleFactor >= MagnificationGestureHandler.MIN_SCALE || scaleFactor >= scale) ? scaleFactor : 2.0f : 5.0f, scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY(), false, 0);
                return true;
            }
            if (this.mInitialScaleFactor < 0.0f) {
                this.mInitialScaleFactor = scaleGestureDetector.getScaleFactor();
                return false;
            }
            this.mScaling = Math.abs(scaleGestureDetector.getScaleFactor() - this.mInitialScaleFactor) > this.mScalingThreshold;
            return this.mScaling;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            return MagnificationGestureHandler.this.mCurrentState == MagnificationGestureHandler.this.mPanningScalingState;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            clear();
        }

        @Override
        public void clear() {
            this.mInitialScaleFactor = -1.0f;
            this.mScaling = false;
        }

        public String toString() {
            return "PanningScalingState{mInitialScaleFactor=" + this.mInitialScaleFactor + ", mScaling=" + this.mScaling + '}';
        }
    }

    final class ViewportDraggingState implements State {
        private boolean mLastMoveOutsideMagnifiedRegion;
        boolean mZoomedInBeforeDrag;

        ViewportDraggingState() {
        }

        @Override
        public void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
            int actionMasked = motionEvent.getActionMasked();
            switch (actionMasked) {
                case 0:
                case 6:
                    throw new IllegalArgumentException("Unexpected event type: " + MotionEvent.actionToString(actionMasked));
                case 1:
                case 3:
                    if (!this.mZoomedInBeforeDrag) {
                        MagnificationGestureHandler.this.zoomOff();
                    }
                    clear();
                    MagnificationGestureHandler.this.transitionTo(MagnificationGestureHandler.this.mDetectingState);
                    return;
                case 2:
                    if (motionEvent.getPointerCount() != 1) {
                        throw new IllegalStateException("Should have one pointer down.");
                    }
                    float x = motionEvent.getX();
                    float y = motionEvent.getY();
                    if (MagnificationGestureHandler.this.mMagnificationController.magnificationRegionContains(x, y)) {
                        MagnificationGestureHandler.this.mMagnificationController.setCenter(x, y, this.mLastMoveOutsideMagnifiedRegion, 0);
                        this.mLastMoveOutsideMagnifiedRegion = false;
                        return;
                    } else {
                        this.mLastMoveOutsideMagnifiedRegion = true;
                        return;
                    }
                case 4:
                default:
                    return;
                case 5:
                    clear();
                    MagnificationGestureHandler.this.transitionTo(MagnificationGestureHandler.this.mPanningScalingState);
                    return;
            }
        }

        @Override
        public void clear() {
            this.mLastMoveOutsideMagnifiedRegion = false;
        }

        public String toString() {
            return "ViewportDraggingState{mZoomedInBeforeDrag=" + this.mZoomedInBeforeDrag + ", mLastMoveOutsideMagnifiedRegion=" + this.mLastMoveOutsideMagnifiedRegion + '}';
        }
    }

    final class DelegatingState implements State {
        public long mLastDelegatedDownEventTime;

        DelegatingState() {
        }

        @Override
        public void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked != 3) {
                switch (actionMasked) {
                    case 0:
                        MagnificationGestureHandler.this.transitionTo(MagnificationGestureHandler.this.mDelegatingState);
                        this.mLastDelegatedDownEventTime = motionEvent.getDownTime();
                        break;
                    case 1:
                        MagnificationGestureHandler.this.transitionTo(MagnificationGestureHandler.this.mDetectingState);
                        break;
                }
            }
            if (MagnificationGestureHandler.this.getNext() != null) {
                motionEvent.setDownTime(this.mLastDelegatedDownEventTime);
                MagnificationGestureHandler.this.dispatchTransformedEvent(motionEvent, motionEvent2, i);
            }
        }
    }

    final class DetectingState implements State, Handler.Callback {
        private static final int MESSAGE_ON_TRIPLE_TAP_AND_HOLD = 1;
        private static final int MESSAGE_TRANSITION_TO_DELEGATING_STATE = 2;
        private MotionEventInfo mDelayedEventQueue;
        MotionEvent mLastDown;
        private MotionEvent mLastUp;
        final int mMultiTapMaxDelay;
        final int mMultiTapMaxDistance;
        private MotionEvent mPreLastDown;
        private MotionEvent mPreLastUp;

        @VisibleForTesting
        boolean mShortcutTriggered;
        final int mSwipeMinDistance;

        @VisibleForTesting
        Handler mHandler = new Handler(Looper.getMainLooper(), this);
        final int mLongTapMinDelay = ViewConfiguration.getLongPressTimeout();

        public DetectingState(Context context) {
            this.mMultiTapMaxDelay = ViewConfiguration.getDoubleTapTimeout() + context.getResources().getInteger(R.integer.config_drawLockTimeoutMillis);
            this.mSwipeMinDistance = ViewConfiguration.get(context).getScaledTouchSlop();
            this.mMultiTapMaxDistance = ViewConfiguration.get(context).getScaledDoubleTapSlop();
        }

        @Override
        public boolean handleMessage(Message message) {
            int i = message.what;
            switch (i) {
                case 1:
                    MotionEvent motionEvent = (MotionEvent) message.obj;
                    transitionToViewportDraggingStateAndClear(motionEvent);
                    motionEvent.recycle();
                    return true;
                case 2:
                    transitionToDelegatingStateAndClear();
                    return true;
                default:
                    throw new IllegalArgumentException("Unknown message type: " + i);
            }
        }

        @Override
        public void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
            cacheDelayedMotionEvent(motionEvent, motionEvent2, i);
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked != 5) {
                switch (actionMasked) {
                    case 0:
                        this.mHandler.removeMessages(2);
                        if (!MagnificationGestureHandler.this.mMagnificationController.magnificationRegionContains(motionEvent.getX(), motionEvent.getY())) {
                            transitionToDelegatingStateAndClear();
                        } else if (isMultiTapTriggered(2)) {
                            afterLongTapTimeoutTransitionToDraggingState(motionEvent);
                        } else if (MagnificationGestureHandler.this.mDetectTripleTap || MagnificationGestureHandler.this.mMagnificationController.isMagnifying()) {
                            afterMultiTapTimeoutTransitionToDelegatingState();
                        } else {
                            transitionToDelegatingStateAndClear();
                        }
                        break;
                    case 1:
                        this.mHandler.removeMessages(1);
                        if (!MagnificationGestureHandler.this.mMagnificationController.magnificationRegionContains(motionEvent.getX(), motionEvent.getY())) {
                            transitionToDelegatingStateAndClear();
                        } else if (isMultiTapTriggered(3)) {
                            onTripleTap(motionEvent);
                        } else if (isFingerDown()) {
                            if (timeBetween(this.mLastDown, this.mLastUp) >= this.mLongTapMinDelay || GestureUtils.distance(this.mLastDown, this.mLastUp) >= this.mSwipeMinDistance) {
                                transitionToDelegatingStateAndClear();
                            }
                        }
                        break;
                    case 2:
                        if (isFingerDown() && GestureUtils.distance(this.mLastDown, motionEvent) > this.mSwipeMinDistance) {
                            if (isMultiTapTriggered(2)) {
                                transitionToViewportDraggingStateAndClear(motionEvent);
                            } else {
                                transitionToDelegatingStateAndClear();
                            }
                            break;
                        }
                        break;
                }
            }
            if (MagnificationGestureHandler.this.mMagnificationController.isMagnifying()) {
                MagnificationGestureHandler.this.transitionTo(MagnificationGestureHandler.this.mPanningScalingState);
                clear();
            } else {
                transitionToDelegatingStateAndClear();
            }
        }

        public boolean isMultiTapTriggered(int i) {
            return this.mShortcutTriggered ? tapCount() + 2 >= i : MagnificationGestureHandler.this.mDetectTripleTap && tapCount() >= i && isMultiTap(this.mPreLastDown, this.mLastDown) && isMultiTap(this.mPreLastUp, this.mLastUp);
        }

        private boolean isMultiTap(MotionEvent motionEvent, MotionEvent motionEvent2) {
            return GestureUtils.isMultiTap(motionEvent, motionEvent2, this.mMultiTapMaxDelay, this.mMultiTapMaxDistance);
        }

        public boolean isFingerDown() {
            return this.mLastDown != null;
        }

        private long timeBetween(MotionEvent motionEvent, MotionEvent motionEvent2) {
            if (motionEvent == null && motionEvent2 == null) {
                return 0L;
            }
            return Math.abs(timeOf(motionEvent) - timeOf(motionEvent2));
        }

        private long timeOf(MotionEvent motionEvent) {
            if (motionEvent != null) {
                return motionEvent.getEventTime();
            }
            return Long.MIN_VALUE;
        }

        public int tapCount() {
            return MotionEventInfo.countOf(this.mDelayedEventQueue, 1);
        }

        public void afterMultiTapTimeoutTransitionToDelegatingState() {
            this.mHandler.sendEmptyMessageDelayed(2, this.mMultiTapMaxDelay);
        }

        public void afterLongTapTimeoutTransitionToDraggingState(MotionEvent motionEvent) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, MotionEvent.obtain(motionEvent)), ViewConfiguration.getLongPressTimeout());
        }

        @Override
        public void clear() {
            setShortcutTriggered(false);
            removePendingDelayedMessages();
            clearDelayedMotionEvents();
        }

        private void removePendingDelayedMessages() {
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(2);
        }

        private void cacheDelayedMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
            if (motionEvent.getActionMasked() == 0) {
                this.mPreLastDown = this.mLastDown;
                this.mLastDown = MotionEvent.obtain(motionEvent);
            } else if (motionEvent.getActionMasked() == 1) {
                this.mPreLastUp = this.mLastUp;
                this.mLastUp = MotionEvent.obtain(motionEvent);
            }
            MotionEventInfo motionEventInfoObtain = MotionEventInfo.obtain(motionEvent, motionEvent2, i);
            if (this.mDelayedEventQueue == null) {
                this.mDelayedEventQueue = motionEventInfoObtain;
                return;
            }
            MotionEventInfo motionEventInfo = this.mDelayedEventQueue;
            while (motionEventInfo.mNext != null) {
                motionEventInfo = motionEventInfo.mNext;
            }
            motionEventInfo.mNext = motionEventInfoObtain;
        }

        private void sendDelayedMotionEvents() {
            while (this.mDelayedEventQueue != null) {
                MotionEventInfo motionEventInfo = this.mDelayedEventQueue;
                this.mDelayedEventQueue = motionEventInfo.mNext;
                MagnificationGestureHandler.this.handleEventWith(MagnificationGestureHandler.this.mDelegatingState, motionEventInfo.event, motionEventInfo.rawEvent, motionEventInfo.policyFlags);
                motionEventInfo.recycle();
            }
        }

        private void clearDelayedMotionEvents() {
            while (this.mDelayedEventQueue != null) {
                MotionEventInfo motionEventInfo = this.mDelayedEventQueue;
                this.mDelayedEventQueue = motionEventInfo.mNext;
                motionEventInfo.recycle();
            }
            this.mPreLastDown = null;
            this.mPreLastUp = null;
            this.mLastDown = null;
            this.mLastUp = null;
        }

        void transitionToDelegatingStateAndClear() {
            MagnificationGestureHandler.this.transitionTo(MagnificationGestureHandler.this.mDelegatingState);
            sendDelayedMotionEvents();
            removePendingDelayedMessages();
        }

        private void onTripleTap(MotionEvent motionEvent) {
            clear();
            if (MagnificationGestureHandler.this.mMagnificationController.isMagnifying()) {
                MagnificationGestureHandler.this.zoomOff();
            } else {
                MagnificationGestureHandler.this.zoomOn(motionEvent.getX(), motionEvent.getY());
            }
        }

        void transitionToViewportDraggingStateAndClear(MotionEvent motionEvent) {
            clear();
            MagnificationGestureHandler.this.mViewportDraggingState.mZoomedInBeforeDrag = MagnificationGestureHandler.this.mMagnificationController.isMagnifying();
            MagnificationGestureHandler.this.zoomOn(motionEvent.getX(), motionEvent.getY());
            MagnificationGestureHandler.this.transitionTo(MagnificationGestureHandler.this.mViewportDraggingState);
        }

        public String toString() {
            return "DetectingState{tapCount()=" + tapCount() + ", mShortcutTriggered=" + this.mShortcutTriggered + ", mDelayedEventQueue=" + MotionEventInfo.toString(this.mDelayedEventQueue) + '}';
        }

        void toggleShortcutTriggered() {
            setShortcutTriggered(!this.mShortcutTriggered);
        }

        void setShortcutTriggered(boolean z) {
            if (this.mShortcutTriggered == z) {
                return;
            }
            this.mShortcutTriggered = z;
            MagnificationGestureHandler.this.mMagnificationController.setForceShowMagnifiableBounds(z);
        }
    }

    private void zoomOn(float f, float f2) {
        this.mMagnificationController.setScaleAndCenter(MathUtils.constrain(this.mMagnificationController.getPersistedScale(), MIN_SCALE, 5.0f), f, f2, true, 0);
    }

    private void zoomOff() {
        this.mMagnificationController.reset(true);
    }

    private static MotionEvent recycleAndNullify(MotionEvent motionEvent) {
        if (motionEvent != null) {
            motionEvent.recycle();
            return null;
        }
        return null;
    }

    public String toString() {
        return "MagnificationGesture{mDetectingState=" + this.mDetectingState + ", mDelegatingState=" + this.mDelegatingState + ", mMagnifiedInteractionState=" + this.mPanningScalingState + ", mViewportDraggingState=" + this.mViewportDraggingState + ", mDetectTripleTap=" + this.mDetectTripleTap + ", mDetectShortcutTrigger=" + this.mDetectShortcutTrigger + ", mCurrentState=" + State.nameOf(this.mCurrentState) + ", mPreviousState=" + State.nameOf(this.mPreviousState) + ", mMagnificationController=" + this.mMagnificationController + '}';
    }

    private static final class MotionEventInfo {
        private static final int MAX_POOL_SIZE = 10;
        private static final Object sLock = new Object();
        private static MotionEventInfo sPool;
        private static int sPoolSize;
        public MotionEvent event;
        private boolean mInPool;
        private MotionEventInfo mNext;
        public int policyFlags;
        public MotionEvent rawEvent;

        private MotionEventInfo() {
        }

        public static MotionEventInfo obtain(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
            MotionEventInfo motionEventInfoObtainInternal;
            synchronized (sLock) {
                motionEventInfoObtainInternal = obtainInternal();
                motionEventInfoObtainInternal.initialize(motionEvent, motionEvent2, i);
            }
            return motionEventInfoObtainInternal;
        }

        private static MotionEventInfo obtainInternal() {
            if (sPoolSize > 0) {
                sPoolSize--;
                MotionEventInfo motionEventInfo = sPool;
                sPool = motionEventInfo.mNext;
                motionEventInfo.mNext = null;
                motionEventInfo.mInPool = false;
                return motionEventInfo;
            }
            return new MotionEventInfo();
        }

        private void initialize(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
            this.event = MotionEvent.obtain(motionEvent);
            this.rawEvent = MotionEvent.obtain(motionEvent2);
            this.policyFlags = i;
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
            this.event = MagnificationGestureHandler.recycleAndNullify(this.event);
            this.rawEvent = MagnificationGestureHandler.recycleAndNullify(this.rawEvent);
            this.policyFlags = 0;
        }

        static int countOf(MotionEventInfo motionEventInfo, int i) {
            if (motionEventInfo == null) {
                return 0;
            }
            return (motionEventInfo.event.getAction() == i ? 1 : 0) + countOf(motionEventInfo.mNext, i);
        }

        public static String toString(MotionEventInfo motionEventInfo) {
            if (motionEventInfo == null) {
                return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            }
            return MotionEvent.actionToString(motionEventInfo.event.getAction()).replace("ACTION_", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS) + " " + toString(motionEventInfo.mNext);
        }
    }

    private static class ScreenStateReceiver extends BroadcastReceiver {
        private final Context mContext;
        private final MagnificationGestureHandler mGestureHandler;

        public ScreenStateReceiver(Context context, MagnificationGestureHandler magnificationGestureHandler) {
            this.mContext = context;
            this.mGestureHandler = magnificationGestureHandler;
        }

        public void register() {
            this.mContext.registerReceiver(this, new IntentFilter("android.intent.action.SCREEN_OFF"));
        }

        public void unregister() {
            this.mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.mGestureHandler.mDetectingState.setShortcutTriggered(false);
        }
    }
}
