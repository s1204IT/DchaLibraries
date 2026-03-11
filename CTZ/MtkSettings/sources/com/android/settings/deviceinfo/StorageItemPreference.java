package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import com.android.settings.R;
import com.android.settings.utils.FileSizeFormatter;

public class StorageItemPreference extends Preference {
    private ProgressBar mProgressBar;
    private int mProgressPercent;
    public int userHandle;

    public StorageItemPreference(Context context) {
        this(context, null);
    }

    public StorageItemPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mProgressPercent = -1;
        setLayoutResource(R.layout.storage_item);
        setSummary(R.string.memory_calculating_size);
    }

    public void setStorageSize(long j, long j2) {
        setSummary(FileSizeFormatter.formatFileSize(getContext(), j, getGigabyteSuffix(getContext().getResources()), 1000000000L));
        if (j2 == 0) {
            this.mProgressPercent = 0;
        } else {
            this.mProgressPercent = (int) ((j * 100) / j2);
        }
        updateProgressBar();
    }

    protected void updateProgressBar() {
        if (this.mProgressBar == null || this.mProgressPercent == -1) {
            return;
        }
        this.mProgressBar.setMax(100);
        this.mProgressBar.setProgress(this.mProgressPercent);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        this.mProgressBar = (ProgressBar) preferenceViewHolder.findViewById(android.R.id.progress);
        updateProgressBar();
        super.onBindViewHolder(preferenceViewHolder);
    }

    private static int getGigabyteSuffix(Resources resources) {
        return resources.getIdentifier("gigabyteShort", "string", "android");
    }
}
