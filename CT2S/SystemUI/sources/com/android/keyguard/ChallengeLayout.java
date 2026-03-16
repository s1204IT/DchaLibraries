package com.android.keyguard;

public interface ChallengeLayout {

    public interface OnBouncerStateChangedListener {
        void onBouncerStateChanged(boolean z);
    }

    int getBouncerAnimationDuration();

    boolean isBouncing();

    boolean isChallengeOverlapping();

    boolean isChallengeShowing();

    void setOnBouncerStateChangedListener(OnBouncerStateChangedListener onBouncerStateChangedListener);

    void showBouncer();
}
