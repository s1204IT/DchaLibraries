package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;

public class KeyguardSimpleHostView extends KeyguardViewBase {
    private KeyguardUpdateMonitorCallback mUpdateCallback;

    public KeyguardSimpleHostView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mUpdateCallback = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onUserSwitchComplete(int userId) {
                KeyguardSimpleHostView.this.getSecurityContainer().showPrimarySecurityScreen(false);
            }

            @Override
            public void onTrustInitiatedByUser(int userId) {
                if (userId == KeyguardSimpleHostView.this.mLockPatternUtils.getCurrentUser() && KeyguardSimpleHostView.this.isAttachedToWindow()) {
                    if (KeyguardSimpleHostView.this.isVisibleToUser()) {
                        KeyguardSimpleHostView.this.dismiss(false);
                    } else {
                        KeyguardSimpleHostView.this.mViewMediatorCallback.playTrustedSound();
                    }
                }
            }
        };
        KeyguardUpdateMonitor.getInstance(context).registerCallback(this.mUpdateCallback);
    }

    @Override
    public void cleanUp() {
        getSecurityContainer().onPause();
    }

    @Override
    public long getUserActivityTimeout() {
        return -1L;
    }
}
