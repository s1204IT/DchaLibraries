package com.android.commands.monkey;

import android.app.IActivityManager;
import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.IWindowManager;
import android.view.MotionEvent;

public abstract class MonkeyMotionEvent extends MonkeyEvent {
    private int mAction;
    private int mDeviceId;
    private long mDownTime;
    private int mEdgeFlags;
    private long mEventTime;
    private int mFlags;
    private boolean mIntermediateNote;
    private int mMetaState;
    private SparseArray<MotionEvent.PointerCoords> mPointers;
    private int mSource;
    private float mXPrecision;
    private float mYPrecision;

    protected abstract String getTypeLabel();

    protected MonkeyMotionEvent(int i, int i2, int i3) {
        super(i);
        this.mSource = i2;
        this.mDownTime = -1L;
        this.mEventTime = -1L;
        this.mAction = i3;
        this.mPointers = new SparseArray<>();
        this.mXPrecision = 1.0f;
        this.mYPrecision = 1.0f;
    }

    public MonkeyMotionEvent addPointer(int i, float f, float f2) {
        return addPointer(i, f, f2, 0.0f, 0.0f);
    }

    public MonkeyMotionEvent addPointer(int i, float f, float f2, float f3, float f4) {
        MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
        pointerCoords.x = f;
        pointerCoords.y = f2;
        pointerCoords.pressure = f3;
        pointerCoords.size = f4;
        this.mPointers.append(i, pointerCoords);
        return this;
    }

    public MonkeyMotionEvent setIntermediateNote(boolean z) {
        this.mIntermediateNote = z;
        return this;
    }

    public boolean getIntermediateNote() {
        return this.mIntermediateNote;
    }

    public int getAction() {
        return this.mAction;
    }

    public long getDownTime() {
        return this.mDownTime;
    }

    public long getEventTime() {
        return this.mEventTime;
    }

    public MonkeyMotionEvent setDownTime(long j) {
        this.mDownTime = j;
        return this;
    }

    public MonkeyMotionEvent setEventTime(long j) {
        this.mEventTime = j;
        return this;
    }

    public MonkeyMotionEvent setMetaState(int i) {
        this.mMetaState = i;
        return this;
    }

    public MonkeyMotionEvent setPrecision(float f, float f2) {
        this.mXPrecision = f;
        this.mYPrecision = f2;
        return this;
    }

    public MonkeyMotionEvent setDeviceId(int i) {
        this.mDeviceId = i;
        return this;
    }

    public MonkeyMotionEvent setEdgeFlags(int i) {
        this.mEdgeFlags = i;
        return this;
    }

    private MotionEvent getEvent() {
        int size = this.mPointers.size();
        int[] iArr = new int[size];
        MotionEvent.PointerCoords[] pointerCoordsArr = new MotionEvent.PointerCoords[size];
        for (int i = 0; i < size; i++) {
            iArr[i] = this.mPointers.keyAt(i);
            pointerCoordsArr[i] = this.mPointers.valueAt(i);
        }
        return MotionEvent.obtain(this.mDownTime, this.mEventTime < 0 ? SystemClock.uptimeMillis() : this.mEventTime, this.mAction, size, iArr, pointerCoordsArr, this.mMetaState, this.mXPrecision, this.mYPrecision, this.mDeviceId, this.mEdgeFlags, this.mSource, this.mFlags);
    }

    @Override
    public boolean isThrottlable() {
        return getAction() == 1;
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) {
        MotionEvent event = getEvent();
        if ((i > 0 && !this.mIntermediateNote) || i > 1) {
            StringBuilder sb = new StringBuilder(":Sending ");
            sb.append(getTypeLabel());
            sb.append(" (");
            switch (event.getActionMasked()) {
                case 0:
                    sb.append("ACTION_DOWN");
                    break;
                case 1:
                    sb.append("ACTION_UP");
                    break;
                case 2:
                    sb.append("ACTION_MOVE");
                    break;
                case 3:
                    sb.append("ACTION_CANCEL");
                    break;
                case 4:
                default:
                    sb.append(event.getAction());
                    break;
                case 5:
                    sb.append("ACTION_POINTER_DOWN ");
                    sb.append(event.getPointerId(event.getActionIndex()));
                    break;
                case 6:
                    sb.append("ACTION_POINTER_UP ");
                    sb.append(event.getPointerId(event.getActionIndex()));
                    break;
            }
            sb.append("):");
            int pointerCount = event.getPointerCount();
            for (int i2 = 0; i2 < pointerCount; i2++) {
                sb.append(" ");
                sb.append(event.getPointerId(i2));
                sb.append(":(");
                sb.append(event.getX(i2));
                sb.append(",");
                sb.append(event.getY(i2));
                sb.append(")");
            }
            Logger.out.println(sb.toString());
        }
        try {
            return !InputManager.getInstance().injectInputEvent(event, 1) ? 0 : 1;
        } finally {
            event.recycle();
        }
    }
}
