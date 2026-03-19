package com.android.commands.monkey;

import android.app.IActivityManager;
import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.IWindowManager;
import android.view.KeyEvent;

public class MonkeyKeyEvent extends MonkeyEvent {
    private int mAction;
    private int mDeviceId;
    private long mDownTime;
    private long mEventTime;
    private int mKeyCode;
    private KeyEvent mKeyEvent;
    private int mMetaState;
    private int mRepeatCount;
    private int mScanCode;

    public MonkeyKeyEvent(int i, int i2) {
        this(-1L, -1L, i, i2, 0, 0, -1, 0);
    }

    public MonkeyKeyEvent(long j, long j2, int i, int i2, int i3, int i4, int i5, int i6) {
        super(0);
        this.mDownTime = j;
        this.mEventTime = j2;
        this.mAction = i;
        this.mKeyCode = i2;
        this.mRepeatCount = i3;
        this.mMetaState = i4;
        this.mDeviceId = i5;
        this.mScanCode = i6;
    }

    public MonkeyKeyEvent(KeyEvent keyEvent) {
        super(0);
        this.mKeyEvent = keyEvent;
    }

    public int getKeyCode() {
        return this.mKeyEvent != null ? this.mKeyEvent.getKeyCode() : this.mKeyCode;
    }

    public int getAction() {
        return this.mKeyEvent != null ? this.mKeyEvent.getAction() : this.mAction;
    }

    public long getDownTime() {
        return this.mKeyEvent != null ? this.mKeyEvent.getDownTime() : this.mDownTime;
    }

    public long getEventTime() {
        return this.mKeyEvent != null ? this.mKeyEvent.getEventTime() : this.mEventTime;
    }

    public void setDownTime(long j) {
        if (this.mKeyEvent != null) {
            throw new IllegalStateException("Cannot modify down time of this key event.");
        }
        this.mDownTime = j;
    }

    public void setEventTime(long j) {
        if (this.mKeyEvent != null) {
            throw new IllegalStateException("Cannot modify event time of this key event.");
        }
        this.mEventTime = j;
    }

    @Override
    public boolean isThrottlable() {
        return getAction() == 1;
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) {
        String str;
        if (i > 1) {
            if (this.mAction == 1) {
                str = "ACTION_UP";
            } else {
                str = "ACTION_DOWN";
            }
            String str2 = str;
            try {
                Logger.out.println(":Sending Key (" + str2 + "): " + this.mKeyCode + "    // " + MonkeySourceRandom.getKeyName(this.mKeyCode));
            } catch (ArrayIndexOutOfBoundsException e) {
                Logger.out.println(":Sending Key (" + str2 + "): " + this.mKeyCode + "    // Unknown key event");
            }
        }
        KeyEvent keyEvent = this.mKeyEvent;
        if (keyEvent == null) {
            long jUptimeMillis = this.mEventTime;
            if (jUptimeMillis <= 0) {
                jUptimeMillis = SystemClock.uptimeMillis();
            }
            long j = jUptimeMillis;
            long j2 = this.mDownTime;
            keyEvent = new KeyEvent(j2 <= 0 ? j : j2, j, this.mAction, this.mKeyCode, this.mRepeatCount, this.mMetaState, this.mDeviceId, this.mScanCode, 8, 257);
        }
        if (InputManager.getInstance().injectInputEvent(keyEvent, 1)) {
            return 1;
        }
        return 0;
    }
}
