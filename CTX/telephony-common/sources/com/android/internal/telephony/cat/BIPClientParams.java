package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

public class BIPClientParams extends CommandParams {
    public boolean mHasAlphaId;
    public TextMessage mTextMsg;

    BIPClientParams(CommandDetails commandDetails, TextMessage textMessage, boolean z) {
        super(commandDetails);
        this.mTextMsg = textMessage;
        this.mHasAlphaId = z;
    }

    @Override
    boolean setIcon(Bitmap bitmap) {
        if (bitmap != null && this.mTextMsg != null) {
            this.mTextMsg.icon = bitmap;
            return true;
        }
        return false;
    }
}
