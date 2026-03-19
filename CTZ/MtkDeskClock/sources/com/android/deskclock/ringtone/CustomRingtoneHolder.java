package com.android.deskclock.ringtone;

import com.android.deskclock.data.CustomRingtone;

class CustomRingtoneHolder extends RingtoneHolder {
    CustomRingtoneHolder(CustomRingtone customRingtone) {
        super(customRingtone.getUri(), customRingtone.getTitle(), customRingtone.hasPermissions());
    }

    @Override
    public int getItemViewType() {
        return -2131558500;
    }
}
