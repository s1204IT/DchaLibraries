package com.android.settings.users;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.SwitchPreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import java.util.List;

public class UserDetailsSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = UserDetailsSettings.class.getSimpleName();
    private Bundle mDefaultGuestRestrictions;
    private boolean mGuestUser;
    private SwitchPreference mPhonePref;
    private Preference mRemoveUserPref;
    private UserInfo mUserInfo;
    private UserManager mUserManager;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Context context = getActivity();
        this.mUserManager = (UserManager) context.getSystemService("user");
        addPreferencesFromResource(R.xml.user_details_settings);
        this.mPhonePref = (SwitchPreference) findPreference("enable_calling");
        this.mRemoveUserPref = findPreference("remove_user");
        this.mGuestUser = getArguments().getBoolean("guest_user", false);
        if (!this.mGuestUser) {
            int userId = getArguments().getInt("user_id", -1);
            if (userId == -1) {
                throw new RuntimeException("Arguments to this fragment must contain the user id");
            }
            this.mUserInfo = this.mUserManager.getUserInfo(userId);
            this.mPhonePref.setChecked(!this.mUserManager.hasUserRestriction("no_outgoing_calls", new UserHandle(userId)));
            this.mRemoveUserPref.setOnPreferenceClickListener(this);
        } else {
            removePreference("remove_user");
            this.mPhonePref.setTitle(R.string.user_enable_calling);
            this.mDefaultGuestRestrictions = this.mUserManager.getDefaultGuestRestrictions();
            this.mPhonePref.setChecked(this.mDefaultGuestRestrictions.getBoolean("no_outgoing_calls") ? false : true);
        }
        if (this.mUserManager.hasUserRestriction("no_remove_user")) {
            removePreference("remove_user");
        }
        this.mPhonePref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference != this.mRemoveUserPref) {
            return false;
        }
        if (UserHandle.myUserId() != 0) {
            throw new RuntimeException("Only the owner can remove a user");
        }
        showDialog(1);
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (this.mGuestUser) {
            this.mDefaultGuestRestrictions.putBoolean("no_outgoing_calls", !((Boolean) newValue).booleanValue());
            this.mDefaultGuestRestrictions.putBoolean("no_sms", true);
            this.mUserManager.setDefaultGuestRestrictions(this.mDefaultGuestRestrictions);
            List<UserInfo> users = this.mUserManager.getUsers(true);
            for (UserInfo user : users) {
                if (user.isGuest()) {
                    UserHandle userHandle = new UserHandle(user.id);
                    Bundle userRestrictions = this.mUserManager.getUserRestrictions(userHandle);
                    userRestrictions.putAll(this.mDefaultGuestRestrictions);
                    this.mUserManager.setUserRestrictions(userRestrictions, userHandle);
                }
            }
        } else {
            UserHandle userHandle2 = new UserHandle(this.mUserInfo.id);
            this.mUserManager.setUserRestriction("no_outgoing_calls", !((Boolean) newValue).booleanValue(), userHandle2);
            this.mUserManager.setUserRestriction("no_sms", ((Boolean) newValue).booleanValue() ? false : true, userHandle2);
        }
        return true;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Context context = getActivity();
        if (context == null) {
            return null;
        }
        switch (dialogId) {
            case 1:
                return Utils.createRemoveConfirmationDialog(getActivity(), this.mUserInfo.id, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UserDetailsSettings.this.removeUser();
                    }
                });
            default:
                return null;
        }
    }

    void removeUser() {
        this.mUserManager.removeUser(this.mUserInfo.id);
        finishFragment();
    }
}
