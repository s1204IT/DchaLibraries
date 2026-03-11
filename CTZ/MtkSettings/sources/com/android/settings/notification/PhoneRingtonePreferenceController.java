package com.android.settings.notification;

import android.content.Context;
import com.android.settings.Utils;

public class PhoneRingtonePreferenceController extends RingtonePreferenceControllerBase {
    public PhoneRingtonePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "ringtone";
    }

    @Override
    public boolean isAvailable() {
        return Utils.isVoiceCapable(this.mContext);
    }

    @Override
    public int getRingtoneType() {
        return 1;
    }
}
