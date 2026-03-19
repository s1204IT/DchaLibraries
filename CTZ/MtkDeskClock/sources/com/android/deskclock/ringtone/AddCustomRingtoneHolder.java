package com.android.deskclock.ringtone;

import android.net.Uri;
import com.android.deskclock.ItemAdapter;

final class AddCustomRingtoneHolder extends ItemAdapter.ItemHolder<Uri> {
    AddCustomRingtoneHolder() {
        super(null, -1L);
    }

    @Override
    public int getItemViewType() {
        return Integer.MIN_VALUE;
    }
}
