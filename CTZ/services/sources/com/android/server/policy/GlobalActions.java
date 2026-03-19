package com.android.server.policy;

import android.content.Context;
import android.os.Handler;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.policy.GlobalActionsProvider;
import com.android.server.policy.WindowManagerPolicy;

class GlobalActions implements GlobalActionsProvider.GlobalActionsListener {
    private static final boolean DEBUG = false;
    private static final String TAG = "GlobalActions";
    private final Context mContext;
    private boolean mDeviceProvisioned;
    private boolean mGlobalActionsAvailable;
    private boolean mKeyguardShowing;
    private LegacyGlobalActions mLegacyGlobalActions;
    private boolean mShowing;
    private final WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;
    private final Runnable mShowTimeout = new Runnable() {
        @Override
        public void run() {
            GlobalActions.this.ensureLegacyCreated();
            GlobalActions.this.mLegacyGlobalActions.showDialog(GlobalActions.this.mKeyguardShowing, GlobalActions.this.mDeviceProvisioned);
        }
    };
    private final Handler mHandler = new Handler();
    private final GlobalActionsProvider mGlobalActionsProvider = (GlobalActionsProvider) LocalServices.getService(GlobalActionsProvider.class);

    public GlobalActions(Context context, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        this.mContext = context;
        this.mWindowManagerFuncs = windowManagerFuncs;
        if (this.mGlobalActionsProvider != null) {
            this.mGlobalActionsProvider.setGlobalActionsListener(this);
        } else {
            Slog.i(TAG, "No GlobalActionsProvider found, defaulting to LegacyGlobalActions");
        }
    }

    private void ensureLegacyCreated() {
        if (this.mLegacyGlobalActions != null) {
            return;
        }
        this.mLegacyGlobalActions = new LegacyGlobalActions(this.mContext, this.mWindowManagerFuncs, new Runnable() {
            @Override
            public final void run() {
                this.f$0.onGlobalActionsDismissed();
            }
        });
    }

    public void showDialog(boolean z, boolean z2) {
        if (this.mGlobalActionsProvider != null && this.mGlobalActionsProvider.isGlobalActionsDisabled()) {
            return;
        }
        this.mKeyguardShowing = z;
        this.mDeviceProvisioned = z2;
        this.mShowing = true;
        if (this.mGlobalActionsAvailable) {
            this.mHandler.postDelayed(this.mShowTimeout, 5000L);
            this.mGlobalActionsProvider.showGlobalActions();
        } else {
            ensureLegacyCreated();
            this.mLegacyGlobalActions.showDialog(this.mKeyguardShowing, this.mDeviceProvisioned);
        }
    }

    @Override
    public void onGlobalActionsShown() {
        this.mHandler.removeCallbacks(this.mShowTimeout);
    }

    @Override
    public void onGlobalActionsDismissed() {
        this.mShowing = false;
    }

    @Override
    public void onGlobalActionsAvailableChanged(boolean z) {
        this.mGlobalActionsAvailable = z;
        if (this.mShowing && !this.mGlobalActionsAvailable) {
            ensureLegacyCreated();
            this.mLegacyGlobalActions.showDialog(this.mKeyguardShowing, this.mDeviceProvisioned);
        }
    }
}
