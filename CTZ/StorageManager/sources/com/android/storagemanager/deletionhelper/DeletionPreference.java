package com.android.storagemanager.deletionhelper;

import android.content.Context;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.storagemanager.R;
import com.android.storagemanager.deletionhelper.DeletionType;

public abstract class DeletionPreference extends CheckBoxPreference implements Preference.OnPreferenceChangeListener, DeletionType.FreeableChangedListener {
    private DeletionType mDeletionService;
    private long mFreeableBytes;
    private int mFreeableItems;
    private DeletionType.FreeableChangedListener mListener;
    private boolean mLoaded;
    private ProgressBar mProgressBar;
    private TextView mSummary;
    private View mWidget;

    public DeletionPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setLayoutResource(R.layout.deletion_preference);
        setOnPreferenceChangeListener(this);
        setPersistent(false);
    }

    public long getFreeableBytes(boolean z) {
        if (isChecked() || z) {
            return this.mFreeableBytes;
        }
        return 0L;
    }

    public void registerFreeableChangedListener(DeletionType.FreeableChangedListener freeableChangedListener) {
        this.mListener = freeableChangedListener;
    }

    public void registerDeletionService(DeletionType deletionType) {
        this.mDeletionService = deletionType;
        if (this.mDeletionService != null) {
            this.mDeletionService.registerFreeableChangedListener(this);
        }
    }

    @Override
    public void onFreeableChanged(int i, long j) {
        this.mFreeableItems = i;
        this.mFreeableBytes = j;
        this.mLoaded = true;
        if (this.mDeletionService != null) {
            setEnabled(true ^ this.mDeletionService.isEmpty());
        }
        if (!isEnabled()) {
            setChecked(false);
        }
        if (this.mWidget != null) {
            this.mWidget.setVisibility(0);
        }
        if (this.mProgressBar != null) {
            this.mProgressBar.setVisibility(8);
        }
        maybeUpdateListener();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        this.mSummary = (TextView) preferenceViewHolder.findViewById(android.R.id.summary);
        this.mProgressBar = (ProgressBar) preferenceViewHolder.findViewById(R.id.progress_bar);
        this.mProgressBar.setVisibility(this.mLoaded ? 8 : 0);
        this.mWidget = preferenceViewHolder.findViewById(android.R.id.widget_frame);
        this.mWidget.setVisibility(this.mLoaded ? 0 : 8);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Boolean bool = (Boolean) obj;
        setChecked(bool.booleanValue());
        this.mSummary.setActivated(bool.booleanValue());
        maybeUpdateListener();
        return true;
    }

    private void maybeUpdateListener() {
        if (this.mListener != null) {
            this.mListener.onFreeableChanged(this.mFreeableItems, getFreeableBytes(false));
        }
    }
}
