package com.android.phone;

public class CdmaPhoneCallState {
    private boolean mAddCallMenuStateAfterCW;
    private PhoneCallState mCurrentCallState;
    private PhoneCallState mPreviousCallState;
    private boolean mThreeWayCallOrigStateDialing;

    public enum PhoneCallState {
        IDLE,
        SINGLE_ACTIVE,
        THRWAY_ACTIVE,
        CONF_CALL
    }

    public void CdmaPhoneCallStateInit() {
        this.mCurrentCallState = PhoneCallState.IDLE;
        this.mPreviousCallState = PhoneCallState.IDLE;
        this.mThreeWayCallOrigStateDialing = false;
        this.mAddCallMenuStateAfterCW = true;
    }

    public PhoneCallState getCurrentCallState() {
        return this.mCurrentCallState;
    }

    public void setCurrentCallState(PhoneCallState phoneCallState) {
        this.mPreviousCallState = this.mCurrentCallState;
        this.mCurrentCallState = phoneCallState;
        this.mThreeWayCallOrigStateDialing = false;
        if (this.mCurrentCallState == PhoneCallState.SINGLE_ACTIVE && this.mPreviousCallState == PhoneCallState.IDLE) {
            this.mAddCallMenuStateAfterCW = true;
        }
    }

    public boolean IsThreeWayCallOrigStateDialing() {
        return this.mThreeWayCallOrigStateDialing;
    }

    public void setThreeWayCallOrigState(boolean z) {
        this.mThreeWayCallOrigStateDialing = z;
    }

    public boolean getAddCallMenuStateAfterCallWaiting() {
        return this.mAddCallMenuStateAfterCW;
    }

    public void setAddCallMenuStateAfterCallWaiting(boolean z) {
        this.mAddCallMenuStateAfterCW = z;
    }

    public PhoneCallState getPreviousCallState() {
        return this.mPreviousCallState;
    }

    public void resetCdmaPhoneCallState() {
        this.mCurrentCallState = PhoneCallState.IDLE;
        this.mPreviousCallState = PhoneCallState.IDLE;
        this.mThreeWayCallOrigStateDialing = false;
        this.mAddCallMenuStateAfterCW = true;
    }
}
