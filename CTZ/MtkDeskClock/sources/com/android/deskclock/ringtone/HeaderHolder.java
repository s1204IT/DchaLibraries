package com.android.deskclock.ringtone;

import android.net.Uri;
import android.support.annotation.StringRes;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.R;

final class HeaderHolder extends ItemAdapter.ItemHolder<Uri> {

    @StringRes
    private final int mTextResId;

    HeaderHolder(@StringRes int i) {
        super(null, -1L);
        this.mTextResId = i;
    }

    @StringRes
    int getTextResId() {
        return this.mTextResId;
    }

    @Override
    public int getItemViewType() {
        return R.layout.ringtone_item_header;
    }
}
