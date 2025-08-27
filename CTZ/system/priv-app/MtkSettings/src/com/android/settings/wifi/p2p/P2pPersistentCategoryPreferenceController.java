package com.android.settings.wifi.p2p;

import android.content.Context;

/* loaded from: classes.dex */
public class P2pPersistentCategoryPreferenceController extends P2pCategoryPreferenceController {
    public P2pPersistentCategoryPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "p2p_persistent_group";
    }
}
