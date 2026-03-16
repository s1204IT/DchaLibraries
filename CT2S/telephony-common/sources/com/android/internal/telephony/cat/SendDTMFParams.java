package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

class SendDTMFParams extends CommandParams {
    String mDTMFString;
    TextMessage mTextMsg;

    SendDTMFParams(CommandDetails cmdDet, TextMessage textMsg, String dtmfString) {
        super(cmdDet);
        this.mTextMsg = textMsg;
        this.mDTMFString = dtmfString;
    }

    @Override
    boolean setIcon(Bitmap icon) {
        if (icon == null || this.mTextMsg == null) {
            return false;
        }
        this.mTextMsg.icon = icon;
        return true;
    }
}
