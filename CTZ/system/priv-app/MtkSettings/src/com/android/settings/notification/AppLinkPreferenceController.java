package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;

/* loaded from: classes.dex */
public class AppLinkPreferenceController extends NotificationPreferenceController implements PreferenceControllerMixin {
    public AppLinkPreferenceController(Context context) {
        super(context, null);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "app_link";
    }

    @Override // com.android.settings.notification.NotificationPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return super.isAvailable() && this.mAppRow.settingsIntent != null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (this.mAppRow != null) {
            preference.setIntent(this.mAppRow.settingsIntent);
        }
    }
}
