package com.android.systemui.statusbar.phone;

import android.util.Log;
import com.android.keyguard.KeyguardHostView;

/* loaded from: classes.dex */
public class KeyguardDismissUtil implements KeyguardDismissHandler {
    private volatile KeyguardDismissHandler mDismissHandler;

    public void setDismissHandler(KeyguardDismissHandler keyguardDismissHandler) {
        this.mDismissHandler = keyguardDismissHandler;
    }

    @Override // com.android.systemui.statusbar.phone.KeyguardDismissHandler
    public void executeWhenUnlocked(KeyguardHostView.OnDismissAction onDismissAction) {
        KeyguardDismissHandler keyguardDismissHandler = this.mDismissHandler;
        if (keyguardDismissHandler == null) {
            Log.wtf("KeyguardDismissUtil", "KeyguardDismissHandler not set.");
            onDismissAction.onDismiss();
        } else {
            keyguardDismissHandler.executeWhenUnlocked(onDismissAction);
        }
    }
}
