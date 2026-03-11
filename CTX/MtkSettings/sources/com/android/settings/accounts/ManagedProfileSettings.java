package com.android.settings.accounts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;

public class ManagedProfileSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private RestrictedSwitchPreference mContactPrefrence;
    private Context mContext;
    private ManagedProfileBroadcastReceiver mManagedProfileBroadcastReceiver;
    private UserHandle mManagedUser;
    private UserManager mUserManager;
    private SwitchPreference mWorkModePreference;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.managed_profile_settings);
        this.mWorkModePreference = (SwitchPreference) findPreference("work_mode");
        this.mWorkModePreference.setOnPreferenceChangeListener(this);
        this.mContactPrefrence = (RestrictedSwitchPreference) findPreference("contacts_search");
        this.mContactPrefrence.setOnPreferenceChangeListener(this);
        this.mContext = getActivity().getApplicationContext();
        this.mUserManager = (UserManager) getSystemService("user");
        this.mManagedUser = getManagedUserFromArgument();
        if (this.mManagedUser == null) {
            getActivity().finish();
        }
        this.mManagedProfileBroadcastReceiver = new ManagedProfileBroadcastReceiver();
        this.mManagedProfileBroadcastReceiver.register(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDataAndPopulateUi();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mManagedProfileBroadcastReceiver.unregister(getActivity());
    }

    private UserHandle getManagedUserFromArgument() {
        UserHandle userHandle;
        Bundle arguments = getArguments();
        if (arguments != null && (userHandle = (UserHandle) arguments.getParcelable("android.intent.extra.USER")) != null && this.mUserManager.isManagedProfile(userHandle.getIdentifier())) {
            return userHandle;
        }
        return Utils.getManagedProfile(this.mUserManager);
    }

    private void loadDataAndPopulateUi() {
        if (this.mWorkModePreference != null) {
            updateWorkModePreference();
        }
        if (this.mContactPrefrence != null) {
            this.mContactPrefrence.setChecked(Settings.Secure.getIntForUser(getContentResolver(), "managed_profile_contact_remote_search", 0, this.mManagedUser.getIdentifier()) != 0);
            this.mContactPrefrence.setDisabledByAdmin(RestrictedLockUtils.checkIfRemoteContactSearchDisallowed(this.mContext, this.mManagedUser.getIdentifier()));
        }
    }

    @Override
    public int getMetricsCategory() {
        return 401;
    }

    public void updateWorkModePreference() {
        int i;
        boolean z = !this.mUserManager.isQuietModeEnabled(this.mManagedUser);
        this.mWorkModePreference.setChecked(z);
        SwitchPreference switchPreference = this.mWorkModePreference;
        if (z) {
            i = R.string.work_mode_on_summary;
        } else {
            i = R.string.work_mode_off_summary;
        }
        switchPreference.setSummary(i);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mWorkModePreference) {
            this.mUserManager.requestQuietModeEnabled(!((Boolean) obj).booleanValue(), this.mManagedUser);
            return true;
        }
        if (preference != this.mContactPrefrence) {
            return false;
        }
        Settings.Secure.putIntForUser(getContentResolver(), "managed_profile_contact_remote_search", ((Boolean) obj).booleanValue() ? 1 : 0, this.mManagedUser.getIdentifier());
        return true;
    }

    private class ManagedProfileBroadcastReceiver extends BroadcastReceiver {
        private ManagedProfileBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v("ManagedProfileSettings", "Received broadcast: " + action);
            if (action.equals("android.intent.action.MANAGED_PROFILE_REMOVED")) {
                if (intent.getIntExtra("android.intent.extra.user_handle", -10000) == ManagedProfileSettings.this.mManagedUser.getIdentifier()) {
                    ManagedProfileSettings.this.getActivity().finish();
                }
            } else if (action.equals("android.intent.action.MANAGED_PROFILE_AVAILABLE") || action.equals("android.intent.action.MANAGED_PROFILE_UNAVAILABLE")) {
                if (intent.getIntExtra("android.intent.extra.user_handle", -10000) == ManagedProfileSettings.this.mManagedUser.getIdentifier()) {
                    ManagedProfileSettings.this.updateWorkModePreference();
                }
            } else {
                Log.w("ManagedProfileSettings", "Cannot handle received broadcast: " + intent.getAction());
            }
        }

        public void register(Context context) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
            intentFilter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
            intentFilter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
            context.registerReceiver(this, intentFilter);
        }

        public void unregister(Context context) {
            context.unregisterReceiver(this);
        }
    }
}
