package com.mediatek.server;

import com.mediatek.server.BenesseExtensionService;

/* compiled from: lambda */
/* renamed from: com.mediatek.server.-$$Lambda$BenesseExtensionService$DuLYMgReFex30dZ2dylIKOPJ6RA */
/* loaded from: classes.dex */
public final /* synthetic */ class lambda implements Runnable {
    private final /* synthetic */ BenesseExtensionService.UpdateParams f$1;

    /* JADX DEBUG: Method not inlined, still used in: [com.mediatek.server.BenesseExtensionService.executeFwUpdate(com.mediatek.server.BenesseExtensionService$UpdateParams):boolean] */
    public /* synthetic */ lambda(BenesseExtensionService.UpdateParams updateParams) {
        updateParams = updateParams;
    }

    @Override // java.lang.Runnable
    public final void run() {
        BenesseExtensionService.lambda$executeFwUpdate$1(this.f$0, updateParams);
    }
}