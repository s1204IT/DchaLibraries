package com.android.storagemanager.deletionhelper;

import android.content.Context;
import android.os.Handler;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.Formatter;
import android.util.AttributeSet;
import com.android.internal.logging.MetricsLogger;
import com.android.storagemanager.R;

public class PhotosDeletionPreference extends DeletionPreference {
    private int mDaysToKeep;
    private boolean mLoaded;

    public PhotosDeletionPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDaysToKeep = 30;
        updatePreferenceText(0, 0L);
        setTitle(R.string.deletion_helper_photos_loading_title);
        setSummary(R.string.deletion_helper_photos_loading_summary);
    }

    public void updatePreferenceText(int i, long j) {
        Context context = getContext();
        setTitle(context.getString(R.string.deletion_helper_photos_title));
        this.mLoaded = true;
        setSummary(context.getString(R.string.deletion_helper_photos_summary, Formatter.formatFileSize(context, j), Integer.valueOf(this.mDaysToKeep)));
    }

    public void setDaysToKeep(int i) {
        this.mDaysToKeep = i;
        updatePreferenceText(0, 0L);
    }

    @Override
    public void onFreeableChanged(final int i, final long j) {
        new Handler(getContext().getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                PhotosDeletionPreference.super.onFreeableChanged(i, j);
                PhotosDeletionPreference.this.updatePreferenceText(i, j);
            }
        });
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        MetricsLogger.action(getContext(), 460, ((Boolean) obj).booleanValue());
        return super.onPreferenceChange(preference, obj);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        preferenceViewHolder.findViewById(android.R.id.icon).setVisibility(8);
    }
}
