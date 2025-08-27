package com.android.settings.password;

import com.android.settings.password.ConfirmLockPassword;

/* compiled from: lambda */
/* renamed from: com.android.settings.password.-$$Lambda$ConfirmLockPassword$ConfirmLockPasswordFragment$Myp25CGN_sn9Gs6wDwuZ61aKfg8, reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$ConfirmLockPassword$ConfirmLockPasswordFragment$Myp25CGN_sn9Gs6wDwuZ61aKfg8 implements Runnable {
    private final /* synthetic */ ConfirmLockPassword.ConfirmLockPasswordFragment f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.settings.password.ConfirmLockPassword.ConfirmLockPasswordFragment.onWindowFocusChanged(boolean):void, com.android.settings.password.ConfirmLockPassword.ConfirmLockPasswordFragment.startEnterAnimation():void] */
    public /* synthetic */ $$Lambda$ConfirmLockPassword$ConfirmLockPasswordFragment$Myp25CGN_sn9Gs6wDwuZ61aKfg8(ConfirmLockPassword.ConfirmLockPasswordFragment confirmLockPasswordFragment) {
        this.f$0 = confirmLockPasswordFragment;
    }

    /* JADX DEBUG: Class process forced to load method for inline: com.android.settings.password.ConfirmLockPassword.ConfirmLockPasswordFragment.lambda$Myp25CGN_sn9Gs6wDwuZ61aKfg8(com.android.settings.password.ConfirmLockPassword$ConfirmLockPasswordFragment):void */
    @Override // java.lang.Runnable
    public final void run() {
        this.f$0.updatePasswordEntry();
    }
}
