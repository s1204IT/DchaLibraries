package com.android.systemui.statusbar.phone;

import com.android.keyguard.KeyguardHostView;

public interface KeyguardDismissHandler {
    void executeWhenUnlocked(KeyguardHostView.OnDismissAction onDismissAction);
}
