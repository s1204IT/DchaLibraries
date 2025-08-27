package com.mediatek.keyguard.ext;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

/* loaded from: classes.dex */
public class DefaultKeyguardUtilExt implements IKeyguardUtilExt {
    private static final String TAG = "DefaultKeyguardUtilExt";

    @Override // com.mediatek.keyguard.ext.IKeyguardUtilExt
    public void showToastWhenUnlockPinPuk(Context context, int i) {
    }

    @Override // com.mediatek.keyguard.ext.IKeyguardUtilExt
    public void customizePinPukLockView(int i, ImageView imageView, TextView textView) {
    }

    @Override // com.mediatek.keyguard.ext.IKeyguardUtilExt
    public void customizeCarrierTextGravity(TextView textView) {
    }

    @Override // com.mediatek.keyguard.ext.IKeyguardUtilExt
    public boolean lockImmediatelyWhenScreenTimeout() {
        return false;
    }
}
