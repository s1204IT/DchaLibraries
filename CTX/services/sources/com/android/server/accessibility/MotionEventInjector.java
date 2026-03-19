package com.android.server.accessibility;

import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import com.android.internal.os.SomeArgs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MotionEventInjector extends BaseEventStreamTransformation implements Handler.Callback {
    private static final int EVENT_BUTTON_STATE = 0;
    private static final int EVENT_DEVICE_ID = 0;
    private static final int EVENT_EDGE_FLAGS = 0;
    private static final int EVENT_FLAGS = 0;
    private static final int EVENT_META_STATE = 0;
    private static final int EVENT_SOURCE = 4098;
    private static final float EVENT_X_PRECISION = 1.0f;
    private static final float EVENT_Y_PRECISION = 1.0f;
    private static final String LOG_TAG = "MotionEventInjector";
    private static final int MESSAGE_INJECT_EVENTS = 2;
    private static final int MESSAGE_SEND_MOTION_EVENT = 1;
    private static MotionEvent.PointerCoords[] sPointerCoords;
    private static MotionEvent.PointerProperties[] sPointerProps;
    private long mDownTime;
    private final Handler mHandler;
    private long mLastScheduledEventTime;
    private GestureDescription.TouchPoint[] mLastTouchPoints;
    private int mNumLastTouchPoints;
    private IAccessibilityServiceClient mServiceInterfaceForCurrentGesture;
    private final SparseArray<Boolean> mOpenGesturesInProgress = new SparseArray<>();
    private IntArray mSequencesInProgress = new IntArray(5);
    private boolean mIsDestroyed = false;
    private SparseIntArray mStrokeIdToPointerId = new SparseIntArray(5);

    @Override
    public EventStreamTransformation getNext() {
        return super.getNext();
    }

    @Override
    public void setNext(EventStreamTransformation eventStreamTransformation) {
        super.setNext(eventStreamTransformation);
    }

    public MotionEventInjector(Looper looper) {
        this.mHandler = new Handler(looper, this);
    }

    public MotionEventInjector(Handler handler) {
        this.mHandler = handler;
    }

    public void injectEvents(List<GestureDescription.GestureStep> list, IAccessibilityServiceClient iAccessibilityServiceClient, int i) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = list;
        someArgsObtain.arg2 = iAccessibilityServiceClient;
        someArgsObtain.argi1 = i;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, someArgsObtain));
    }

    @Override
    public void onMotionEvent(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
        cancelAnyPendingInjectedEvents();
        sendMotionEventToNext(motionEvent, motionEvent2, i);
    }

    @Override
    public void clearEvents(int i) {
        if (!this.mHandler.hasMessages(1)) {
            this.mOpenGesturesInProgress.put(i, false);
        }
    }

    @Override
    public void onDestroy() {
        cancelAnyPendingInjectedEvents();
        this.mIsDestroyed = true;
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message.what == 2) {
            SomeArgs someArgs = (SomeArgs) message.obj;
            injectEventsMainThread((List) someArgs.arg1, (IAccessibilityServiceClient) someArgs.arg2, someArgs.argi1);
            someArgs.recycle();
            return true;
        }
        if (message.what != 1) {
            Slog.e(LOG_TAG, "Unknown message: " + message.what);
            return false;
        }
        MotionEvent motionEvent = (MotionEvent) message.obj;
        sendMotionEventToNext(motionEvent, motionEvent, 1073741824);
        if (message.arg1 != 0) {
            notifyService(this.mServiceInterfaceForCurrentGesture, this.mSequencesInProgress.get(0), true);
            this.mSequencesInProgress.remove(0);
        }
        return true;
    }

    private void injectEventsMainThread(List<GestureDescription.GestureStep> list, IAccessibilityServiceClient iAccessibilityServiceClient, int i) {
        long j;
        if (this.mIsDestroyed) {
            try {
                iAccessibilityServiceClient.onPerformGestureResult(i, false);
                return;
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error sending status with mIsDestroyed to " + iAccessibilityServiceClient, e);
                return;
            }
        }
        if (getNext() == null) {
            notifyService(iAccessibilityServiceClient, i, false);
            return;
        }
        boolean zNewGestureTriesToContinueOldOne = newGestureTriesToContinueOldOne(list);
        if (zNewGestureTriesToContinueOldOne && (iAccessibilityServiceClient != this.mServiceInterfaceForCurrentGesture || !prepareToContinueOldGesture(list))) {
            cancelAnyPendingInjectedEvents();
            notifyService(iAccessibilityServiceClient, i, false);
            return;
        }
        if (!zNewGestureTriesToContinueOldOne) {
            cancelAnyPendingInjectedEvents();
            cancelAnyGestureInProgress(4098);
        }
        this.mServiceInterfaceForCurrentGesture = iAccessibilityServiceClient;
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (this.mSequencesInProgress.size() == 0) {
            j = jUptimeMillis;
        } else {
            j = this.mLastScheduledEventTime;
        }
        List<MotionEvent> motionEventsFromGestureSteps = getMotionEventsFromGestureSteps(list, j);
        if (motionEventsFromGestureSteps.isEmpty()) {
            notifyService(iAccessibilityServiceClient, i, false);
            return;
        }
        this.mSequencesInProgress.add(i);
        int i2 = 0;
        while (i2 < motionEventsFromGestureSteps.size()) {
            MotionEvent motionEvent = motionEventsFromGestureSteps.get(i2);
            Message messageObtainMessage = this.mHandler.obtainMessage(1, i2 == motionEventsFromGestureSteps.size() - 1 ? 1 : 0, 0, motionEvent);
            this.mLastScheduledEventTime = motionEvent.getEventTime();
            this.mHandler.sendMessageDelayed(messageObtainMessage, Math.max(0L, motionEvent.getEventTime() - jUptimeMillis));
            i2++;
        }
    }

    private boolean newGestureTriesToContinueOldOne(List<GestureDescription.GestureStep> list) {
        if (list.isEmpty()) {
            return false;
        }
        GestureDescription.GestureStep gestureStep = list.get(0);
        for (int i = 0; i < gestureStep.numTouchPoints; i++) {
            if (!gestureStep.touchPoints[i].mIsStartOfPath) {
                return true;
            }
        }
        return false;
    }

    private boolean prepareToContinueOldGesture(List<GestureDescription.GestureStep> list) {
        if (list.isEmpty() || this.mLastTouchPoints == null || this.mNumLastTouchPoints == 0) {
            return false;
        }
        GestureDescription.GestureStep gestureStep = list.get(0);
        int i = 0;
        for (int i2 = 0; i2 < gestureStep.numTouchPoints; i2++) {
            GestureDescription.TouchPoint touchPoint = gestureStep.touchPoints[i2];
            if (!touchPoint.mIsStartOfPath) {
                int i3 = this.mStrokeIdToPointerId.get(touchPoint.mContinuedStrokeId, -1);
                if (i3 == -1) {
                    Slog.w(LOG_TAG, "Can't continue gesture due to unknown continued stroke id in " + touchPoint);
                    return false;
                }
                this.mStrokeIdToPointerId.put(touchPoint.mStrokeId, i3);
                int iFindPointByStrokeId = findPointByStrokeId(this.mLastTouchPoints, this.mNumLastTouchPoints, touchPoint.mContinuedStrokeId);
                if (iFindPointByStrokeId < 0) {
                    Slog.w(LOG_TAG, "Can't continue gesture due continued gesture id of " + touchPoint + " not matching any previous strokes in " + Arrays.asList(this.mLastTouchPoints));
                    return false;
                }
                if (this.mLastTouchPoints[iFindPointByStrokeId].mIsEndOfPath || this.mLastTouchPoints[iFindPointByStrokeId].mX != touchPoint.mX || this.mLastTouchPoints[iFindPointByStrokeId].mY != touchPoint.mY) {
                    Slog.w(LOG_TAG, "Can't continue gesture due to points mismatch between " + this.mLastTouchPoints[iFindPointByStrokeId] + " and " + touchPoint);
                    return false;
                }
                this.mLastTouchPoints[iFindPointByStrokeId].mStrokeId = touchPoint.mStrokeId;
            }
            i++;
        }
        for (int i4 = 0; i4 < this.mNumLastTouchPoints; i4++) {
            if (!this.mLastTouchPoints[i4].mIsEndOfPath) {
                i--;
            }
        }
        return i == 0;
    }

    private void sendMotionEventToNext(MotionEvent motionEvent, MotionEvent motionEvent2, int i) {
        if (getNext() != null) {
            super.onMotionEvent(motionEvent, motionEvent2, i);
            if (motionEvent.getActionMasked() == 0) {
                this.mOpenGesturesInProgress.put(motionEvent.getSource(), true);
            }
            if (motionEvent.getActionMasked() == 1 || motionEvent.getActionMasked() == 3) {
                this.mOpenGesturesInProgress.put(motionEvent.getSource(), false);
            }
        }
    }

    private void cancelAnyGestureInProgress(int i) {
        if (getNext() != null && this.mOpenGesturesInProgress.get(i, false).booleanValue()) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            MotionEvent motionEventObtainMotionEvent = obtainMotionEvent(jUptimeMillis, jUptimeMillis, 3, getLastTouchPoints(), 1);
            sendMotionEventToNext(motionEventObtainMotionEvent, motionEventObtainMotionEvent, 1073741824);
            this.mOpenGesturesInProgress.put(i, false);
        }
    }

    private void cancelAnyPendingInjectedEvents() {
        if (this.mHandler.hasMessages(1)) {
            this.mHandler.removeMessages(1);
            cancelAnyGestureInProgress(4098);
            for (int size = this.mSequencesInProgress.size() - 1; size >= 0; size--) {
                notifyService(this.mServiceInterfaceForCurrentGesture, this.mSequencesInProgress.get(size), false);
                this.mSequencesInProgress.remove(size);
            }
        } else if (this.mNumLastTouchPoints != 0) {
            cancelAnyGestureInProgress(4098);
        }
        this.mNumLastTouchPoints = 0;
        this.mStrokeIdToPointerId.clear();
    }

    private void notifyService(IAccessibilityServiceClient iAccessibilityServiceClient, int i, boolean z) {
        try {
            iAccessibilityServiceClient.onPerformGestureResult(i, z);
        } catch (RemoteException e) {
            Slog.e(LOG_TAG, "Error sending motion event injection status to " + this.mServiceInterfaceForCurrentGesture, e);
        }
    }

    private List<MotionEvent> getMotionEventsFromGestureSteps(List<GestureDescription.GestureStep> list, long j) {
        ArrayList arrayList = new ArrayList();
        GestureDescription.TouchPoint[] lastTouchPoints = getLastTouchPoints();
        for (int i = 0; i < list.size(); i++) {
            GestureDescription.GestureStep gestureStep = list.get(i);
            int i2 = gestureStep.numTouchPoints;
            if (i2 > lastTouchPoints.length) {
                this.mNumLastTouchPoints = 0;
                arrayList.clear();
                return arrayList;
            }
            appendMoveEventIfNeeded(arrayList, gestureStep.touchPoints, i2, j + gestureStep.timeSinceGestureStart);
            appendUpEvents(arrayList, gestureStep.touchPoints, i2, j + gestureStep.timeSinceGestureStart);
            appendDownEvents(arrayList, gestureStep.touchPoints, i2, j + gestureStep.timeSinceGestureStart);
        }
        return arrayList;
    }

    private GestureDescription.TouchPoint[] getLastTouchPoints() {
        if (this.mLastTouchPoints == null) {
            int maxStrokeCount = GestureDescription.getMaxStrokeCount();
            this.mLastTouchPoints = new GestureDescription.TouchPoint[maxStrokeCount];
            for (int i = 0; i < maxStrokeCount; i++) {
                this.mLastTouchPoints[i] = new GestureDescription.TouchPoint();
            }
        }
        return this.mLastTouchPoints;
    }

    private void appendMoveEventIfNeeded(List<MotionEvent> list, GestureDescription.TouchPoint[] touchPointArr, int i, long j) {
        GestureDescription.TouchPoint[] lastTouchPoints = getLastTouchPoints();
        boolean z = false;
        for (int i2 = 0; i2 < i; i2++) {
            int iFindPointByStrokeId = findPointByStrokeId(lastTouchPoints, this.mNumLastTouchPoints, touchPointArr[i2].mStrokeId);
            if (iFindPointByStrokeId >= 0) {
                z |= (lastTouchPoints[iFindPointByStrokeId].mX == touchPointArr[i2].mX && lastTouchPoints[iFindPointByStrokeId].mY == touchPointArr[i2].mY) ? false : true;
                lastTouchPoints[iFindPointByStrokeId].copyFrom(touchPointArr[i2]);
            }
        }
        if (z) {
            list.add(obtainMotionEvent(this.mDownTime, j, 2, lastTouchPoints, this.mNumLastTouchPoints));
        }
    }

    private void appendUpEvents(List<MotionEvent> list, GestureDescription.TouchPoint[] touchPointArr, int i, long j) {
        int iFindPointByStrokeId;
        int i2;
        GestureDescription.TouchPoint[] lastTouchPoints = getLastTouchPoints();
        for (int i3 = 0; i3 < i; i3++) {
            if (touchPointArr[i3].mIsEndOfPath && (iFindPointByStrokeId = findPointByStrokeId(lastTouchPoints, this.mNumLastTouchPoints, touchPointArr[i3].mStrokeId)) >= 0) {
                if (this.mNumLastTouchPoints != 1) {
                    i2 = 6;
                } else {
                    i2 = 1;
                }
                list.add(obtainMotionEvent(this.mDownTime, j, i2 | (iFindPointByStrokeId << 8), lastTouchPoints, this.mNumLastTouchPoints));
                while (iFindPointByStrokeId < this.mNumLastTouchPoints - 1) {
                    GestureDescription.TouchPoint touchPoint = lastTouchPoints[iFindPointByStrokeId];
                    iFindPointByStrokeId++;
                    touchPoint.copyFrom(this.mLastTouchPoints[iFindPointByStrokeId]);
                }
                this.mNumLastTouchPoints--;
                if (this.mNumLastTouchPoints == 0) {
                    this.mStrokeIdToPointerId.clear();
                }
            }
        }
    }

    private void appendDownEvents(List<MotionEvent> list, GestureDescription.TouchPoint[] touchPointArr, int i, long j) {
        int i2;
        long j2;
        GestureDescription.TouchPoint[] lastTouchPoints = getLastTouchPoints();
        for (int i3 = 0; i3 < i; i3++) {
            if (touchPointArr[i3].mIsStartOfPath) {
                int i4 = this.mNumLastTouchPoints;
                this.mNumLastTouchPoints = i4 + 1;
                lastTouchPoints[i4].copyFrom(touchPointArr[i3]);
                if (this.mNumLastTouchPoints != 1) {
                    i2 = 5;
                } else {
                    i2 = 0;
                }
                if (i2 == 0) {
                    j2 = j;
                    this.mDownTime = j2;
                } else {
                    j2 = j;
                }
                list.add(obtainMotionEvent(this.mDownTime, j2, i2 | (i3 << 8), lastTouchPoints, this.mNumLastTouchPoints));
            }
        }
    }

    private MotionEvent obtainMotionEvent(long j, long j2, int i, GestureDescription.TouchPoint[] touchPointArr, int i2) {
        if (sPointerCoords == null || sPointerCoords.length < i2) {
            sPointerCoords = new MotionEvent.PointerCoords[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                sPointerCoords[i3] = new MotionEvent.PointerCoords();
            }
        }
        if (sPointerProps == null || sPointerProps.length < i2) {
            sPointerProps = new MotionEvent.PointerProperties[i2];
            for (int i4 = 0; i4 < i2; i4++) {
                sPointerProps[i4] = new MotionEvent.PointerProperties();
            }
        }
        for (int i5 = 0; i5 < i2; i5++) {
            int unusedPointerId = this.mStrokeIdToPointerId.get(touchPointArr[i5].mStrokeId, -1);
            if (unusedPointerId == -1) {
                unusedPointerId = getUnusedPointerId();
                this.mStrokeIdToPointerId.put(touchPointArr[i5].mStrokeId, unusedPointerId);
            }
            sPointerProps[i5].id = unusedPointerId;
            sPointerProps[i5].toolType = 0;
            sPointerCoords[i5].clear();
            sPointerCoords[i5].pressure = 1.0f;
            sPointerCoords[i5].size = 1.0f;
            sPointerCoords[i5].x = touchPointArr[i5].mX;
            sPointerCoords[i5].y = touchPointArr[i5].mY;
        }
        return MotionEvent.obtain(j, j2, i, i2, sPointerProps, sPointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, 4098, 0);
    }

    private static int findPointByStrokeId(GestureDescription.TouchPoint[] touchPointArr, int i, int i2) {
        for (int i3 = 0; i3 < i; i3++) {
            if (touchPointArr[i3].mStrokeId == i2) {
                return i3;
            }
        }
        return -1;
    }

    private int getUnusedPointerId() {
        int i = 0;
        while (this.mStrokeIdToPointerId.indexOfValue(i) >= 0) {
            i++;
            if (i >= 10) {
                return 10;
            }
        }
        return i;
    }
}
