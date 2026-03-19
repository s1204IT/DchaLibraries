package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

class LaunchBrowserParams extends CommandParams {
    TextMessage mConfirmMsg;
    LaunchBrowserMode mMode;
    String mUrl;

    LaunchBrowserParams(CommandDetails cmdDet, TextMessage confirmMsg, String url, LaunchBrowserMode mode) {
        super(cmdDet);
        this.mConfirmMsg = confirmMsg;
        this.mMode = mode;
        this.mUrl = url;
    }

    @Override
    boolean setIcon(Bitmap icon) {
        if (icon != null && this.mConfirmMsg != null) {
            this.mConfirmMsg.icon = icon;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "TextMessage=" + this.mConfirmMsg + " " + super.toString();
    }
}
