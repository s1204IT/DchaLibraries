package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

public class CallSetupParams extends CommandParams {
    public TextMessage mCallMsg;
    public TextMessage mConfirmMsg;

    public CallSetupParams(CommandDetails commandDetails, TextMessage textMessage, TextMessage textMessage2) {
        super(commandDetails);
        this.mConfirmMsg = textMessage;
        this.mCallMsg = textMessage2;
    }

    @Override
    boolean setIcon(Bitmap bitmap) {
        if (bitmap == null) {
            return false;
        }
        if (this.mConfirmMsg != null && this.mConfirmMsg.icon == null) {
            this.mConfirmMsg.icon = bitmap;
            return true;
        }
        if (this.mCallMsg == null || this.mCallMsg.icon != null) {
            return false;
        }
        this.mCallMsg.icon = bitmap;
        return true;
    }
}
