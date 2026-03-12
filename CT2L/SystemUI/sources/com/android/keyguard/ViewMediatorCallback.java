package com.android.keyguard;

public interface ViewMediatorCallback {
    boolean isInputRestricted();

    void keyguardDone(boolean z);

    void keyguardDoneDrawing();

    void keyguardDonePending();

    void keyguardGone();

    void onUserActivityTimeoutChanged();

    void playTrustedSound();

    void readyForKeyguardDone();

    void setNeedsInput(boolean z);

    void userActivity();
}
