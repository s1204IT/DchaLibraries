package com.android.internal.telephony.sip;

import android.os.SystemClock;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.UUSInfo;

abstract class SipConnectionBase extends Connection {

    private static final int[] f26comandroidinternaltelephonyCall$StateSwitchesValues = null;
    private static final boolean DBG = true;
    private static final String LOG_TAG = "SipConnBase";
    private static final boolean VDBG = false;
    private long mConnectTime;
    private long mConnectTimeReal;
    private long mCreateTime;
    private long mDisconnectTime;
    private long mDuration;
    private long mHoldingStartTime;

    private static int[] m455getcomandroidinternaltelephonyCall$StateSwitchesValues() {
        if (f26comandroidinternaltelephonyCall$StateSwitchesValues != null) {
            return f26comandroidinternaltelephonyCall$StateSwitchesValues;
        }
        int[] iArr = new int[Call.State.valuesCustom().length];
        try {
            iArr[Call.State.ACTIVE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Call.State.ALERTING.ordinal()] = 4;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Call.State.DIALING.ordinal()] = 5;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Call.State.DISCONNECTED.ordinal()] = 2;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[Call.State.DISCONNECTING.ordinal()] = 6;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[Call.State.HOLDING.ordinal()] = 3;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[Call.State.IDLE.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[Call.State.INCOMING.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[Call.State.WAITING.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        f26comandroidinternaltelephonyCall$StateSwitchesValues = iArr;
        return iArr;
    }

    protected abstract Phone getPhone();

    SipConnectionBase(String dialString) {
        super(3);
        this.mDuration = -1L;
        log("SipConnectionBase: ctor dialString=" + SipPhone.hidePii(dialString));
        this.mPostDialString = PhoneNumberUtils.extractPostDialPortion(dialString);
        this.mCreateTime = System.currentTimeMillis();
    }

    protected void setState(Call.State state) {
        log("setState: state=" + state);
        switch (m455getcomandroidinternaltelephonyCall$StateSwitchesValues()[state.ordinal()]) {
            case 1:
                if (this.mConnectTime == 0) {
                    this.mConnectTimeReal = SystemClock.elapsedRealtime();
                    this.mConnectTime = System.currentTimeMillis();
                }
                break;
            case 2:
                this.mDuration = getDurationMillis();
                this.mDisconnectTime = System.currentTimeMillis();
                break;
            case 3:
                this.mHoldingStartTime = SystemClock.elapsedRealtime();
                break;
        }
    }

    @Override
    public long getCreateTime() {
        return this.mCreateTime;
    }

    @Override
    public long getConnectTime() {
        return this.mConnectTime;
    }

    @Override
    public long getDisconnectTime() {
        return this.mDisconnectTime;
    }

    @Override
    public long getDurationMillis() {
        if (this.mConnectTimeReal == 0) {
            return 0L;
        }
        if (this.mDuration < 0) {
            long dur = SystemClock.elapsedRealtime() - this.mConnectTimeReal;
            return dur;
        }
        long dur2 = this.mDuration;
        return dur2;
    }

    @Override
    public long getHoldDurationMillis() {
        if (getState() != Call.State.HOLDING) {
            return 0L;
        }
        long dur = SystemClock.elapsedRealtime() - this.mHoldingStartTime;
        return dur;
    }

    void setDisconnectCause(int cause) {
        log("setDisconnectCause: prev=" + this.mCause + " new=" + cause);
        this.mCause = cause;
    }

    @Override
    public String getVendorDisconnectCause() {
        return null;
    }

    @Override
    public void proceedAfterWaitChar() {
        log("proceedAfterWaitChar: ignore");
    }

    @Override
    public void proceedAfterWildChar(String str) {
        log("proceedAfterWildChar: ignore");
    }

    @Override
    public void cancelPostDial() {
        log("cancelPostDial: ignore");
    }

    private void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    @Override
    public int getNumberPresentation() {
        return 1;
    }

    @Override
    public UUSInfo getUUSInfo() {
        return null;
    }

    @Override
    public int getPreciseDisconnectCause() {
        return 0;
    }

    @Override
    public long getHoldingStartTime() {
        return this.mHoldingStartTime;
    }

    @Override
    public long getConnectTimeReal() {
        return this.mConnectTimeReal;
    }

    @Override
    public Connection getOrigConnection() {
        return null;
    }

    @Override
    public boolean isMultiparty() {
        return false;
    }
}
