package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

public final class KeyguardMonitor {
    private final ArrayList<Callback> mCallbacks = new ArrayList<>();
    private boolean mSecure;
    private boolean mShowing;

    public interface Callback {
        void onKeyguardChanged();
    }

    public void addCallback(Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public boolean isSecure() {
        return this.mSecure;
    }

    public void notifyKeyguardState(boolean showing, boolean secure) {
        if (this.mShowing != showing || this.mSecure != secure) {
            this.mShowing = showing;
            this.mSecure = secure;
            for (Callback callback : this.mCallbacks) {
                callback.onKeyguardChanged();
            }
        }
    }
}
