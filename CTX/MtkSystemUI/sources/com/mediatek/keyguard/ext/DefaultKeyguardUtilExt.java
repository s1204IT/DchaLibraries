package com.mediatek.keyguard.ext;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

public class DefaultKeyguardUtilExt implements IKeyguardUtilExt {
    private static final String TAG = "DefaultKeyguardUtilExt";

    @Override
    public void showToastWhenUnlockPinPuk(Context context, int i) {
    }

    @Override
    public void customizePinPukLockView(int i, ImageView imageView, TextView textView) {
    }

    @Override
    public void customizeCarrierTextGravity(TextView textView) {
    }

    @Override
    public boolean lockImmediatelyWhenScreenTimeout() {
        return false;
    }
}
