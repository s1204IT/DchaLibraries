package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.RilMessage;

class MtkRilMessage extends RilMessage {
    boolean mSetUpMenuFromMD;

    MtkRilMessage(int i, String str) {
        super(i, str);
        this.mSetUpMenuFromMD = false;
    }

    MtkRilMessage(MtkRilMessage mtkRilMessage) {
        super(mtkRilMessage);
        this.mSetUpMenuFromMD = mtkRilMessage.mSetUpMenuFromMD;
    }

    void setSetUpMenuFromMD(boolean z) {
        this.mSetUpMenuFromMD = z;
    }
}
