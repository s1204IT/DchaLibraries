package com.android.deskclock.ringtone;

import android.net.Uri;
import com.android.deskclock.ItemAdapter;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;

abstract class RingtoneHolder extends ItemAdapter.ItemHolder<Uri> {
    private final boolean mHasPermissions;
    private final String mName;
    private boolean mPlaying;
    private boolean mSelected;

    RingtoneHolder(Uri uri, String str) {
        this(uri, str, true);
    }

    RingtoneHolder(Uri uri, String str, boolean z) {
        super(uri, -1L);
        this.mName = str;
        this.mHasPermissions = z;
    }

    long getId() {
        return this.itemId;
    }

    boolean hasPermissions() {
        return this.mHasPermissions;
    }

    Uri getUri() {
        return (Uri) this.item;
    }

    boolean isSilent() {
        return Utils.RINGTONE_SILENT.equals(getUri());
    }

    boolean isSelected() {
        return this.mSelected;
    }

    void setSelected(boolean z) {
        this.mSelected = z;
    }

    boolean isPlaying() {
        return this.mPlaying;
    }

    void setPlaying(boolean z) {
        this.mPlaying = z;
    }

    String getName() {
        return this.mName != null ? this.mName : DataModel.getDataModel().getRingtoneTitle(getUri());
    }
}
