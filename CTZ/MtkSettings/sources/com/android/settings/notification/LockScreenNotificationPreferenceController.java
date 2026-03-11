package com.android.settings.notification;

import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import java.util.ArrayList;

public class LockScreenNotificationPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private RestrictedListPreference mLockscreen;
    private RestrictedListPreference mLockscreenProfile;
    private int mLockscreenSelectedValue;
    private int mLockscreenSelectedValueProfile;
    private final int mProfileUserId;
    private final boolean mSecure;
    private final boolean mSecureProfile;
    private final String mSettingKey;
    private SettingObserver mSettingObserver;
    private final String mWorkSettingCategoryKey;
    private final String mWorkSettingKey;

    public LockScreenNotificationPreferenceController(Context context) {
        this(context, null, null, null);
    }

    public LockScreenNotificationPreferenceController(Context context, String str, String str2, String str3) {
        super(context);
        this.mSettingKey = str;
        this.mWorkSettingCategoryKey = str2;
        this.mWorkSettingKey = str3;
        this.mProfileUserId = Utils.getManagedProfileId(UserManager.get(context), UserHandle.myUserId());
        LockPatternUtils lockPatternUtils = FeatureFactory.getFactory(context).getSecurityFeatureProvider().getLockPatternUtils(context);
        this.mSecure = lockPatternUtils.isSecure(UserHandle.myUserId());
        this.mSecureProfile = this.mProfileUserId != -10000 && lockPatternUtils.isSecure(this.mProfileUserId);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mLockscreen = (RestrictedListPreference) preferenceScreen.findPreference(this.mSettingKey);
        if (this.mLockscreen == null) {
            Log.i("LockScreenNotifPref", "Preference not found: " + this.mSettingKey);
            return;
        }
        if (this.mProfileUserId != -10000) {
            this.mLockscreenProfile = (RestrictedListPreference) preferenceScreen.findPreference(this.mWorkSettingKey);
            this.mLockscreenProfile.setRequiresActiveUnlockedProfile(true);
            this.mLockscreenProfile.setProfileUserId(this.mProfileUserId);
        } else {
            setVisible(preferenceScreen, this.mWorkSettingKey, false);
            setVisible(preferenceScreen, this.mWorkSettingCategoryKey, false);
        }
        this.mSettingObserver = new SettingObserver();
        initLockScreenNotificationPrefDisplay();
        initLockscreenNotificationPrefForProfile();
    }

    private void initLockScreenNotificationPrefDisplay() {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        String string = this.mContext.getString(R.string.lock_screen_notifications_summary_show);
        String string2 = Integer.toString(R.string.lock_screen_notifications_summary_show);
        arrayList.add(string);
        arrayList2.add(string2);
        setRestrictedIfNotificationFeaturesDisabled(string, string2, 12);
        if (this.mSecure) {
            String string3 = this.mContext.getString(R.string.lock_screen_notifications_summary_hide);
            String string4 = Integer.toString(R.string.lock_screen_notifications_summary_hide);
            arrayList.add(string3);
            arrayList2.add(string4);
            setRestrictedIfNotificationFeaturesDisabled(string3, string4, 4);
        }
        arrayList.add(this.mContext.getString(R.string.lock_screen_notifications_summary_disable));
        arrayList2.add(Integer.toString(R.string.lock_screen_notifications_summary_disable));
        this.mLockscreen.setEntries((CharSequence[]) arrayList.toArray(new CharSequence[arrayList.size()]));
        this.mLockscreen.setEntryValues((CharSequence[]) arrayList2.toArray(new CharSequence[arrayList2.size()]));
        updateLockscreenNotifications();
        if (this.mLockscreen.getEntries().length > 1) {
            this.mLockscreen.setOnPreferenceChangeListener(this);
        } else {
            this.mLockscreen.setEnabled(false);
        }
    }

    private void initLockscreenNotificationPrefForProfile() {
        if (this.mLockscreenProfile == null) {
            Log.i("LockScreenNotifPref", "Preference not found: " + this.mWorkSettingKey);
            return;
        }
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        String string = this.mContext.getString(R.string.lock_screen_notifications_summary_show_profile);
        String string2 = Integer.toString(R.string.lock_screen_notifications_summary_show_profile);
        arrayList.add(string);
        arrayList2.add(string2);
        setRestrictedIfNotificationFeaturesDisabled(string, string2, 12);
        if (this.mSecureProfile) {
            String string3 = this.mContext.getString(R.string.lock_screen_notifications_summary_hide_profile);
            String string4 = Integer.toString(R.string.lock_screen_notifications_summary_hide_profile);
            arrayList.add(string3);
            arrayList2.add(string4);
            setRestrictedIfNotificationFeaturesDisabled(string3, string4, 4);
        }
        this.mLockscreenProfile.setEntries((CharSequence[]) arrayList.toArray(new CharSequence[arrayList.size()]));
        this.mLockscreenProfile.setEntryValues((CharSequence[]) arrayList2.toArray(new CharSequence[arrayList2.size()]));
        updateLockscreenNotificationsForProfile();
        if (this.mLockscreenProfile.getEntries().length > 1) {
            this.mLockscreenProfile.setOnPreferenceChangeListener(this);
        } else {
            this.mLockscreenProfile.setEnabled(false);
        }
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void onResume() {
        if (this.mSettingObserver != null) {
            this.mSettingObserver.register(this.mContext.getContentResolver(), true);
        }
    }

    @Override
    public void onPause() {
        if (this.mSettingObserver != null) {
            this.mSettingObserver.register(this.mContext.getContentResolver(), false);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        int i;
        String key = preference.getKey();
        if (TextUtils.equals(this.mWorkSettingKey, key)) {
            int i2 = Integer.parseInt((String) obj);
            if (i2 == this.mLockscreenSelectedValueProfile) {
                return false;
            }
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", i2 == R.string.lock_screen_notifications_summary_show_profile ? 1 : 0, this.mProfileUserId);
            this.mLockscreenSelectedValueProfile = i2;
            return true;
        }
        if (!TextUtils.equals(this.mSettingKey, key) || (i = Integer.parseInt((String) obj)) == this.mLockscreenSelectedValue) {
            return false;
        }
        int i3 = i != R.string.lock_screen_notifications_summary_disable ? 1 : 0;
        Settings.Secure.putInt(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", i == R.string.lock_screen_notifications_summary_show ? 1 : 0);
        Settings.Secure.putInt(this.mContext.getContentResolver(), "lock_screen_show_notifications", i3);
        this.mLockscreenSelectedValue = i;
        return true;
    }

    private void setRestrictedIfNotificationFeaturesDisabled(CharSequence charSequence, CharSequence charSequence2, int i) {
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfKeyguardFeaturesDisabled;
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfKeyguardFeaturesDisabled2 = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(this.mContext, i, UserHandle.myUserId());
        if (enforcedAdminCheckIfKeyguardFeaturesDisabled2 != null && this.mLockscreen != null) {
            this.mLockscreen.addRestrictedItem(new RestrictedListPreference.RestrictedItem(charSequence, charSequence2, enforcedAdminCheckIfKeyguardFeaturesDisabled2));
        }
        if (this.mProfileUserId != -10000 && (enforcedAdminCheckIfKeyguardFeaturesDisabled = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(this.mContext, i, this.mProfileUserId)) != null && this.mLockscreenProfile != null) {
            this.mLockscreenProfile.addRestrictedItem(new RestrictedListPreference.RestrictedItem(charSequence, charSequence2, enforcedAdminCheckIfKeyguardFeaturesDisabled));
        }
    }

    public static int getSummaryResource(Context context) {
        return !getLockscreenNotificationsEnabled(context) ? R.string.lock_screen_notifications_summary_disable : !FeatureFactory.getFactory(context).getSecurityFeatureProvider().getLockPatternUtils(context).isSecure(UserHandle.myUserId()) || getAllowPrivateNotifications(context, UserHandle.myUserId()) ? R.string.lock_screen_notifications_summary_show : R.string.lock_screen_notifications_summary_hide;
    }

    public void updateLockscreenNotifications() {
        if (this.mLockscreen == null) {
            return;
        }
        this.mLockscreenSelectedValue = getSummaryResource(this.mContext);
        this.mLockscreen.setSummary("%s");
        this.mLockscreen.setValue(Integer.toString(this.mLockscreenSelectedValue));
    }

    private boolean adminAllowsUnredactedNotifications(int i) {
        return (((DevicePolicyManager) this.mContext.getSystemService(DevicePolicyManager.class)).getKeyguardDisabledFeatures(null, i) & 8) == 0;
    }

    public void updateLockscreenNotificationsForProfile() {
        int i;
        if (this.mProfileUserId == -10000 || this.mLockscreenProfile == null) {
            return;
        }
        boolean z = adminAllowsUnredactedNotifications(this.mProfileUserId) && (!this.mSecureProfile || getAllowPrivateNotifications(this.mContext, this.mProfileUserId));
        this.mLockscreenProfile.setSummary("%s");
        if (z) {
            i = R.string.lock_screen_notifications_summary_show_profile;
        } else {
            i = R.string.lock_screen_notifications_summary_hide_profile;
        }
        this.mLockscreenSelectedValueProfile = i;
        this.mLockscreenProfile.setValue(Integer.toString(this.mLockscreenSelectedValueProfile));
    }

    private static boolean getLockscreenNotificationsEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "lock_screen_show_notifications", 0) != 0;
    }

    private static boolean getAllowPrivateNotifications(Context context, int i) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), "lock_screen_allow_private_notifications", 0, i) != 0;
    }

    class SettingObserver extends ContentObserver {
        private final Uri LOCK_SCREEN_PRIVATE_URI;
        private final Uri LOCK_SCREEN_SHOW_URI;

        public SettingObserver() {
            super(new Handler());
            this.LOCK_SCREEN_PRIVATE_URI = Settings.Secure.getUriFor("lock_screen_allow_private_notifications");
            this.LOCK_SCREEN_SHOW_URI = Settings.Secure.getUriFor("lock_screen_show_notifications");
        }

        public void register(ContentResolver contentResolver, boolean z) {
            if (z) {
                contentResolver.registerContentObserver(this.LOCK_SCREEN_PRIVATE_URI, false, this);
                contentResolver.registerContentObserver(this.LOCK_SCREEN_SHOW_URI, false, this);
            } else {
                contentResolver.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            super.onChange(z, uri);
            if (this.LOCK_SCREEN_PRIVATE_URI.equals(uri) || this.LOCK_SCREEN_SHOW_URI.equals(uri)) {
                LockScreenNotificationPreferenceController.this.updateLockscreenNotifications();
                if (LockScreenNotificationPreferenceController.this.mProfileUserId != -10000) {
                    LockScreenNotificationPreferenceController.this.updateLockscreenNotificationsForProfile();
                }
            }
        }
    }
}
