package com.android.settings.network;

import android.content.Context;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class NetworkResetPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final NetworkResetRestrictionChecker mRestrictionChecker;

    public NetworkResetPreferenceController(Context context) {
        super(context);
        this.mRestrictionChecker = new NetworkResetRestrictionChecker(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !this.mRestrictionChecker.hasUserRestriction();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "network_reset_pref";
    }
}
