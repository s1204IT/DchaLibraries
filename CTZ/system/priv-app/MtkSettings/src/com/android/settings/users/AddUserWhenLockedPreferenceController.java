package com.android.settings.users;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/* loaded from: classes.dex */
public class AddUserWhenLockedPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private final String mPrefKey;
    private boolean mShouldUpdateUserList;
    private final UserCapabilities mUserCaps;

    public AddUserWhenLockedPreferenceController(Context context, String str, Lifecycle lifecycle) {
        super(context);
        this.mPrefKey = str;
        this.mUserCaps = UserCapabilities.create(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        RestrictedSwitchPreference restrictedSwitchPreference = (RestrictedSwitchPreference) preference;
        restrictedSwitchPreference.setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "add_users_when_locked", 0) == 1);
        restrictedSwitchPreference.setDisabledByAdmin(this.mUserCaps.disallowAddUser() ? this.mUserCaps.getEnforcedAdmin() : null);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        int i;
        Boolean bool = (Boolean) obj;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        if (bool != null && bool.booleanValue()) {
            i = 1;
        } else {
            i = 0;
        }
        Settings.Global.putInt(contentResolver, "add_users_when_locked", i);
        return true;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        this.mShouldUpdateUserList = true;
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (this.mShouldUpdateUserList) {
            this.mUserCaps.updateAddUserCapabilities(this.mContext);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mUserCaps.isAdmin() && (!this.mUserCaps.disallowAddUser() || this.mUserCaps.disallowAddUserSetByAdmin());
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return this.mPrefKey;
    }
}
