package com.android.settings.deviceinfo;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.Formatter;
import android.widget.ProgressBar;
import com.android.settings.R;

public class StorageItemPreference extends Preference {
    private int progress;
    private ProgressBar progressBar;
    public int userHandle;

    public StorageItemPreference(Context context) {
        super(context);
        this.progress = -1;
        setLayoutResource(R.layout.storage_item);
    }

    public void setStorageSize(long size, long total) {
        String fileSize;
        if (size == 0) {
            fileSize = String.valueOf(0);
        } else {
            fileSize = Formatter.formatFileSize(getContext(), size);
        }
        setSummary(fileSize);
        if (total == 0) {
            this.progress = 0;
        } else {
            this.progress = (int) ((100 * size) / total);
        }
        updateProgressBar();
    }

    protected void updateProgressBar() {
        if (this.progressBar == null) {
            return;
        }
        if (this.progress == -1) {
            this.progressBar.setVisibility(8);
            return;
        }
        this.progressBar.setVisibility(0);
        this.progressBar.setMax(100);
        this.progressBar.setProgress(this.progress);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        this.progressBar = (ProgressBar) view.findViewById(android.R.id.progress);
        updateProgressBar();
        super.onBindViewHolder(view);
    }
}
