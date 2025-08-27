package com.android.keyguard;

/* loaded from: classes.dex */
public interface KeyguardSecurityCallback {
    void dismiss(boolean z, int i);

    void reportUnlockAttempt(int i, boolean z, int i2);

    void reset();

    void userActivity();
}
