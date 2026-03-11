package com.android.settings.applications;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.Pair;
import android.view.View;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.applications.AppStateSmsPremBridge;
import com.android.settings.notification.EmptyTextSettings;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.widget.FooterPreference;
import java.util.ArrayList;

public class PremiumSmsAccess extends EmptyTextSettings implements Preference.OnPreferenceChangeListener, AppStateBaseBridge.Callback, ApplicationsState.Callbacks {
    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private AppStateSmsPremBridge mSmsBackend;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mApplicationsState = ApplicationsState.getInstance((Application) getContext().getApplicationContext());
        this.mSession = this.mApplicationsState.newSession(this, getLifecycle());
        this.mSmsBackend = new AppStateSmsPremBridge(getContext(), this.mApplicationsState, this);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        setLoading(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSmsBackend.resume();
    }

    @Override
    public void onPause() {
        this.mSmsBackend.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        this.mSmsBackend.release();
        super.onDestroy();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.premium_sms_settings;
    }

    @Override
    public int getMetricsCategory() {
        return 388;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        PremiumSmsPreference premiumSmsPreference = (PremiumSmsPreference) preference;
        int i = Integer.parseInt((String) obj);
        logSpecialPermissionChange(i, premiumSmsPreference.mAppEntry.info.packageName);
        this.mSmsBackend.setSmsState(premiumSmsPreference.mAppEntry.info.packageName, i);
        return true;
    }

    @VisibleForTesting
    void logSpecialPermissionChange(int i, String str) {
        int i2;
        switch (i) {
            case 1:
                i2 = 778;
                break;
            case 2:
                i2 = 779;
                break;
            case 3:
                i2 = 780;
                break;
            default:
                i2 = 0;
                break;
        }
        if (i2 != 0) {
            FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(), i2, str, new Pair[0]);
        }
    }

    private void updatePrefs(ArrayList<ApplicationsState.AppEntry> arrayList) {
        if (arrayList == null) {
            return;
        }
        setEmptyText(R.string.premium_sms_none);
        setLoading(false, true);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        preferenceScreen.setOrderingAsAdded(true);
        for (int i = 0; i < arrayList.size(); i++) {
            PremiumSmsPreference premiumSmsPreference = new PremiumSmsPreference(arrayList.get(i), getPrefContext());
            premiumSmsPreference.setOnPreferenceChangeListener(this);
            preferenceScreen.addPreference(premiumSmsPreference);
        }
        if (arrayList.size() != 0) {
            FooterPreference footerPreference = new FooterPreference(getPrefContext());
            footerPreference.setTitle(R.string.premium_sms_warning);
            preferenceScreen.addPreference(footerPreference);
        }
    }

    private void update() {
        updatePrefs(this.mSession.rebuild(AppStateSmsPremBridge.FILTER_APP_PREMIUM_SMS, ApplicationsState.ALPHA_COMPARATOR));
    }

    @Override
    public void onExtraInfoUpdated() {
        update();
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> arrayList) {
        updatePrefs(arrayList);
    }

    @Override
    public void onRunningStateChanged(boolean z) {
    }

    @Override
    public void onPackageListChanged() {
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String str) {
    }

    @Override
    public void onAllSizesComputed() {
    }

    @Override
    public void onLauncherInfoChanged() {
    }

    @Override
    public void onLoadEntriesCompleted() {
    }

    private class PremiumSmsPreference extends DropDownPreference {
        private final ApplicationsState.AppEntry mAppEntry;

        public PremiumSmsPreference(ApplicationsState.AppEntry appEntry, Context context) {
            super(context);
            this.mAppEntry = appEntry;
            this.mAppEntry.ensureLabel(context);
            setTitle(this.mAppEntry.label);
            if (this.mAppEntry.icon != null) {
                setIcon(this.mAppEntry.icon);
            }
            setEntries(R.array.security_settings_premium_sms_values);
            setEntryValues(new CharSequence[]{String.valueOf(1), String.valueOf(2), String.valueOf(3)});
            setValue(String.valueOf(getCurrentValue()));
            setSummary("%s");
        }

        private int getCurrentValue() {
            if (this.mAppEntry.extraInfo instanceof AppStateSmsPremBridge.SmsState) {
                return ((AppStateSmsPremBridge.SmsState) this.mAppEntry.extraInfo).smsState;
            }
            return 0;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
            if (getIcon() == null) {
                preferenceViewHolder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        PremiumSmsAccess.this.mApplicationsState.ensureIcon(PremiumSmsPreference.this.mAppEntry);
                        PremiumSmsPreference.this.setIcon(PremiumSmsPreference.this.mAppEntry.icon);
                    }
                });
            }
            super.onBindViewHolder(preferenceViewHolder);
        }
    }
}
