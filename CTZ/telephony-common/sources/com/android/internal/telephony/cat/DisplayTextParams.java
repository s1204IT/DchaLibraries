package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

public class DisplayTextParams extends CommandParams {
    public TextMessage mTextMsg;

    public DisplayTextParams(CommandDetails commandDetails, TextMessage textMessage) {
        super(commandDetails);
        this.mTextMsg = textMessage;
    }

    @Override
    boolean setIcon(Bitmap bitmap) {
        if (bitmap != null && this.mTextMsg != null) {
            this.mTextMsg.icon = bitmap;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "TextMessage=" + this.mTextMsg + " " + super.toString();
    }
}
