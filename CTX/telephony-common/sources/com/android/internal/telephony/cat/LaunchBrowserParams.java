package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

public class LaunchBrowserParams extends CommandParams {
    public TextMessage mConfirmMsg;
    public LaunchBrowserMode mMode;
    public String mUrl;

    public LaunchBrowserParams(CommandDetails commandDetails, TextMessage textMessage, String str, LaunchBrowserMode launchBrowserMode) {
        super(commandDetails);
        this.mConfirmMsg = textMessage;
        this.mMode = launchBrowserMode;
        this.mUrl = str;
    }

    @Override
    boolean setIcon(Bitmap bitmap) {
        if (bitmap != null && this.mConfirmMsg != null) {
            this.mConfirmMsg.icon = bitmap;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "TextMessage=" + this.mConfirmMsg + " " + super.toString();
    }
}
