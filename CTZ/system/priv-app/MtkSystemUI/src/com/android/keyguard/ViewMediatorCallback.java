package com.android.keyguard;

import android.os.Bundle;

/* loaded from: classes.dex */
public interface ViewMediatorCallback {
    void adjustStatusBarLocked();

    CharSequence consumeCustomMessage();

    int getBouncerPromptReason();

    void hideLocked();

    boolean isKeyguardDoneOnGoing();

    boolean isScreenOn();

    boolean isSecure();

    boolean isShowing();

    void keyguardDone(boolean z, int i);

    void keyguardDoneDrawing();

    void keyguardDonePending(boolean z, int i);

    void keyguardGone();

    void onBouncerVisiblityChanged(boolean z);

    void onSecondaryDisplayShowingChanged(int i);

    void playTrustedSound();

    void readyForKeyguardDone();

    void resetKeyguard();

    void resetStateLocked();

    void setNeedsInput(boolean z);

    void setSuppressPlaySoundFlag();

    void showLocked(Bundle bundle);

    void updateNavbarStatus();

    void userActivity();
}
