package com.android.internal.telephony.sip;

import android.os.SystemClock;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.UUSInfo;

abstract class SipConnectionBase extends Connection {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "SipConnBase";
    private static final boolean VDBG = false;
    private long mConnectTime;
    private long mConnectTimeReal;
    private long mCreateTime;
    private long mDisconnectTime;
    private long mDuration;
    private long mHoldingStartTime;

    protected abstract Phone getPhone();

    SipConnectionBase(String str) {
        super(3);
        this.mDuration = -1L;
        log("SipConnectionBase: ctor dialString=" + SipPhone.hidePii(str));
        this.mPostDialString = PhoneNumberUtils.extractPostDialPortion(str);
        this.mCreateTime = System.currentTimeMillis();
    }

    protected void setState(Call.State state) {
        log("setState: state=" + state);
        switch (state) {
            case ACTIVE:
                if (this.mConnectTime == 0) {
                    this.mConnectTimeReal = SystemClock.elapsedRealtime();
                    this.mConnectTime = System.currentTimeMillis();
                }
                break;
            case DISCONNECTED:
                this.mDuration = getDurationMillis();
                this.mDisconnectTime = System.currentTimeMillis();
                break;
            case HOLDING:
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
            return SystemClock.elapsedRealtime() - this.mConnectTimeReal;
        }
        return this.mDuration;
    }

    @Override
    public long getHoldDurationMillis() {
        if (getState() != Call.State.HOLDING) {
            return 0L;
        }
        return SystemClock.elapsedRealtime() - this.mHoldingStartTime;
    }

    void setDisconnectCause(int i) {
        log("setDisconnectCause: prev=" + this.mCause + " new=" + i);
        this.mCause = i;
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

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
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
