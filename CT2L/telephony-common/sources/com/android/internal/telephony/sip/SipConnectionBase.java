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
    private long mHoldingStartTime;
    private int mNextPostDialChar;
    private String mPostDialString;
    private long mDuration = -1;
    private int mCause = 0;
    private Connection.PostDialState mPostDialState = Connection.PostDialState.NOT_STARTED;

    protected abstract Phone getPhone();

    SipConnectionBase(String dialString) {
        log("SipConnectionBase: ctor dialString=" + dialString);
        this.mPostDialString = PhoneNumberUtils.extractPostDialPortion(dialString);
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

    @Override
    public int getDisconnectCause() {
        return this.mCause;
    }

    void setDisconnectCause(int cause) {
        log("setDisconnectCause: prev=" + this.mCause + " new=" + cause);
        this.mCause = cause;
    }

    @Override
    public Connection.PostDialState getPostDialState() {
        return this.mPostDialState;
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

    @Override
    public String getRemainingPostDialString() {
        if (this.mPostDialState != Connection.PostDialState.CANCELLED && this.mPostDialState != Connection.PostDialState.COMPLETE && this.mPostDialString != null && this.mPostDialString.length() > this.mNextPostDialChar) {
            return this.mPostDialString.substring(this.mNextPostDialChar);
        }
        log("getRemaingPostDialString: ret empty string");
        return "";
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
