package com.android.settings.applications;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;
import com.android.settings.DividerPreference;
import com.android.settings.R;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.applications.AppStateSmsPremBridge;
import com.android.settings.notification.EmptyTextSettings;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;

public class PremiumSmsAccess extends EmptyTextSettings implements AppStateBaseBridge.Callback, ApplicationsState.Callbacks, Preference.OnPreferenceChangeListener {
    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private AppStateSmsPremBridge mSmsBackend;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mApplicationsState = ApplicationsState.getInstance((Application) getContext().getApplicationContext());
        this.mSession = this.mApplicationsState.newSession(this);
        this.mSmsBackend = new AppStateSmsPremBridge(getContext(), this.mApplicationsState, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setLoading(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSession.resume();
        this.mSmsBackend.resume();
    }

    @Override
    public void onPause() {
        this.mSmsBackend.pause();
        this.mSession.pause();
        super.onPause();
    }

    @Override
    protected int getMetricsCategory() {
        return 388;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        PremiumSmsPreference pref = (PremiumSmsPreference) preference;
        this.mSmsBackend.setSmsState(pref.mAppEntry.info.packageName, Integer.parseInt((String) newValue));
        return true;
    }

    private void updatePrefs(ArrayList<ApplicationsState.AppEntry> apps) {
        if (apps == null) {
            return;
        }
        setEmptyText(R.string.premium_sms_none);
        setLoading(false, true);
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getPrefContext());
        screen.setOrderingAsAdded(true);
        for (int i = 0; i < apps.size(); i++) {
            Preference smsPreference = new PremiumSmsPreference(apps.get(i), getPrefContext());
            smsPreference.setOnPreferenceChangeListener(this);
            screen.addPreference(smsPreference);
        }
        if (apps.size() != 0) {
            DividerPreference summary = new DividerPreference(getPrefContext());
            summary.setSelectable(false);
            summary.setSummary(R.string.premium_sms_warning);
            summary.setDividerAllowedAbove(true);
            screen.addPreference(summary);
        }
        setPreferenceScreen(screen);
    }

    private void update() {
        updatePrefs(this.mSession.rebuild(AppStateSmsPremBridge.FILTER_APP_PREMIUM_SMS, ApplicationsState.ALPHA_COMPARATOR));
    }

    @Override
    public void onExtraInfoUpdated() {
        update();
    }

    @Override
    public void m683xf40fefa9(ArrayList<ApplicationsState.AppEntry> apps) {
        updatePrefs(apps);
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    @Override
    public void onPackageListChanged() {
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
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
        public void onBindViewHolder(PreferenceViewHolder holder) {
            if (getIcon() == null) {
                holder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        PremiumSmsAccess.this.mApplicationsState.ensureIcon(PremiumSmsPreference.this.mAppEntry);
                        PremiumSmsPreference.this.setIcon(PremiumSmsPreference.this.mAppEntry.icon);
                    }
                });
            }
            super.onBindViewHolder(holder);
        }
    }
}
