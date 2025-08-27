package com.android.settings.utils;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.View;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.notification.EmptyTextSettings;
import com.android.settings.utils.ManagedServiceSettings;
import com.android.settings.widget.AppSwitchPreference;
import com.android.settingslib.applications.ServiceListing;
import java.util.List;

/* loaded from: classes.dex */
public abstract class ManagedServiceSettings extends EmptyTextSettings {
    private final Config mConfig = getConfig();
    protected Context mContext;
    private DevicePolicyManager mDpm;
    private IconDrawableFactory mIconDrawableFactory;
    private PackageManager mPm;
    private ServiceListing mServiceListing;

    protected abstract Config getConfig();

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mContext = getActivity();
        this.mPm = this.mContext.getPackageManager();
        this.mDpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        this.mIconDrawableFactory = IconDrawableFactory.newInstance(this.mContext);
        this.mServiceListing = new ServiceListing.Builder(this.mContext).setPermission(this.mConfig.permission).setIntentAction(this.mConfig.intentAction).setNoun(this.mConfig.noun).setSetting(this.mConfig.setting).setTag(this.mConfig.tag).build();
        this.mServiceListing.addCallback(new ServiceListing.Callback() { // from class: com.android.settings.utils.-$$Lambda$ManagedServiceSettings$6gJSYmD-m4iGVFUdlUroaoAptMw
            @Override // com.android.settingslib.applications.ServiceListing.Callback
            public final void onServicesReloaded(List list) {
                this.f$0.updateList(list);
            }
        });
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(this.mContext));
    }

    @Override // com.android.settings.notification.EmptyTextSettings, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        setEmptyText(this.mConfig.emptyText);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        if (!ActivityManager.isLowRamDeviceStatic()) {
            this.mServiceListing.reload();
            this.mServiceListing.setListening(true);
        } else {
            setEmptyText(R.string.disabled_low_ram_device);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mServiceListing.setListening(false);
    }

    private void updateList(List<ServiceInfo> list) {
        int managedProfileId = Utils.getManagedProfileId((UserManager) this.mContext.getSystemService("user"), UserHandle.myUserId());
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        list.sort(new PackageItemInfo.DisplayNameComparator(this.mPm));
        for (ServiceInfo serviceInfo : list) {
            final ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
            CharSequence charSequenceLoadLabel = null;
            try {
                charSequenceLoadLabel = this.mPm.getApplicationInfoAsUser(serviceInfo.packageName, 0, getCurrentUser(managedProfileId)).loadLabel(this.mPm);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("ManagedServiceSettings", "can't find package name", e);
            }
            final String string = serviceInfo.loadLabel(this.mPm).toString();
            AppSwitchPreference appSwitchPreference = new AppSwitchPreference(getPrefContext());
            appSwitchPreference.setPersistent(false);
            appSwitchPreference.setIcon(this.mIconDrawableFactory.getBadgedIcon(serviceInfo, serviceInfo.applicationInfo, UserHandle.getUserId(serviceInfo.applicationInfo.uid)));
            if (charSequenceLoadLabel != null && !charSequenceLoadLabel.equals(string)) {
                appSwitchPreference.setTitle(charSequenceLoadLabel);
                appSwitchPreference.setSummary(string);
            } else {
                appSwitchPreference.setTitle(string);
            }
            appSwitchPreference.setKey(componentName.flattenToString());
            appSwitchPreference.setChecked(isServiceEnabled(componentName));
            if (managedProfileId != -10000 && !this.mDpm.isNotificationListenerServicePermitted(serviceInfo.packageName, managedProfileId)) {
                appSwitchPreference.setSummary(R.string.work_profile_notification_access_blocked_summary);
            }
            appSwitchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.android.settings.utils.-$$Lambda$ManagedServiceSettings$qzumG4qfCDX22E2-mvpKDzSZyck
                @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
                public final boolean onPreferenceChange(Preference preference, Object obj) {
                    return this.f$0.setEnabled(componentName, string, ((Boolean) obj).booleanValue());
                }
            });
            appSwitchPreference.setKey(componentName.flattenToString());
            preferenceScreen.addPreference(appSwitchPreference);
        }
        highlightPreferenceIfNeeded();
    }

    private int getCurrentUser(int i) {
        if (i != -10000) {
            return i;
        }
        return UserHandle.myUserId();
    }

    protected boolean isServiceEnabled(ComponentName componentName) {
        return this.mServiceListing.isEnabled(componentName);
    }

    protected boolean setEnabled(ComponentName componentName, String str, boolean z) {
        if (!z) {
            this.mServiceListing.setEnabled(componentName, false);
            return true;
        }
        if (this.mServiceListing.isEnabled(componentName)) {
            return true;
        }
        new ScaryWarningDialogFragment().setServiceInfo(componentName, str, this).show(getFragmentManager(), "dialog");
        return false;
    }

    protected void enable(ComponentName componentName) {
        this.mServiceListing.setEnabled(componentName, true);
    }

    public static class ScaryWarningDialogFragment extends InstrumentedDialogFragment {
        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 557;
        }

        public ScaryWarningDialogFragment setServiceInfo(ComponentName componentName, String str, Fragment fragment) {
            Bundle bundle = new Bundle();
            bundle.putString("c", componentName.flattenToString());
            bundle.putString("l", str);
            setArguments(bundle);
            setTargetFragment(fragment, 0);
            return this;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            Bundle arguments = getArguments();
            String string = arguments.getString("l");
            final ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(arguments.getString("c"));
            final ManagedServiceSettings managedServiceSettings = (ManagedServiceSettings) getTargetFragment();
            return new AlertDialog.Builder(getContext()).setMessage(getResources().getString(managedServiceSettings.mConfig.warningDialogSummary, string)).setTitle(getResources().getString(managedServiceSettings.mConfig.warningDialogTitle, string)).setCancelable(true).setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() { // from class: com.android.settings.utils.-$$Lambda$ManagedServiceSettings$ScaryWarningDialogFragment$GfuRaJIB12V_MS8RLGOsdgpO8G0
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    managedServiceSettings.enable(componentNameUnflattenFromString);
                }
            }).setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() { // from class: com.android.settings.utils.-$$Lambda$ManagedServiceSettings$ScaryWarningDialogFragment$zGrX-jMl8gPwJu7rfyhg512VL6Y
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    ManagedServiceSettings.ScaryWarningDialogFragment.lambda$onCreateDialog$1(dialogInterface, i);
                }
            }).create();
        }

        static /* synthetic */ void lambda$onCreateDialog$1(DialogInterface dialogInterface, int i) {
        }
    }

    public static class Config {
        public final int emptyText;
        public final String intentAction;
        public final String noun;
        public final String permission;
        public final String setting;
        public final String tag;
        public final int warningDialogSummary;
        public final int warningDialogTitle;

        private Config(String str, String str2, String str3, String str4, String str5, int i, int i2, int i3) {
            this.tag = str;
            this.setting = str2;
            this.intentAction = str3;
            this.permission = str4;
            this.noun = str5;
            this.warningDialogTitle = i;
            this.warningDialogSummary = i2;
            this.emptyText = i3;
        }

        public static class Builder {
            private int mEmptyText;
            private String mIntentAction;
            private String mNoun;
            private String mPermission;
            private String mSetting;
            private String mTag;
            private int mWarningDialogSummary;
            private int mWarningDialogTitle;

            public Builder setTag(String str) {
                this.mTag = str;
                return this;
            }

            public Builder setSetting(String str) {
                this.mSetting = str;
                return this;
            }

            public Builder setIntentAction(String str) {
                this.mIntentAction = str;
                return this;
            }

            public Builder setPermission(String str) {
                this.mPermission = str;
                return this;
            }

            public Builder setNoun(String str) {
                this.mNoun = str;
                return this;
            }

            public Builder setWarningDialogTitle(int i) {
                this.mWarningDialogTitle = i;
                return this;
            }

            public Builder setWarningDialogSummary(int i) {
                this.mWarningDialogSummary = i;
                return this;
            }

            public Builder setEmptyText(int i) {
                this.mEmptyText = i;
                return this;
            }

            public Config build() {
                return new Config(this.mTag, this.mSetting, this.mIntentAction, this.mPermission, this.mNoun, this.mWarningDialogTitle, this.mWarningDialogSummary, this.mEmptyText);
            }
        }
    }
}
