package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

class SMSTPDUParams extends CommandParams {
    byte[] mData;
    int mLength;
    String mSMSAdress;
    TextMessage mTextMsg;

    SMSTPDUParams(CommandDetails cmdDet, TextMessage textMsg, String smsAdress, int length, byte[] data) {
        super(cmdDet);
        this.mLength = 0;
        this.mData = null;
        this.mTextMsg = textMsg;
        this.mSMSAdress = smsAdress;
        this.mLength = length;
        this.mData = data;
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
