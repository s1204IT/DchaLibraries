package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

class SendUSSDParams extends CommandParams {
    TextMessage mTextMsg;
    String mUSSDString;

    SendUSSDParams(CommandDetails cmdDet, TextMessage textMsg, String uSSDString) {
        super(cmdDet);
        this.mTextMsg = textMsg;
        this.mUSSDString = uSSDString;
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
