package com.mediatek.internal.telephony.imsphone;

import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;

public class MtkImsPhoneCall extends ImsPhoneCall {
    private static final String LOG_TAG = "MtkImsPhoneCall";

    public MtkImsPhoneCall(ImsPhoneCallTracker imsPhoneCallTracker, String str) {
        super(imsPhoneCallTracker, str);
    }

    void resetRingbackTone() {
        this.mRingbackTonePlayed = false;
    }

    protected void setConferenceAsHostIfNecessary(ImsPhoneConnection imsPhoneConnection) {
        if (imsPhoneConnection instanceof MtkImsPhoneConnection) {
            ((MtkImsPhoneConnection) imsPhoneConnection).setConferenceAsHost();
        }
    }

    protected ImsPhoneCall makeTempImsPhoneCall() {
        return new MtkImsPhoneCall(this.mOwner, "UK");
    }
}
