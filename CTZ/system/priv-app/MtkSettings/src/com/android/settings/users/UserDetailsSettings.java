package com.android.settings.users;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.RestrictedLockUtils;

/* loaded from: classes.dex */
public class UserDetailsSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = UserDetailsSettings.class.getSimpleName();
    private Bundle mDefaultGuestRestrictions;
    private ProgressDialog mDeletingUserDialog;
    private boolean mGuestUser;
    private SwitchPreference mPhonePref;
    private Preference mRemoveUserPref;
    private UserInfo mUserInfo;
    private UserManager mUserManager;
    private Handler mHandler = new Handler();
    private Runnable mCheckDeleteComplete = new Runnable() { // from class: com.android.settings.users.UserDetailsSettings.1
        @Override // java.lang.Runnable
        public void run() {
            if (UserDetailsSettings.this.isResumed()) {
                if (UserDetailsSettings.this.mUserInfo == null || UserDetailsSettings.this.mUserManager == null) {
                    UserDetailsSettings.this.dismissDialogAndFinish();
                    return;
                }
                UserInfo userInfo = UserDetailsSettings.this.mUserManager.getUserInfo(UserDetailsSettings.this.mUserInfo.id);
                if (userInfo == null) {
                    UserDetailsSettings.this.dismissDialogAndFinish();
                } else if (!userInfo.isEnabled()) {
                    UserDetailsSettings.this.mHandler.postDelayed(this, 500L);
                }
            }
        }
    };
    private BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() { // from class: com.android.settings.users.UserDetailsSettings.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.USER_REMOVED")) {
                UserDetailsSettings.this.dismissDialogAndFinish();
            }
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 98;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        this.mUserManager = (UserManager) activity.getSystemService("user");
        addPreferencesFromResource(R.xml.user_details_settings);
        this.mPhonePref = (SwitchPreference) findPreference("enable_calling");
        this.mRemoveUserPref = findPreference("remove_user");
        this.mGuestUser = getArguments().getBoolean("guest_user", false);
        if (!this.mGuestUser) {
            int i = getArguments().getInt("user_id", -1);
            if (i == -1) {
                throw new RuntimeException("Arguments to this fragment must contain the user id");
            }
            this.mUserInfo = this.mUserManager.getUserInfo(i);
            this.mPhonePref.setChecked(!this.mUserManager.hasUserRestriction("no_outgoing_calls", new UserHandle(i)));
            this.mRemoveUserPref.setOnPreferenceClickListener(this);
        } else {
            removePreference("remove_user");
            this.mPhonePref.setTitle(R.string.user_enable_calling);
            this.mDefaultGuestRestrictions = this.mUserManager.getDefaultGuestRestrictions();
            this.mPhonePref.setChecked(!this.mDefaultGuestRestrictions.getBoolean("no_outgoing_calls"));
        }
        if (RestrictedLockUtils.hasBaseUserRestriction(activity, "no_remove_user", UserHandle.myUserId())) {
            removePreference("remove_user");
        }
        this.mPhonePref.setOnPreferenceChangeListener(this);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        getContext().registerReceiverAsUser(this.mUserChangeReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.USER_REMOVED"), null, null);
        if (!this.mGuestUser) {
            if (this.mUserInfo != null) {
                UserInfo userInfo = this.mUserManager.getUserInfo(this.mUserInfo.id);
                if (userInfo == null) {
                    dismissDialogAndFinish();
                    return;
                } else {
                    if (!userInfo.isEnabled()) {
                        showDeleteUserDialog();
                        this.mHandler.postDelayed(this.mCheckDeleteComplete, 500L);
                        return;
                    }
                    return;
                }
            }
            dismissDialogAndFinish();
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        getActivity().unregisterReceiver(this.mUserChangeReceiver);
        super.onPause();
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        if (preference == this.mRemoveUserPref) {
            if (!this.mUserManager.isAdminUser()) {
                throw new RuntimeException("Only admins can remove a user");
            }
            showDialog(1);
            return true;
        }
        return false;
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (Boolean.TRUE.equals(obj)) {
            showDialog(this.mGuestUser ? 2 : 3);
            return false;
        }
        enableCallsAndSms(false);
        return true;
    }

    void enableCallsAndSms(boolean z) {
        this.mPhonePref.setChecked(z);
        if (this.mGuestUser) {
            this.mDefaultGuestRestrictions.putBoolean("no_outgoing_calls", !z);
            this.mDefaultGuestRestrictions.putBoolean("no_sms", true);
            this.mUserManager.setDefaultGuestRestrictions(this.mDefaultGuestRestrictions);
            for (UserInfo userInfo : this.mUserManager.getUsers(true)) {
                if (userInfo.isGuest()) {
                    UserHandle userHandleOf = UserHandle.of(userInfo.id);
                    for (String str : this.mDefaultGuestRestrictions.keySet()) {
                        this.mUserManager.setUserRestriction(str, this.mDefaultGuestRestrictions.getBoolean(str), userHandleOf);
                    }
                }
            }
            return;
        }
        UserHandle userHandleOf2 = UserHandle.of(this.mUserInfo.id);
        this.mUserManager.setUserRestriction("no_outgoing_calls", !z, userHandleOf2);
        this.mUserManager.setUserRestriction("no_sms", !z, userHandleOf2);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public Dialog onCreateDialog(int i) {
        if (getActivity() == null) {
            return null;
        }
        switch (i) {
            case 1:
                return UserDialogs.createRemoveDialog(getActivity(), this.mUserInfo.id, new DialogInterface.OnClickListener() { // from class: com.android.settings.users.UserDetailsSettings.3
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        UserDetailsSettings.this.removeUser();
                    }
                });
            case 2:
                return UserDialogs.createEnablePhoneCallsDialog(getActivity(), new DialogInterface.OnClickListener() { // from class: com.android.settings.users.UserDetailsSettings.4
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        UserDetailsSettings.this.enableCallsAndSms(true);
                    }
                });
            case 3:
                return UserDialogs.createEnablePhoneCallsAndSmsDialog(getActivity(), new DialogInterface.OnClickListener() { // from class: com.android.settings.users.UserDetailsSettings.5
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        UserDetailsSettings.this.enableCallsAndSms(true);
                    }
                });
            default:
                throw new IllegalArgumentException("Unsupported dialogId " + i);
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public int getDialogMetricsCategory(int i) {
        switch (i) {
            case 1:
                return 591;
            case 2:
                return 592;
            case 3:
                return 593;
            default:
                return 0;
        }
    }

    void removeUser() {
        showDeleteUserDialog();
        this.mUserManager.removeUser(this.mUserInfo.id);
    }

    private void showDeleteUserDialog() {
        if (this.mDeletingUserDialog == null) {
            this.mDeletingUserDialog = new ProgressDialog(getActivity());
            this.mDeletingUserDialog.setMessage(getResources().getString(R.string.master_clear_progress_text));
            this.mDeletingUserDialog.setIndeterminate(true);
            this.mDeletingUserDialog.setCancelable(false);
        }
        if (!this.mDeletingUserDialog.isShowing()) {
            this.mDeletingUserDialog.show();
        }
    }

    private void dismissDeleteUserDialog() {
        if (this.mDeletingUserDialog != null && this.mDeletingUserDialog.isShowing()) {
            this.mDeletingUserDialog.dismiss();
        }
    }

    private void dismissDialogAndFinish() {
        dismissDeleteUserDialog();
        finish();
    }
}
