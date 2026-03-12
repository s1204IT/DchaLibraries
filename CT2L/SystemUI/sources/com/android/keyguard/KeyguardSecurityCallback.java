package com.android.keyguard;

public interface KeyguardSecurityCallback {
    void dismiss(boolean z);

    void reportUnlockAttempt(boolean z);

    void showBackupSecurity();

    void userActivity();
}
