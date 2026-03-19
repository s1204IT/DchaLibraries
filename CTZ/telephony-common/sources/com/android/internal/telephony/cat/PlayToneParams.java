package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

public class PlayToneParams extends CommandParams {
    ToneSettings mSettings;
    TextMessage mTextMsg;

    public PlayToneParams(CommandDetails commandDetails, TextMessage textMessage, Tone tone, Duration duration, boolean z) {
        super(commandDetails);
        this.mTextMsg = textMessage;
        this.mSettings = new ToneSettings(duration, tone, z);
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
