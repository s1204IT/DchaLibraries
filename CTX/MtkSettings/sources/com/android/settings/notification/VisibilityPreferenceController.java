package com.android.settings.notification;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import java.util.ArrayList;

public class VisibilityPreferenceController extends NotificationPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private LockPatternUtils mLockPatternUtils;

    public VisibilityPreferenceController(Context context, LockPatternUtils lockPatternUtils, NotificationBackend notificationBackend) {
        super(context, notificationBackend);
        this.mLockPatternUtils = lockPatternUtils;
    }

    @Override
    public String getPreferenceKey() {
        return "visibility_override";
    }

    @Override
    public boolean isAvailable() {
        return super.isAvailable() && this.mChannel != null && !this.mAppRow.banned && checkCanBeVisible(2) && isLockScreenSecure();
    }

    @Override
    public void updateState(Preference preference) {
        if (this.mChannel != null && this.mAppRow != null) {
            RestrictedListPreference restrictedListPreference = (RestrictedListPreference) preference;
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            restrictedListPreference.clearRestrictedItems();
            if (getLockscreenNotificationsEnabled() && getLockscreenAllowPrivateNotifications()) {
                String string = this.mContext.getString(R.string.lock_screen_notifications_summary_show);
                String string2 = Integer.toString(-1000);
                arrayList.add(string);
                arrayList2.add(string2);
                setRestrictedIfNotificationFeaturesDisabled(restrictedListPreference, string, string2, 12);
            }
            if (getLockscreenNotificationsEnabled()) {
                String string3 = this.mContext.getString(R.string.lock_screen_notifications_summary_hide);
                String string4 = Integer.toString(0);
                arrayList.add(string3);
                arrayList2.add(string4);
                setRestrictedIfNotificationFeaturesDisabled(restrictedListPreference, string3, string4, 4);
            }
            arrayList.add(this.mContext.getString(R.string.lock_screen_notifications_summary_disable));
            arrayList2.add(Integer.toString(-1));
            restrictedListPreference.setEntries((CharSequence[]) arrayList.toArray(new CharSequence[arrayList.size()]));
            restrictedListPreference.setEntryValues((CharSequence[]) arrayList2.toArray(new CharSequence[arrayList2.size()]));
            if (this.mChannel.getLockscreenVisibility() == -1000) {
                restrictedListPreference.setValue(Integer.toString(getGlobalVisibility()));
            } else {
                restrictedListPreference.setValue(Integer.toString(this.mChannel.getLockscreenVisibility()));
            }
            restrictedListPreference.setSummary("%s");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mChannel != null) {
            int i = Integer.parseInt((String) obj);
            if (i == getGlobalVisibility()) {
                i = -1000;
            }
            this.mChannel.setLockscreenVisibility(i);
            this.mChannel.lockFields(2);
            saveChannel();
            return true;
        }
        return true;
    }

    private void setRestrictedIfNotificationFeaturesDisabled(RestrictedListPreference restrictedListPreference, CharSequence charSequence, CharSequence charSequence2, int i) {
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfKeyguardFeaturesDisabled = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(this.mContext, i, this.mAppRow.userId);
        if (enforcedAdminCheckIfKeyguardFeaturesDisabled != null) {
            restrictedListPreference.addRestrictedItem(new RestrictedListPreference.RestrictedItem(charSequence, charSequence2, enforcedAdminCheckIfKeyguardFeaturesDisabled));
        }
    }

    private int getGlobalVisibility() {
        if (!getLockscreenNotificationsEnabled()) {
            return -1;
        }
        if (!getLockscreenAllowPrivateNotifications()) {
            return 0;
        }
        return -1000;
    }

    private boolean getLockscreenNotificationsEnabled() {
        UserInfo profileParent = this.mUm.getProfileParent(UserHandle.myUserId());
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "lock_screen_show_notifications", 0, profileParent != null ? profileParent.id : UserHandle.myUserId()) != 0;
    }

    private boolean getLockscreenAllowPrivateNotifications() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "lock_screen_allow_private_notifications", 0) != 0;
    }

    protected boolean isLockScreenSecure() {
        boolean zIsSecure = this.mLockPatternUtils.isSecure(UserHandle.myUserId());
        UserInfo profileParent = this.mUm.getProfileParent(UserHandle.myUserId());
        if (profileParent != null) {
            return zIsSecure | this.mLockPatternUtils.isSecure(profileParent.id);
        }
        return zIsSecure;
    }
}
