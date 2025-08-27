package com.mediatek.server;

import com.mediatek.server.BenesseExtensionService;

/* compiled from: lambda */
/* renamed from: com.mediatek.server.-$$Lambda$BenesseExtensionService$erbcCrbZOhYH-JEcBSKtqZ9g-84 */
/* loaded from: classes.dex */
public final /* synthetic */ class lambda2 implements Runnable {
    private final /* synthetic */ BenesseExtensionService.UpdateParams f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ lambda2(BenesseExtensionService.UpdateParams updateParams, int i) {
        updateParams = updateParams;
        iWaitFor = i;
    }

    @Override // java.lang.Runnable
    public final void run() {
        BenesseExtensionService.lambda$executeFwUpdate$0(this.f$0, updateParams, iWaitFor);
    }
}