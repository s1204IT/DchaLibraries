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
/* loaded from: classes.dex */
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

    @Override // com.android.settingslib.core.AbstractPreferenceController
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
        String num = Integer.toString(R.string.lock_screen_notifications_summary_show);
        arrayList.add(string);
        arrayList2.add(num);
        setRestrictedIfNotificationFeaturesDisabled(string, num, 12);
        if (this.mSecure) {
            String string2 = this.mContext.getString(R.string.lock_screen_notifications_summary_hide);
            String num2 = Integer.toString(R.string.lock_screen_notifications_summary_hide);
            arrayList.add(string2);
            arrayList2.add(num2);
            setRestrictedIfNotificationFeaturesDisabled(string2, num2, 4);
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
        String num = Integer.toString(R.string.lock_screen_notifications_summary_show_profile);
        arrayList.add(string);
        arrayList2.add(num);
        setRestrictedIfNotificationFeaturesDisabled(string, num, 12);
        if (this.mSecureProfile) {
            String string2 = this.mContext.getString(R.string.lock_screen_notifications_summary_hide_profile);
            String num2 = Integer.toString(R.string.lock_screen_notifications_summary_hide_profile);
            arrayList.add(string2);
            arrayList2.add(num2);
            setRestrictedIfNotificationFeaturesDisabled(string2, num2, 4);
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

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return false;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (this.mSettingObserver != null) {
            this.mSettingObserver.register(this.mContext.getContentResolver(), true);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        if (this.mSettingObserver != null) {
            this.mSettingObserver.register(this.mContext.getContentResolver(), false);
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        int parseInt;
        String key = preference.getKey();
        if (TextUtils.equals(this.mWorkSettingKey, key)) {
            int parseInt2 = Integer.parseInt((String) obj);
            if (parseInt2 == this.mLockscreenSelectedValueProfile) {
                return false;
            }
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", parseInt2 == R.string.lock_screen_notifications_summary_show_profile ? 1 : 0, this.mProfileUserId);
            this.mLockscreenSelectedValueProfile = parseInt2;
            return true;
        } else if (!TextUtils.equals(this.mSettingKey, key) || (parseInt = Integer.parseInt((String) obj)) == this.mLockscreenSelectedValue) {
            return false;
        } else {
            int i = parseInt != R.string.lock_screen_notifications_summary_disable ? 1 : 0;
            Settings.Secure.putInt(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", parseInt == R.string.lock_screen_notifications_summary_show ? 1 : 0);
            Settings.Secure.putInt(this.mContext.getContentResolver(), "lock_screen_show_notifications", i);
            this.mLockscreenSelectedValue = parseInt;
            return true;
        }
    }

    private void setRestrictedIfNotificationFeaturesDisabled(CharSequence charSequence, CharSequence charSequence2, int i) {
        RestrictedLockUtils.EnforcedAdmin checkIfKeyguardFeaturesDisabled;
        RestrictedLockUtils.EnforcedAdmin checkIfKeyguardFeaturesDisabled2 = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(this.mContext, i, UserHandle.myUserId());
        if (checkIfKeyguardFeaturesDisabled2 != null && this.mLockscreen != null) {
            this.mLockscreen.addRestrictedItem(new RestrictedListPreference.RestrictedItem(charSequence, charSequence2, checkIfKeyguardFeaturesDisabled2));
        }
        if (this.mProfileUserId != -10000 && (checkIfKeyguardFeaturesDisabled = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(this.mContext, i, this.mProfileUserId)) != null && this.mLockscreenProfile != null) {
            this.mLockscreenProfile.addRestrictedItem(new RestrictedListPreference.RestrictedItem(charSequence, charSequence2, checkIfKeyguardFeaturesDisabled));
        }
    }

    public static int getSummaryResource(Context context) {
        return !getLockscreenNotificationsEnabled(context) ? R.string.lock_screen_notifications_summary_disable : !FeatureFactory.getFactory(context).getSecurityFeatureProvider().getLockPatternUtils(context).isSecure(UserHandle.myUserId()) || getAllowPrivateNotifications(context, UserHandle.myUserId()) ? R.string.lock_screen_notifications_summary_show : R.string.lock_screen_notifications_summary_hide;
    }

    /* JADX INFO: Access modifiers changed from: private */
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

    /* JADX INFO: Access modifiers changed from: private */
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

    /* loaded from: classes.dex */
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
                return;
            }
            contentResolver.unregisterContentObserver(this);
        }

        @Override // android.database.ContentObserver
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
