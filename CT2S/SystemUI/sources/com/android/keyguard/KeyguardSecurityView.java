package com.android.keyguard;

import com.android.internal.widget.LockPatternUtils;

public interface KeyguardSecurityView {
    void hideBouncer(int i);

    boolean needsInput();

    void onPause();

    void onResume(int i);

    void setKeyguardCallback(KeyguardSecurityCallback keyguardSecurityCallback);

    void setLockPatternUtils(LockPatternUtils lockPatternUtils);

    void showBouncer(int i);

    void showUsabilityHint();

    void startAppearAnimation();

    boolean startDisappearAnimation(Runnable runnable);
}
