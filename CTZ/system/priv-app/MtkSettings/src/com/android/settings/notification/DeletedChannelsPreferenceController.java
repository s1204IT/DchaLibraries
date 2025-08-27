package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;

/* loaded from: classes.dex */
public class DeletedChannelsPreferenceController extends NotificationPreferenceController implements PreferenceControllerMixin {
    public DeletedChannelsPreferenceController(Context context, NotificationBackend notificationBackend) {
        super(context, notificationBackend);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "deleted";
    }

    @Override // com.android.settings.notification.NotificationPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return super.isAvailable() && this.mChannel == null && !hasValidGroup() && this.mBackend.getDeletedChannelCount(this.mAppRow.pkg, this.mAppRow.uid) > 0;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (this.mAppRow != null) {
            int deletedChannelCount = this.mBackend.getDeletedChannelCount(this.mAppRow.pkg, this.mAppRow.uid);
            preference.setTitle(this.mContext.getResources().getQuantityString(R.plurals.deleted_channels, deletedChannelCount, Integer.valueOf(deletedChannelCount)));
        }
        preference.setSelectable(false);
    }
}
