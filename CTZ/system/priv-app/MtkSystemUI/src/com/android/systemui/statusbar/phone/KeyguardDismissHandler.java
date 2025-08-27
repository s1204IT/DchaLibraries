package com.android.systemui.statusbar.phone;

import com.android.keyguard.KeyguardHostView;

/* loaded from: classes.dex */
public interface KeyguardDismissHandler {
    void executeWhenUnlocked(KeyguardHostView.OnDismissAction onDismissAction);
}
