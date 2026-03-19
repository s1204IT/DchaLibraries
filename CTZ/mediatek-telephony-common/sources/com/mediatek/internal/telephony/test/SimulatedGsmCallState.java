package com.mediatek.internal.telephony.test;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.internal.telephony.DriverCall;
import com.mediatek.internal.telephony.test.CallInfo;
import java.util.ArrayList;
import java.util.List;

class SimulatedGsmCallState extends Handler {
    static final int CONNECTING_PAUSE_MSEC = 500;
    static final int EVENT_PROGRESS_CALL_STATE = 1;
    static final int MAX_CALLS = 7;
    private boolean mAutoProgressConnecting;
    CallInfo[] mCalls;
    private boolean mNextDialFailImmediately;

    public SimulatedGsmCallState(Looper looper) {
        super(looper);
        this.mCalls = new CallInfo[7];
        this.mAutoProgressConnecting = true;
    }

    @Override
    public void handleMessage(Message message) {
        synchronized (this) {
            if (message.what == 1) {
                progressConnectingCallState();
            }
        }
    }

    public boolean triggerRing(String str) {
        synchronized (this) {
            int i = -1;
            boolean z = false;
            for (int i2 = 0; i2 < this.mCalls.length; i2++) {
                CallInfo callInfo = this.mCalls[i2];
                if (callInfo == null && i < 0) {
                    i = i2;
                } else {
                    if (callInfo != null && (callInfo.mState == CallInfo.State.INCOMING || callInfo.mState == CallInfo.State.WAITING)) {
                        Rlog.w("ModelInterpreter", "triggerRing failed; phone already ringing");
                        return false;
                    }
                    if (callInfo != null) {
                        z = true;
                    }
                }
            }
            if (i < 0) {
                Rlog.w("ModelInterpreter", "triggerRing failed; all full");
                return false;
            }
            this.mCalls[i] = CallInfo.createIncomingCall(PhoneNumberUtils.extractNetworkPortion(str));
            if (z) {
                this.mCalls[i].mState = CallInfo.State.WAITING;
            }
            return true;
        }
    }

    public void progressConnectingCallState() {
        synchronized (this) {
            int i = 0;
            while (true) {
                if (i >= this.mCalls.length) {
                    break;
                }
                CallInfo callInfo = this.mCalls[i];
                if (callInfo != null && callInfo.mState == CallInfo.State.DIALING) {
                    callInfo.mState = CallInfo.State.ALERTING;
                    if (this.mAutoProgressConnecting) {
                        sendMessageDelayed(obtainMessage(1, callInfo), 500L);
                    }
                } else if (callInfo == null || callInfo.mState != CallInfo.State.ALERTING) {
                    i++;
                } else {
                    callInfo.mState = CallInfo.State.ACTIVE;
                    break;
                }
            }
        }
    }

    public void progressConnectingToActive() {
        synchronized (this) {
            for (int i = 0; i < this.mCalls.length; i++) {
                CallInfo callInfo = this.mCalls[i];
                if (callInfo != null && (callInfo.mState == CallInfo.State.DIALING || callInfo.mState == CallInfo.State.ALERTING)) {
                    callInfo.mState = CallInfo.State.ACTIVE;
                    break;
                }
            }
        }
    }

    public void setAutoProgressConnectingCall(boolean z) {
        this.mAutoProgressConnecting = z;
    }

    public void setNextDialFailImmediately(boolean z) {
        this.mNextDialFailImmediately = z;
    }

    public boolean triggerHangupForeground() {
        boolean z;
        synchronized (this) {
            z = false;
            for (int i = 0; i < this.mCalls.length; i++) {
                CallInfo callInfo = this.mCalls[i];
                if (callInfo != null && (callInfo.mState == CallInfo.State.INCOMING || callInfo.mState == CallInfo.State.WAITING)) {
                    this.mCalls[i] = null;
                    z = true;
                }
            }
            for (int i2 = 0; i2 < this.mCalls.length; i2++) {
                CallInfo callInfo2 = this.mCalls[i2];
                if (callInfo2 != null && (callInfo2.mState == CallInfo.State.DIALING || callInfo2.mState == CallInfo.State.ACTIVE || callInfo2.mState == CallInfo.State.ALERTING)) {
                    this.mCalls[i2] = null;
                    z = true;
                }
            }
        }
        return z;
    }

    public boolean triggerHangupBackground() {
        boolean z;
        synchronized (this) {
            z = false;
            for (int i = 0; i < this.mCalls.length; i++) {
                CallInfo callInfo = this.mCalls[i];
                if (callInfo != null && callInfo.mState == CallInfo.State.HOLDING) {
                    this.mCalls[i] = null;
                    z = true;
                }
            }
        }
        return z;
    }

    public boolean triggerHangupAll() {
        boolean z;
        synchronized (this) {
            z = false;
            for (int i = 0; i < this.mCalls.length; i++) {
                CallInfo callInfo = this.mCalls[i];
                if (this.mCalls[i] != null) {
                    z = true;
                }
                this.mCalls[i] = null;
            }
        }
        return z;
    }

    public boolean onAnswer() {
        synchronized (this) {
            for (int i = 0; i < this.mCalls.length; i++) {
                CallInfo callInfo = this.mCalls[i];
                if (callInfo != null && (callInfo.mState == CallInfo.State.INCOMING || callInfo.mState == CallInfo.State.WAITING)) {
                    return switchActiveAndHeldOrWaiting();
                }
            }
            return false;
        }
    }

    public boolean onHangup() {
        boolean z = false;
        for (int i = 0; i < this.mCalls.length; i++) {
            CallInfo callInfo = this.mCalls[i];
            if (callInfo != null && callInfo.mState != CallInfo.State.WAITING) {
                this.mCalls[i] = null;
                z = true;
            }
        }
        return z;
    }

    public boolean onChld(char c, char c2) {
        int i;
        if (c2 != 0) {
            i = c2 - '1';
            if (i < 0 || i >= this.mCalls.length) {
                return false;
            }
        } else {
            i = 0;
        }
        switch (c) {
            case '0':
                return releaseHeldOrUDUB();
            case '1':
                if (c2 <= 0) {
                    return releaseActiveAcceptHeldOrWaiting();
                }
                if (this.mCalls[i] == null) {
                    return false;
                }
                this.mCalls[i] = null;
                return true;
            case '2':
                if (c2 <= 0) {
                    return switchActiveAndHeldOrWaiting();
                }
                return separateCall(i);
            case '3':
                return conference();
            case '4':
                return explicitCallTransfer();
            case '5':
            default:
                return false;
        }
    }

    public boolean releaseHeldOrUDUB() {
        boolean z;
        int i = 0;
        while (true) {
            if (i < this.mCalls.length) {
                CallInfo callInfo = this.mCalls[i];
                if (callInfo == null || !callInfo.isRinging()) {
                    i++;
                } else {
                    this.mCalls[i] = null;
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (!z) {
            for (int i2 = 0; i2 < this.mCalls.length; i2++) {
                CallInfo callInfo2 = this.mCalls[i2];
                if (callInfo2 != null && callInfo2.mState == CallInfo.State.HOLDING) {
                    this.mCalls[i2] = null;
                }
            }
        }
        return true;
    }

    public boolean releaseActiveAcceptHeldOrWaiting() {
        boolean z = false;
        for (int i = 0; i < this.mCalls.length; i++) {
            CallInfo callInfo = this.mCalls[i];
            if (callInfo != null && callInfo.mState == CallInfo.State.ACTIVE) {
                this.mCalls[i] = null;
                z = true;
            }
        }
        if (!z) {
            for (int i2 = 0; i2 < this.mCalls.length; i2++) {
                CallInfo callInfo2 = this.mCalls[i2];
                if (callInfo2 != null && (callInfo2.mState == CallInfo.State.DIALING || callInfo2.mState == CallInfo.State.ALERTING)) {
                    this.mCalls[i2] = null;
                }
            }
        }
        boolean z2 = false;
        for (int i3 = 0; i3 < this.mCalls.length; i3++) {
            CallInfo callInfo3 = this.mCalls[i3];
            if (callInfo3 != null && callInfo3.mState == CallInfo.State.HOLDING) {
                callInfo3.mState = CallInfo.State.ACTIVE;
                z2 = true;
            }
        }
        if (z2) {
            return true;
        }
        for (int i4 = 0; i4 < this.mCalls.length; i4++) {
            CallInfo callInfo4 = this.mCalls[i4];
            if (callInfo4 != null && callInfo4.isRinging()) {
                callInfo4.mState = CallInfo.State.ACTIVE;
                return true;
            }
        }
        return true;
    }

    public boolean switchActiveAndHeldOrWaiting() {
        boolean z;
        int i = 0;
        while (true) {
            if (i < this.mCalls.length) {
                CallInfo callInfo = this.mCalls[i];
                if (callInfo == null || callInfo.mState != CallInfo.State.HOLDING) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        for (int i2 = 0; i2 < this.mCalls.length; i2++) {
            CallInfo callInfo2 = this.mCalls[i2];
            if (callInfo2 != null) {
                if (callInfo2.mState == CallInfo.State.ACTIVE) {
                    callInfo2.mState = CallInfo.State.HOLDING;
                } else if (callInfo2.mState == CallInfo.State.HOLDING) {
                    callInfo2.mState = CallInfo.State.ACTIVE;
                } else if (!z && callInfo2.isRinging()) {
                    callInfo2.mState = CallInfo.State.ACTIVE;
                }
            }
        }
        return true;
    }

    public boolean separateCall(int i) {
        boolean z;
        int i2;
        CallInfo callInfo;
        try {
            CallInfo callInfo2 = this.mCalls[i];
            if (callInfo2 != null && !callInfo2.isConnecting() && countActiveLines() == 1) {
                callInfo2.mState = CallInfo.State.ACTIVE;
                callInfo2.mIsMpty = false;
                for (int i3 = 0; i3 < this.mCalls.length; i3++) {
                    if (i3 == i || (callInfo = this.mCalls[i3]) == null || callInfo.mState != CallInfo.State.ACTIVE) {
                        z = false;
                        i2 = 0;
                    } else {
                        callInfo.mState = CallInfo.State.HOLDING;
                        i2 = i3;
                        z = true;
                    }
                    if (z) {
                        this.mCalls[i2].mIsMpty = false;
                    }
                }
                return true;
            }
            return false;
        } catch (InvalidStateEx e) {
            return false;
        }
    }

    public boolean conference() {
        int i = 0;
        for (int i2 = 0; i2 < this.mCalls.length; i2++) {
            CallInfo callInfo = this.mCalls[i2];
            if (callInfo != null) {
                i++;
                if (callInfo.isConnecting()) {
                    return false;
                }
            }
        }
        for (int i3 = 0; i3 < this.mCalls.length; i3++) {
            CallInfo callInfo2 = this.mCalls[i3];
            if (callInfo2 != null) {
                callInfo2.mState = CallInfo.State.ACTIVE;
                if (i > 0) {
                    callInfo2.mIsMpty = true;
                }
            }
        }
        return true;
    }

    public boolean explicitCallTransfer() {
        for (int i = 0; i < this.mCalls.length; i++) {
            CallInfo callInfo = this.mCalls[i];
            if (callInfo != null && callInfo.isConnecting()) {
                return false;
            }
        }
        return triggerHangupAll();
    }

    public boolean onDial(String str) {
        Rlog.d("GSM", "SC> dial '" + str + "'");
        if (this.mNextDialFailImmediately) {
            this.mNextDialFailImmediately = false;
            Rlog.d("GSM", "SC< dial fail (per request)");
            return false;
        }
        String strExtractNetworkPortion = PhoneNumberUtils.extractNetworkPortion(str);
        if (strExtractNetworkPortion.length() == 0) {
            Rlog.d("GSM", "SC< dial fail (invalid ph num)");
            return false;
        }
        if (strExtractNetworkPortion.startsWith("*99") && strExtractNetworkPortion.endsWith("#")) {
            Rlog.d("GSM", "SC< dial ignored (gprs)");
            return true;
        }
        try {
            if (countActiveLines() > 1) {
                Rlog.d("GSM", "SC< dial fail (invalid call state)");
                return false;
            }
            int i = -1;
            for (int i2 = 0; i2 < this.mCalls.length; i2++) {
                if (i < 0 && this.mCalls[i2] == null) {
                    i = i2;
                }
                if (this.mCalls[i2] != null && !this.mCalls[i2].isActiveOrHeld()) {
                    Rlog.d("GSM", "SC< dial fail (invalid call state)");
                    return false;
                }
                if (this.mCalls[i2] != null && this.mCalls[i2].mState == CallInfo.State.ACTIVE) {
                    this.mCalls[i2].mState = CallInfo.State.HOLDING;
                }
            }
            if (i < 0) {
                Rlog.d("GSM", "SC< dial fail (invalid call state)");
                return false;
            }
            this.mCalls[i] = CallInfo.createOutgoingCall(strExtractNetworkPortion);
            if (this.mAutoProgressConnecting) {
                sendMessageDelayed(obtainMessage(1, this.mCalls[i]), 500L);
            }
            Rlog.d("GSM", "SC< dial (slot = " + i + ")");
            return true;
        } catch (InvalidStateEx e) {
            Rlog.d("GSM", "SC< dial fail (invalid call state)");
            return false;
        }
    }

    public List<DriverCall> getDriverCalls() {
        ArrayList arrayList = new ArrayList(this.mCalls.length);
        for (int i = 0; i < this.mCalls.length; i++) {
            CallInfo callInfo = this.mCalls[i];
            if (callInfo != null) {
                arrayList.add(callInfo.toDriverCall(i + 1));
            }
        }
        Rlog.d("GSM", "SC< getDriverCalls " + arrayList);
        return arrayList;
    }

    public List<String> getClccLines() {
        ArrayList arrayList = new ArrayList(this.mCalls.length);
        for (int i = 0; i < this.mCalls.length; i++) {
            CallInfo callInfo = this.mCalls[i];
            if (callInfo != null) {
                arrayList.add(callInfo.toCLCCLine(i + 1));
            }
        }
        return arrayList;
    }

    private int countActiveLines() throws InvalidStateEx {
        int i = 0;
        int i2 = 0;
        boolean z = false;
        boolean z2 = false;
        boolean z3 = false;
        boolean zIsConnecting = false;
        boolean z4 = false;
        boolean zIsRinging = false;
        while (true) {
            if (i2 < this.mCalls.length) {
                CallInfo callInfo = this.mCalls[i2];
                if (callInfo != null) {
                    if (z2 || !callInfo.mIsMpty) {
                        if (callInfo.mIsMpty && z4 && callInfo.mState == CallInfo.State.ACTIVE) {
                            Rlog.e("ModelInterpreter", "Invalid state");
                            throw new InvalidStateEx();
                        }
                        if (!callInfo.mIsMpty && z2 && z4 && callInfo.mState == CallInfo.State.HOLDING) {
                            Rlog.e("ModelInterpreter", "Invalid state");
                            throw new InvalidStateEx();
                        }
                    } else {
                        z4 = callInfo.mState == CallInfo.State.HOLDING;
                    }
                    z2 |= callInfo.mIsMpty;
                    z |= callInfo.mState == CallInfo.State.HOLDING;
                    z3 |= callInfo.mState == CallInfo.State.ACTIVE;
                    zIsConnecting |= callInfo.isConnecting();
                    zIsRinging |= callInfo.isRinging();
                }
                i2++;
            } else {
                if (z) {
                    i = 1;
                }
                if (z3) {
                    i++;
                }
                if (zIsConnecting) {
                    i++;
                }
                return zIsRinging ? i + 1 : i;
            }
        }
    }
}
