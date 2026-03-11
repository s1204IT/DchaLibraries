package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.notification.RestrictedDropDownPreference;
import com.android.settingslib.RestrictedLockUtils;
import java.util.ArrayList;

public class ConfigureNotificationSettings extends SettingsPreferenceFragment {
    private Context mContext;
    private RestrictedDropDownPreference mLockscreen;
    private RestrictedDropDownPreference mLockscreenProfile;
    private int mLockscreenSelectedValue;
    private int mLockscreenSelectedValueProfile;
    private TwoStatePreference mNotificationPulse;
    private int mProfileChallengeUserId;
    private boolean mSecure;
    private boolean mSecureProfile;
    private final SettingsObserver mSettingsObserver = new SettingsObserver();

    @Override
    protected int getMetricsCategory() {
        return 337;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        boolean z = false;
        super.onCreate(savedInstanceState);
        this.mContext = getActivity();
        this.mProfileChallengeUserId = Utils.getManagedProfileId(UserManager.get(this.mContext), UserHandle.myUserId());
        LockPatternUtils utils = new LockPatternUtils(getActivity());
        boolean isUnified = !utils.isSeparateProfileChallengeEnabled(this.mProfileChallengeUserId);
        this.mSecure = utils.isSecure(UserHandle.myUserId());
        if (this.mProfileChallengeUserId != -10000) {
            if (utils.isSecure(this.mProfileChallengeUserId)) {
                z = true;
            } else if (isUnified) {
                z = this.mSecure;
            }
        }
        this.mSecureProfile = z;
        addPreferencesFromResource(R.xml.configure_notification_settings);
        initPulse();
        initLockscreenNotifications();
        if (this.mProfileChallengeUserId == -10000) {
            return;
        }
        addPreferencesFromResource(R.xml.configure_notification_settings_profile);
        initLockscreenNotificationsForProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSettingsObserver.register(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mSettingsObserver.register(false);
    }

    private void initPulse() {
        this.mNotificationPulse = (TwoStatePreference) getPreferenceScreen().findPreference("notification_pulse");
        if (this.mNotificationPulse == null) {
            Log.i("ConfigNotiSettings", "Preference not found: notification_pulse");
        } else if (!getResources().getBoolean(android.R.^attr-private.dreamActivityCloseExitAnimation)) {
            getPreferenceScreen().removePreference(this.mNotificationPulse);
        } else {
            updatePulse();
            this.mNotificationPulse.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean val = ((Boolean) newValue).booleanValue();
                    return Settings.System.putInt(ConfigureNotificationSettings.this.getContentResolver(), "notification_light_pulse", val ? 1 : 0);
                }
            });
        }
    }

    public void updatePulse() {
        if (this.mNotificationPulse == null) {
            return;
        }
        try {
            this.mNotificationPulse.setChecked(Settings.System.getInt(getContentResolver(), "notification_light_pulse") == 1);
        } catch (Settings.SettingNotFoundException e) {
            Log.e("ConfigNotiSettings", "notification_light_pulse not found");
        }
    }

    private void initLockscreenNotifications() {
        this.mLockscreen = (RestrictedDropDownPreference) getPreferenceScreen().findPreference("lock_screen_notifications");
        if (this.mLockscreen == null) {
            Log.i("ConfigNotiSettings", "Preference not found: lock_screen_notifications");
            return;
        }
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        entries.add(getString(R.string.lock_screen_notifications_summary_disable));
        values.add(Integer.toString(R.string.lock_screen_notifications_summary_disable));
        String summaryShowEntry = getString(R.string.lock_screen_notifications_summary_show);
        String summaryShowEntryValue = Integer.toString(R.string.lock_screen_notifications_summary_show);
        entries.add(summaryShowEntry);
        values.add(summaryShowEntryValue);
        setRestrictedIfNotificationFeaturesDisabled(summaryShowEntry, summaryShowEntryValue, 12);
        if (this.mSecure) {
            String summaryHideEntry = getString(R.string.lock_screen_notifications_summary_hide);
            String summaryHideEntryValue = Integer.toString(R.string.lock_screen_notifications_summary_hide);
            entries.add(summaryHideEntry);
            values.add(summaryHideEntryValue);
            setRestrictedIfNotificationFeaturesDisabled(summaryHideEntry, summaryHideEntryValue, 4);
        }
        this.mLockscreen.setEntries((CharSequence[]) entries.toArray(new CharSequence[entries.size()]));
        this.mLockscreen.setEntryValues((CharSequence[]) values.toArray(new CharSequence[values.size()]));
        updateLockscreenNotifications();
        if (this.mLockscreen.getEntries().length > 1) {
            this.mLockscreen.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int val = Integer.parseInt((String) newValue);
                    if (val == ConfigureNotificationSettings.this.mLockscreenSelectedValue) {
                        return false;
                    }
                    boolean enabled = val != R.string.lock_screen_notifications_summary_disable;
                    boolean show = val == R.string.lock_screen_notifications_summary_show;
                    Settings.Secure.putInt(ConfigureNotificationSettings.this.getContentResolver(), "lock_screen_allow_private_notifications", show ? 1 : 0);
                    Settings.Secure.putInt(ConfigureNotificationSettings.this.getContentResolver(), "lock_screen_show_notifications", enabled ? 1 : 0);
                    ConfigureNotificationSettings.this.mLockscreenSelectedValue = val;
                    return true;
                }
            });
        } else {
            this.mLockscreen.setEnabled(false);
        }
    }

    private void initLockscreenNotificationsForProfile() {
        this.mLockscreenProfile = (RestrictedDropDownPreference) getPreferenceScreen().findPreference("lock_screen_notifications_profile");
        if (this.mLockscreenProfile == null) {
            Log.i("ConfigNotiSettings", "Preference not found: lock_screen_notifications_profile");
            return;
        }
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        entries.add(getString(R.string.lock_screen_notifications_summary_disable_profile));
        values.add(Integer.toString(R.string.lock_screen_notifications_summary_disable_profile));
        String summaryShowEntry = getString(R.string.lock_screen_notifications_summary_show_profile);
        String summaryShowEntryValue = Integer.toString(R.string.lock_screen_notifications_summary_show_profile);
        entries.add(summaryShowEntry);
        values.add(summaryShowEntryValue);
        setRestrictedIfNotificationFeaturesDisabled(summaryShowEntry, summaryShowEntryValue, 12);
        if (this.mSecureProfile) {
            String summaryHideEntry = getString(R.string.lock_screen_notifications_summary_hide_profile);
            String summaryHideEntryValue = Integer.toString(R.string.lock_screen_notifications_summary_hide_profile);
            entries.add(summaryHideEntry);
            values.add(summaryHideEntryValue);
            setRestrictedIfNotificationFeaturesDisabled(summaryHideEntry, summaryHideEntryValue, 4);
        }
        this.mLockscreenProfile.setOnPreClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                return ConfigureNotificationSettings.this.m994x3aedef12(arg0);
            }
        });
        this.mLockscreenProfile.setEntries((CharSequence[]) entries.toArray(new CharSequence[entries.size()]));
        this.mLockscreenProfile.setEntryValues((CharSequence[]) values.toArray(new CharSequence[values.size()]));
        updateLockscreenNotificationsForProfile();
        if (this.mLockscreenProfile.getEntries().length > 1) {
            this.mLockscreenProfile.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int val = Integer.parseInt((String) newValue);
                    if (val == ConfigureNotificationSettings.this.mLockscreenSelectedValueProfile) {
                        return false;
                    }
                    boolean enabled = val != R.string.lock_screen_notifications_summary_disable_profile;
                    boolean show = val == R.string.lock_screen_notifications_summary_show_profile;
                    Settings.Secure.putIntForUser(ConfigureNotificationSettings.this.getContentResolver(), "lock_screen_allow_private_notifications", show ? 1 : 0, ConfigureNotificationSettings.this.mProfileChallengeUserId);
                    Settings.Secure.putIntForUser(ConfigureNotificationSettings.this.getContentResolver(), "lock_screen_show_notifications", enabled ? 1 : 0, ConfigureNotificationSettings.this.mProfileChallengeUserId);
                    ConfigureNotificationSettings.this.mLockscreenSelectedValueProfile = val;
                    return true;
                }
            });
        } else {
            this.mLockscreenProfile.setEnabled(false);
        }
    }

    boolean m994x3aedef12(Preference p) {
        return Utils.startQuietModeDialogIfNecessary(this.mContext, UserManager.get(this.mContext), this.mProfileChallengeUserId);
    }

    private void setRestrictedIfNotificationFeaturesDisabled(CharSequence entry, CharSequence entryValue, int keyguardNotificationFeatures) {
        RestrictedLockUtils.EnforcedAdmin profileAdmin;
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(this.mContext, keyguardNotificationFeatures, UserHandle.myUserId());
        if (admin != null && this.mLockscreen != null) {
            RestrictedDropDownPreference.RestrictedItem item = new RestrictedDropDownPreference.RestrictedItem(entry, entryValue, admin);
            this.mLockscreen.addRestrictedItem(item);
        }
        if (this.mProfileChallengeUserId == -10000 || (profileAdmin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(this.mContext, keyguardNotificationFeatures, this.mProfileChallengeUserId)) == null || this.mLockscreenProfile == null) {
            return;
        }
        RestrictedDropDownPreference.RestrictedItem item2 = new RestrictedDropDownPreference.RestrictedItem(entry, entryValue, profileAdmin);
        this.mLockscreenProfile.addRestrictedItem(item2);
    }

    public void updateLockscreenNotifications() {
        boolean allowPrivate;
        int i;
        if (this.mLockscreen == null) {
            return;
        }
        boolean enabled = getLockscreenNotificationsEnabled(UserHandle.myUserId());
        if (!this.mSecure) {
            allowPrivate = true;
        } else {
            allowPrivate = getLockscreenAllowPrivateNotifications(UserHandle.myUserId());
        }
        if (enabled) {
            i = allowPrivate ? R.string.lock_screen_notifications_summary_show : R.string.lock_screen_notifications_summary_hide;
        } else {
            i = R.string.lock_screen_notifications_summary_disable;
        }
        this.mLockscreenSelectedValue = i;
        this.mLockscreen.setValue(Integer.toString(this.mLockscreenSelectedValue));
    }

    public void updateLockscreenNotificationsForProfile() {
        boolean allowPrivate;
        int i;
        if (this.mProfileChallengeUserId == -10000 || this.mLockscreenProfile == null) {
            return;
        }
        boolean enabled = getLockscreenNotificationsEnabled(this.mProfileChallengeUserId);
        if (!this.mSecureProfile) {
            allowPrivate = true;
        } else {
            allowPrivate = getLockscreenAllowPrivateNotifications(this.mProfileChallengeUserId);
        }
        if (!enabled) {
            i = R.string.lock_screen_notifications_summary_disable_profile;
        } else {
            i = allowPrivate ? R.string.lock_screen_notifications_summary_show_profile : R.string.lock_screen_notifications_summary_hide_profile;
        }
        this.mLockscreenSelectedValueProfile = i;
        this.mLockscreenProfile.setValue(Integer.toString(this.mLockscreenSelectedValueProfile));
    }

    private boolean getLockscreenNotificationsEnabled(int userId) {
        return Settings.Secure.getIntForUser(getContentResolver(), "lock_screen_show_notifications", 0, userId) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications(int userId) {
        return Settings.Secure.getIntForUser(getContentResolver(), "lock_screen_allow_private_notifications", 0, userId) != 0;
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri LOCK_SCREEN_PRIVATE_URI;
        private final Uri LOCK_SCREEN_SHOW_URI;
        private final Uri NOTIFICATION_LIGHT_PULSE_URI;

        public SettingsObserver() {
            super(new Handler());
            this.NOTIFICATION_LIGHT_PULSE_URI = Settings.System.getUriFor("notification_light_pulse");
            this.LOCK_SCREEN_PRIVATE_URI = Settings.Secure.getUriFor("lock_screen_allow_private_notifications");
            this.LOCK_SCREEN_SHOW_URI = Settings.Secure.getUriFor("lock_screen_show_notifications");
        }

        public void register(boolean register) {
            ContentResolver cr = ConfigureNotificationSettings.this.getContentResolver();
            if (register) {
                cr.registerContentObserver(this.NOTIFICATION_LIGHT_PULSE_URI, false, this);
                cr.registerContentObserver(this.LOCK_SCREEN_PRIVATE_URI, false, this);
                cr.registerContentObserver(this.LOCK_SCREEN_SHOW_URI, false, this);
                return;
            }
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (this.NOTIFICATION_LIGHT_PULSE_URI.equals(uri)) {
                ConfigureNotificationSettings.this.updatePulse();
            }
            if (!this.LOCK_SCREEN_PRIVATE_URI.equals(uri) && !this.LOCK_SCREEN_SHOW_URI.equals(uri)) {
                return;
            }
            ConfigureNotificationSettings.this.updateLockscreenNotifications();
            if (ConfigureNotificationSettings.this.mProfileChallengeUserId == -10000) {
                return;
            }
            ConfigureNotificationSettings.this.updateLockscreenNotificationsForProfile();
        }
    }
}
