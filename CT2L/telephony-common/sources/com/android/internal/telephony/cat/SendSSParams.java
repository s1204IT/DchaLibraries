package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

class SendSSParams extends CommandParams {
    String mSSString;
    TextMessage mTextMsg;

    SendSSParams(CommandDetails cmdDet, TextMessage textMsg, String sSString) {
        super(cmdDet);
        this.mTextMsg = textMsg;
        this.mSSString = sSString;
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
