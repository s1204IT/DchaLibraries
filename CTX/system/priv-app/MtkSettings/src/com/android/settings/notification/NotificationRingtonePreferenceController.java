package com.android.settings.notification;

import android.content.Context;
import com.android.settings.R;
/* loaded from: classes.dex */
public class NotificationRingtonePreferenceController extends RingtonePreferenceControllerBase {
    public NotificationRingtonePreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settings.notification.RingtonePreferenceControllerBase, com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_notification_ringtone);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "notification_ringtone";
    }

    @Override // com.android.settings.notification.RingtonePreferenceControllerBase
    public int getRingtoneType() {
        return 2;
    }
}
