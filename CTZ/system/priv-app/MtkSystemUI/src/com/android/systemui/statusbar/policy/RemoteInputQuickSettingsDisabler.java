package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.Configuration;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.ConfigurationController;

/* loaded from: classes.dex */
public class RemoteInputQuickSettingsDisabler implements ConfigurationController.ConfigurationListener {

    @VisibleForTesting
    CommandQueue mCommandQueue;
    private Context mContext;
    private int mLastOrientation;

    @VisibleForTesting
    boolean mRemoteInputActive;

    @VisibleForTesting
    boolean misLandscape;

    public RemoteInputQuickSettingsDisabler(Context context) {
        this.mContext = context;
        this.mCommandQueue = (CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class);
        this.mLastOrientation = this.mContext.getResources().getConfiguration().orientation;
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    public int adjustDisableFlags(int i) {
        if (this.mRemoteInputActive && this.misLandscape) {
            return i | 1;
        }
        return i;
    }

    public void setRemoteInputActive(boolean z) {
        if (this.mRemoteInputActive != z) {
            this.mRemoteInputActive = z;
            recomputeDisableFlags();
        }
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onConfigChanged(Configuration configuration) {
        if (configuration.orientation != this.mLastOrientation) {
            this.misLandscape = configuration.orientation == 2;
            this.mLastOrientation = configuration.orientation;
            recomputeDisableFlags();
        }
    }

    private void recomputeDisableFlags() {
        this.mCommandQueue.recomputeDisableFlags(true);
    }
}
