package com.android.settings.notification;

import android.content.Context;
import com.android.settings.R;

public class NotificationRingtonePreferenceController extends RingtonePreferenceControllerBase {
    public NotificationRingtonePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_notification_ringtone);
    }

    @Override
    public String getPreferenceKey() {
        return "notification_ringtone";
    }

    @Override
    public int getRingtoneType() {
        return 2;
    }
}
