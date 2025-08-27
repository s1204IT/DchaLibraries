package com.android.systemui.analytics;

import android.hardware.SensorEvent;
import android.os.Build;
import android.view.MotionEvent;
import com.android.systemui.statusbar.phone.nano.TouchAnalyticsProto;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class SensorLoggerSession {
    private long mEndTimestampMillis;
    private final long mStartSystemTimeNanos;
    private final long mStartTimestampMillis;
    private int mTouchAreaHeight;
    private int mTouchAreaWidth;
    private ArrayList<TouchAnalyticsProto.Session.TouchEvent> mMotionEvents = new ArrayList<>();
    private ArrayList<TouchAnalyticsProto.Session.SensorEvent> mSensorEvents = new ArrayList<>();
    private ArrayList<TouchAnalyticsProto.Session.PhoneEvent> mPhoneEvents = new ArrayList<>();
    private int mResult = 2;
    private int mType = 3;

    public SensorLoggerSession(long j, long j2) {
        this.mStartTimestampMillis = j;
        this.mStartSystemTimeNanos = j2;
    }

    public void setType(int i) {
        this.mType = i;
    }

    public void end(long j, int i) {
        this.mResult = i;
        this.mEndTimestampMillis = j;
    }

    public void addMotionEvent(MotionEvent motionEvent) {
        this.mMotionEvents.add(motionEventToProto(motionEvent));
    }

    public void addSensorEvent(SensorEvent sensorEvent, long j) {
        this.mSensorEvents.add(sensorEventToProto(sensorEvent, j));
    }

    public void addPhoneEvent(int i, long j) {
        this.mPhoneEvents.add(phoneEventToProto(i, j));
    }

    public String toString() {
        return "Session{mStartTimestampMillis=" + this.mStartTimestampMillis + ", mStartSystemTimeNanos=" + this.mStartSystemTimeNanos + ", mEndTimestampMillis=" + this.mEndTimestampMillis + ", mResult=" + this.mResult + ", mTouchAreaHeight=" + this.mTouchAreaHeight + ", mTouchAreaWidth=" + this.mTouchAreaWidth + ", mMotionEvents=[size=" + this.mMotionEvents.size() + "], mSensorEvents=[size=" + this.mSensorEvents.size() + "], mPhoneEvents=[size=" + this.mPhoneEvents.size() + "]}";
    }

    public TouchAnalyticsProto.Session toProto() {
        TouchAnalyticsProto.Session session = new TouchAnalyticsProto.Session();
        session.setStartTimestampMillis(this.mStartTimestampMillis);
        session.setDurationMillis(this.mEndTimestampMillis - this.mStartTimestampMillis);
        session.setBuild(Build.FINGERPRINT);
        session.setResult(this.mResult);
        session.setType(this.mType);
        session.sensorEvents = (TouchAnalyticsProto.Session.SensorEvent[]) this.mSensorEvents.toArray(session.sensorEvents);
        session.touchEvents = (TouchAnalyticsProto.Session.TouchEvent[]) this.mMotionEvents.toArray(session.touchEvents);
        session.phoneEvents = (TouchAnalyticsProto.Session.PhoneEvent[]) this.mPhoneEvents.toArray(session.phoneEvents);
        session.setTouchAreaWidth(this.mTouchAreaWidth);
        session.setTouchAreaHeight(this.mTouchAreaHeight);
        return session;
    }

    private TouchAnalyticsProto.Session.PhoneEvent phoneEventToProto(int i, long j) {
        TouchAnalyticsProto.Session.PhoneEvent phoneEvent = new TouchAnalyticsProto.Session.PhoneEvent();
        phoneEvent.setType(i);
        phoneEvent.setTimeOffsetNanos(j - this.mStartSystemTimeNanos);
        return phoneEvent;
    }

    private TouchAnalyticsProto.Session.SensorEvent sensorEventToProto(SensorEvent sensorEvent, long j) {
        TouchAnalyticsProto.Session.SensorEvent sensorEvent2 = new TouchAnalyticsProto.Session.SensorEvent();
        sensorEvent2.setType(sensorEvent.sensor.getType());
        sensorEvent2.setTimeOffsetNanos(j - this.mStartSystemTimeNanos);
        sensorEvent2.setTimestamp(sensorEvent.timestamp);
        sensorEvent2.values = (float[]) sensorEvent.values.clone();
        return sensorEvent2;
    }

    private TouchAnalyticsProto.Session.TouchEvent motionEventToProto(MotionEvent motionEvent) {
        int pointerCount = motionEvent.getPointerCount();
        TouchAnalyticsProto.Session.TouchEvent touchEvent = new TouchAnalyticsProto.Session.TouchEvent();
        touchEvent.setTimeOffsetNanos(motionEvent.getEventTimeNano() - this.mStartSystemTimeNanos);
        touchEvent.setAction(motionEvent.getActionMasked());
        touchEvent.setActionIndex(motionEvent.getActionIndex());
        touchEvent.pointers = new TouchAnalyticsProto.Session.TouchEvent.Pointer[pointerCount];
        for (int i = 0; i < pointerCount; i++) {
            TouchAnalyticsProto.Session.TouchEvent.Pointer pointer = new TouchAnalyticsProto.Session.TouchEvent.Pointer();
            pointer.setX(motionEvent.getX(i));
            pointer.setY(motionEvent.getY(i));
            pointer.setSize(motionEvent.getSize(i));
            pointer.setPressure(motionEvent.getPressure(i));
            pointer.setId(motionEvent.getPointerId(i));
            touchEvent.pointers[i] = pointer;
        }
        return touchEvent;
    }

    public void setTouchArea(int i, int i2) {
        this.mTouchAreaWidth = i;
        this.mTouchAreaHeight = i2;
    }

    public int getResult() {
        return this.mResult;
    }

    public long getStartTimestampMillis() {
        return this.mStartTimestampMillis;
    }
}
