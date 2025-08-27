package com.android.keyguard;

import com.android.internal.widget.LockPatternUtils;

/* loaded from: classes.dex */
public interface KeyguardSecurityView {
    CharSequence getTitle();

    boolean needsInput();

    void onPause();

    void onResume(int i);

    void reset();

    void setKeyguardCallback(KeyguardSecurityCallback keyguardSecurityCallback);

    void setLockPatternUtils(LockPatternUtils lockPatternUtils);

    void showMessage(CharSequence charSequence, int i);

    void showPromptReason(int i);

    void startAppearAnimation();

    boolean startDisappearAnimation(Runnable runnable);
}
