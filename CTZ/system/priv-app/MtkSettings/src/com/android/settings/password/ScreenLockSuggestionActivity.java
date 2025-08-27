package com.android.settings.password;

import android.app.KeyguardManager;
import android.content.Context;

/* loaded from: classes.dex */
public class ScreenLockSuggestionActivity extends ChooseLockGeneric {
    public static boolean isSuggestionComplete(Context context) {
        return ((KeyguardManager) context.getSystemService(KeyguardManager.class)).isKeyguardSecure();
    }
}
