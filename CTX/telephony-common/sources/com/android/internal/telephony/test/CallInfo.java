package com.android.internal.telephony.test;

import com.android.internal.telephony.ATParseEx;
import com.android.internal.telephony.DriverCall;

class CallInfo {
    boolean mIsMT;
    boolean mIsMpty;
    String mNumber;
    State mState;
    int mTOA;

    enum State {
        ACTIVE(0),
        HOLDING(1),
        DIALING(2),
        ALERTING(3),
        INCOMING(4),
        WAITING(5);

        private final int mValue;

        State(int i) {
            this.mValue = i;
        }

        public int value() {
            return this.mValue;
        }
    }

    CallInfo(boolean z, State state, boolean z2, String str) {
        this.mIsMT = z;
        this.mState = state;
        this.mIsMpty = z2;
        this.mNumber = str;
        if (str.length() > 0 && str.charAt(0) == '+') {
            this.mTOA = 145;
        } else {
            this.mTOA = 129;
        }
    }

    static CallInfo createOutgoingCall(String str) {
        return new CallInfo(false, State.DIALING, false, str);
    }

    static CallInfo createIncomingCall(String str) {
        return new CallInfo(true, State.INCOMING, false, str);
    }

    String toCLCCLine(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("+CLCC: ");
        sb.append(i);
        sb.append(",");
        sb.append(this.mIsMT ? "1" : "0");
        sb.append(",");
        sb.append(this.mState.value());
        sb.append(",0,");
        sb.append(this.mIsMpty ? "1" : "0");
        sb.append(",\"");
        sb.append(this.mNumber);
        sb.append("\",");
        sb.append(this.mTOA);
        return sb.toString();
    }

    DriverCall toDriverCall(int i) {
        DriverCall driverCall = new DriverCall();
        driverCall.index = i;
        driverCall.isMT = this.mIsMT;
        try {
            driverCall.state = DriverCall.stateFromCLCC(this.mState.value());
            driverCall.isMpty = this.mIsMpty;
            driverCall.number = this.mNumber;
            driverCall.TOA = this.mTOA;
            driverCall.isVoice = true;
            driverCall.als = 0;
            return driverCall;
        } catch (ATParseEx e) {
            throw new RuntimeException("should never happen", e);
        }
    }

    boolean isActiveOrHeld() {
        return this.mState == State.ACTIVE || this.mState == State.HOLDING;
    }

    boolean isConnecting() {
        return this.mState == State.DIALING || this.mState == State.ALERTING;
    }

    boolean isRinging() {
        return this.mState == State.INCOMING || this.mState == State.WAITING;
    }
}
