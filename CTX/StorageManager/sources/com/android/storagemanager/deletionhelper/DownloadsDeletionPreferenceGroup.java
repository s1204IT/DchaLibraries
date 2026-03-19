package com.android.storagemanager.deletionhelper;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.preference.Preference;
import android.text.format.Formatter;
import android.util.AttributeSet;
import com.android.internal.logging.MetricsLogger;
import com.android.storagemanager.R;
import com.android.storagemanager.deletionhelper.DeletionType;
import com.android.storagemanager.utils.IconProvider;
import com.android.storagemanager.utils.PreferenceListCache;
import java.io.File;
import java.util.Set;

public class DownloadsDeletionPreferenceGroup extends CollapsibleCheckboxPreferenceGroup implements Preference.OnPreferenceChangeListener, DeletionType.FreeableChangedListener {
    private DownloadsDeletionType mDeletionType;
    private IconProvider mIconProvider;
    private DeletionType.FreeableChangedListener mListener;

    public DownloadsDeletionPreferenceGroup(Context context) {
        super(context);
        init();
    }

    public DownloadsDeletionPreferenceGroup(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    private void init() {
        setChecked(true);
        setOrderingAsAdded(false);
        setOnPreferenceChangeListener(this);
    }

    public void registerDeletionService(DownloadsDeletionType downloadsDeletionType) {
        this.mDeletionType = downloadsDeletionType;
        this.mDeletionType.registerFreeableChangedListener(this);
    }

    public void registerFreeableChangedListener(DeletionType.FreeableChangedListener freeableChangedListener) {
        this.mListener = freeableChangedListener;
    }

    @Override
    public void onFreeableChanged(int i, long j) {
        updatePreferenceText(i, j, this.mDeletionType.getMostRecentLastModified());
        maybeUpdateListener(i, j);
        switchSpinnerToCheckboxOrDisablePreference(j, this.mDeletionType.getLoadingStatus());
        updateFiles();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        if (!zBooleanValue) {
            setOnPreferenceChangeListener(null);
            setChecked(false);
            setOnPreferenceChangeListener(this);
        }
        if (this.mDeletionType == null) {
            return true;
        }
        if (preference == this) {
            for (int i = 0; i < getPreferenceCount(); i++) {
                DownloadsFilePreference downloadsFilePreference = (DownloadsFilePreference) getPreference(i);
                downloadsFilePreference.setOnPreferenceChangeListener(null);
                this.mDeletionType.setFileChecked(downloadsFilePreference.getFile(), zBooleanValue);
                downloadsFilePreference.setChecked(zBooleanValue);
                downloadsFilePreference.setOnPreferenceChangeListener(this);
            }
            maybeUpdateListener(this.mDeletionType.getFiles().size(), this.mDeletionType.getFreeableBytes(false));
            MetricsLogger.action(getContext(), 465, zBooleanValue);
            return true;
        }
        this.mDeletionType.setFileChecked(((DownloadsFilePreference) preference).getFile(), zBooleanValue);
        maybeUpdateListener(this.mDeletionType.getFiles().size(), this.mDeletionType.getFreeableBytes(false));
        return true;
    }

    @Override
    public void onClick() {
        super.onClick();
        MetricsLogger.action(getContext(), 466, isCollapsed());
    }

    void injectIconProvider(IconProvider iconProvider) {
        this.mIconProvider = iconProvider;
    }

    private void updatePreferenceText(int i, long j, long j2) {
        Context context = getContext();
        setTitle(context.getString(R.string.deletion_helper_downloads_title));
        if (i != 0) {
            setSummary(context.getString(R.string.deletion_helper_downloads_category_summary, Formatter.formatFileSize(context, j)));
        } else {
            setSummary(context.getString(R.string.deletion_helper_downloads_summary_empty, Formatter.formatFileSize(context, j)));
        }
    }

    private void maybeUpdateListener(int i, long j) {
        if (this.mListener != null) {
            this.mListener.onFreeableChanged(i, j);
        }
    }

    private void updateFiles() {
        PreferenceListCache preferenceListCache = new PreferenceListCache(this);
        Set<File> files = this.mDeletionType.getFiles();
        Context context = getContext();
        Resources resources = context.getResources();
        IconProvider iconProvider = this.mIconProvider == null ? new IconProvider(context) : this.mIconProvider;
        for (File file : files) {
            DownloadsFilePreference downloadsFilePreference = (DownloadsFilePreference) preferenceListCache.getCachedPreference(file.getPath());
            if (downloadsFilePreference == null) {
                downloadsFilePreference = new DownloadsFilePreference(context, file, iconProvider);
                downloadsFilePreference.setChecked(this.mDeletionType.isChecked(file));
                downloadsFilePreference.setOnPreferenceChangeListener(this);
                Bitmap cachedThumbnail = this.mDeletionType.getCachedThumbnail(file);
                if (cachedThumbnail != null) {
                    downloadsFilePreference.setIcon(new BitmapDrawable(resources, cachedThumbnail));
                }
            }
            addPreference(downloadsFilePreference);
        }
        preferenceListCache.removeCachedPrefs();
    }
}
