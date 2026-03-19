package com.android.deskclock.ringtone;

import android.net.Uri;
import com.android.deskclock.R;

final class SystemRingtoneHolder extends RingtoneHolder {
    SystemRingtoneHolder(Uri uri, String str) {
        super(uri, str);
    }

    @Override
    public int getItemViewType() {
        return R.layout.ringtone_item_sound;
    }
}
