package com.android.internal.telephony.cat;

class EventDownloadCallInfo {
    int mCause;
    int mCauseLen;
    int mIsFarEnd;
    int mIsMTCall;
    int mState;
    int mTi;

    EventDownloadCallInfo(int state, int ti, int isMTCall, int isFarEnd, int cause_len, int cause) {
        this.mState = state;
        this.mTi = ti;
        this.mIsMTCall = isMTCall;
        this.mIsFarEnd = isFarEnd;
        this.mCauseLen = cause_len;
        this.mCause = cause;
    }
}
