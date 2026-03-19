package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

class DisplayTextParams extends CommandParams {
    TextMessage mTextMsg;

    DisplayTextParams(CommandDetails cmdDet, TextMessage textMsg) {
        super(cmdDet);
        this.mTextMsg = textMsg;
    }

    @Override
    boolean setIcon(Bitmap icon) {
        if (icon != null && this.mTextMsg != null) {
            this.mTextMsg.icon = icon;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "TextMessage=" + this.mTextMsg + " " + super.toString();
    }
}
