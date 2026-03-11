package com.android.systemui.statusbar.car;

import android.content.Context;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;

public class CarStatusBarKeyguardViewManager extends StatusBarKeyguardViewManager {
    protected boolean mShouldHideNavBar;

    public CarStatusBarKeyguardViewManager(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        super(context, viewMediatorCallback, lockPatternUtils);
        this.mShouldHideNavBar = context.getResources().getBoolean(R.bool.config_hideNavWhenKeyguardBouncerShown);
    }

    @Override
    protected void updateNavigationBarVisibility(boolean z) {
        if (!this.mShouldHideNavBar) {
            return;
        }
        ((CarStatusBar) this.mStatusBar).setNavBarVisibility(z ? 0 : 8);
    }

    @Override
    protected boolean shouldDestroyViewOnReset() {
        return true;
    }
}
