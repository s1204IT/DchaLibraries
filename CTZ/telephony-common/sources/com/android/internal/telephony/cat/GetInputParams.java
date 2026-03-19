package com.android.internal.telephony.cat;

import android.graphics.Bitmap;

public class GetInputParams extends CommandParams {
    Input mInput;

    public GetInputParams(CommandDetails commandDetails, Input input) {
        super(commandDetails);
        this.mInput = null;
        this.mInput = input;
    }

    @Override
    boolean setIcon(Bitmap bitmap) {
        if (bitmap != null && this.mInput != null) {
            this.mInput.icon = bitmap;
            return true;
        }
        return true;
    }
}
