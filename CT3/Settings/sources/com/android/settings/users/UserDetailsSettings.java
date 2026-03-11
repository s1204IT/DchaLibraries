package com.android.settings.users;

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
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.List;

public class UserDetailsSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private static final String TAG = UserDetailsSettings.class.getSimpleName();
    private Bundle mDefaultGuestRestrictions;
    private ProgressDialog mDeletingUserDialog;
    private boolean mGuestUser;
    private SwitchPreference mPhonePref;
    private Preference mRemoveUserPref;
    private UserInfo mUserInfo;
    private UserManager mUserManager;
    private Handler mHandler = new Handler();
    private Runnable mCheckDeleteComplete = new Runnable() {
        @Override
        public void run() {
            if (!UserDetailsSettings.this.isResumed()) {
                return;
            }
            if (UserDetailsSettings.this.mUserInfo != null && UserDetailsSettings.this.mUserManager != null) {
                UserInfo info = UserDetailsSettings.this.mUserManager.getUserInfo(UserDetailsSettings.this.mUserInfo.id);
                if (info == null) {
                    UserDetailsSettings.this.dismissDialogAndFinish();
                    return;
                } else {
                    if (info.isEnabled()) {
                        return;
                    }
                    UserDetailsSettings.this.mHandler.postDelayed(this, 500L);
                    return;
                }
            }
            UserDetailsSettings.this.dismissDialogAndFinish();
        }
    };
    private BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals("android.intent.action.USER_REMOVED")) {
                return;
            }
            UserDetailsSettings.this.dismissDialogAndFinish();
        }
    };

    @Override
    protected int getMetricsCategory() {
        return 98;
    }

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
        if (RestrictedLockUtils.hasBaseUserRestriction(context, "no_remove_user", UserHandle.myUserId())) {
            removePreference("remove_user");
        }
        this.mPhonePref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("android.intent.action.USER_REMOVED");
        getContext().registerReceiverAsUser(this.mUserChangeReceiver, UserHandle.ALL, filter, null, null);
        if (this.mGuestUser) {
            return;
        }
        if (this.mUserInfo != null) {
            UserInfo info = this.mUserManager.getUserInfo(this.mUserInfo.id);
            if (info == null) {
                dismissDialogAndFinish();
                return;
            } else {
                if (info.isEnabled()) {
                    return;
                }
                showDeleteUserDialog();
                this.mHandler.postDelayed(this.mCheckDeleteComplete, 500L);
                return;
            }
        }
        dismissDialogAndFinish();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(this.mUserChangeReceiver);
        super.onPause();
    }

    @Override
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Boolean.TRUE.equals(newValue)) {
            showDialog(this.mGuestUser ? 2 : 3);
            return false;
        }
        enableCallsAndSms(false);
        return true;
    }

    void enableCallsAndSms(boolean enabled) {
        this.mPhonePref.setChecked(enabled);
        if (this.mGuestUser) {
            this.mDefaultGuestRestrictions.putBoolean("no_outgoing_calls", enabled ? false : true);
            this.mDefaultGuestRestrictions.putBoolean("no_sms", true);
            this.mUserManager.setDefaultGuestRestrictions(this.mDefaultGuestRestrictions);
            List<UserInfo> users = this.mUserManager.getUsers(true);
            for (UserInfo user : users) {
                if (user.isGuest()) {
                    UserHandle userHandle = UserHandle.of(user.id);
                    for (String key : this.mDefaultGuestRestrictions.keySet()) {
                        this.mUserManager.setUserRestriction(key, this.mDefaultGuestRestrictions.getBoolean(key), userHandle);
                    }
                }
            }
            return;
        }
        UserHandle userHandle2 = UserHandle.of(this.mUserInfo.id);
        this.mUserManager.setUserRestriction("no_outgoing_calls", !enabled, userHandle2);
        this.mUserManager.setUserRestriction("no_sms", enabled ? false : true, userHandle2);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Context context = getActivity();
        if (context == null) {
            return null;
        }
        switch (dialogId) {
            case DefaultWfcSettingsExt.PAUSE:
                return UserDialogs.createRemoveDialog(getActivity(), this.mUserInfo.id, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UserDetailsSettings.this.removeUser();
                    }
                });
            case DefaultWfcSettingsExt.CREATE:
                return UserDialogs.createEnablePhoneCallsDialog(getActivity(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UserDetailsSettings.this.enableCallsAndSms(true);
                    }
                });
            case DefaultWfcSettingsExt.DESTROY:
                return UserDialogs.createEnablePhoneCallsAndSmsDialog(getActivity(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UserDetailsSettings.this.enableCallsAndSms(true);
                    }
                });
            default:
                throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
        }
    }

    void removeUser() {
        showDeleteUserDialog();
        this.mUserManager.removeUser(this.mUserInfo.id);
    }

    private void showDeleteUserDialog() {
        if (this.mDeletingUserDialog == null) {
            this.mDeletingUserDialog = new ProgressDialog(getActivity());
            this.mDeletingUserDialog.setMessage(getResources().getString(R.string.data_enabler_waiting_message));
            this.mDeletingUserDialog.setIndeterminate(true);
            this.mDeletingUserDialog.setCancelable(false);
        }
        if (this.mDeletingUserDialog.isShowing()) {
            return;
        }
        this.mDeletingUserDialog.show();
    }

    private void dismissDeleteUserDialog() {
        if (this.mDeletingUserDialog == null || !this.mDeletingUserDialog.isShowing()) {
            return;
        }
        this.mDeletingUserDialog.dismiss();
    }

    public void dismissDialogAndFinish() {
        dismissDeleteUserDialog();
        finishFragment();
    }
}
