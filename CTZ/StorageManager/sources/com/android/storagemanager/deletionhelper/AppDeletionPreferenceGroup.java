package com.android.storagemanager.deletionhelper;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.format.Formatter;
import android.util.AttributeSet;
import com.android.internal.logging.MetricsLogger;
import com.android.storagemanager.R;
import com.android.storagemanager.deletionhelper.AppDeletionType;
import com.android.storagemanager.deletionhelper.AppsAsyncLoader;
import com.android.storagemanager.utils.PreferenceListCache;
import java.util.List;

public class AppDeletionPreferenceGroup extends CollapsibleCheckboxPreferenceGroup implements Preference.OnPreferenceChangeListener, AppDeletionType.AppListener {
    private AppDeletionType mBackend;
    PreferenceScreen mScreen;

    public AppDeletionPreferenceGroup(Context context) {
        this(context, null);
    }

    public AppDeletionPreferenceGroup(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setOnPreferenceChangeListener(this);
        updateText();
    }

    @Override
    public void onAppRebuild(List<AppsAsyncLoader.PackageInfo> list) {
        int size = list.size();
        int userId = getContext().getUserId();
        PreferenceListCache preferenceListCache = new PreferenceListCache(this);
        for (int i = 0; i < size; i++) {
            AppsAsyncLoader.PackageInfo packageInfo = list.get(i);
            if (packageInfo.userId == userId) {
                String str = packageInfo.packageName;
                AppDeletionPreference appDeletionPreference = (AppDeletionPreference) preferenceListCache.getCachedPreference(str);
                if (appDeletionPreference == null) {
                    appDeletionPreference = new AppDeletionPreference(getContext(), packageInfo);
                    appDeletionPreference.setKey(str);
                    appDeletionPreference.setOnPreferenceChangeListener(this);
                }
                addThresholdDependentPreference(appDeletionPreference, isNoThreshold());
                appDeletionPreference.setChecked(this.mBackend.isChecked(str));
                appDeletionPreference.setOrder(i + 100);
                appDeletionPreference.updateSummary();
            }
        }
        preferenceListCache.removeCachedPrefs();
        updateText();
    }

    private void addThresholdDependentPreference(AppDeletionPreference appDeletionPreference, boolean z) {
        if (isNoThreshold()) {
            addPreferenceToScreen(appDeletionPreference);
        } else {
            addPreference(appDeletionPreference);
        }
    }

    private boolean isNoThreshold() {
        return this.mBackend.getDeletionThreshold() == 0;
    }

    void addPreferenceToScreen(AppDeletionPreference appDeletionPreference) {
        if (this.mScreen == null) {
            this.mScreen = getPreferenceManager().getPreferenceScreen();
        }
        this.mScreen.addPreference(appDeletionPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        if (this.mBackend == null) {
            return true;
        }
        if (preference == this) {
            for (int i = 0; i < getPreferenceCount(); i++) {
                AppDeletionPreference appDeletionPreference = (AppDeletionPreference) getPreference(i);
                appDeletionPreference.setOnPreferenceChangeListener(null);
                appDeletionPreference.setChecked(zBooleanValue);
                this.mBackend.setChecked(appDeletionPreference.getPackageName(), zBooleanValue);
                appDeletionPreference.setOnPreferenceChangeListener(this);
            }
            updateText();
            MetricsLogger.action(getContext(), 461, zBooleanValue);
            return true;
        }
        AppDeletionPreference appDeletionPreference2 = (AppDeletionPreference) preference;
        this.mBackend.setChecked(appDeletionPreference2.getPackageName(), zBooleanValue);
        logAppToggle(zBooleanValue, appDeletionPreference2.getPackageName());
        updateText();
        return true;
    }

    @Override
    public void onClick() {
        super.onClick();
        MetricsLogger.action(getContext(), 464, isCollapsed());
    }

    public void setDeletionType(AppDeletionType appDeletionType) {
        this.mBackend = appDeletionType;
    }

    private void updateText() {
        long totalAppsFreeableSpace;
        long deletionThreshold;
        if (this.mBackend != null) {
            totalAppsFreeableSpace = this.mBackend.getTotalAppsFreeableSpace(true);
            deletionThreshold = this.mBackend.getDeletionThreshold();
            switchSpinnerToCheckboxOrDisablePreference(totalAppsFreeableSpace, this.mBackend.getLoadingStatus());
        } else {
            totalAppsFreeableSpace = 0;
            deletionThreshold = 90;
        }
        Context context = getContext();
        setTitle(context.getString(R.string.deletion_helper_apps_group_title));
        setSummary(context.getString(R.string.deletion_helper_apps_group_summary, Formatter.formatFileSize(context, totalAppsFreeableSpace), Long.valueOf(deletionThreshold)));
    }

    private void logAppToggle(boolean z, String str) {
        if (z) {
            MetricsLogger.action(getContext(), 462, str);
        } else {
            MetricsLogger.action(getContext(), 463, str);
        }
    }
}
