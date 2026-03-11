package com.android.keyguard;

public interface KeyguardSecurityCallback {
    void dismiss(boolean z);

    void reportUnlockAttempt(int i, boolean z, int i2);

    void reset();

    void userActivity();
}
