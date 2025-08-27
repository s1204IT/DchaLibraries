package com.android.settings.users;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class AutoSyncDataPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private final Fragment mParentFragment;
    protected UserHandle mUserHandle;
    protected final UserManager mUserManager;

    public AutoSyncDataPreferenceController(Context context, Fragment fragment) {
        super(context);
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mParentFragment = fragment;
        this.mUserHandle = Process.myUserHandle();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ((SwitchPreference) preference).setChecked(ContentResolver.getMasterSyncAutomaticallyAsUser(this.mUserHandle.getIdentifier()));
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            SwitchPreference switchPreference = (SwitchPreference) preference;
            boolean zIsChecked = switchPreference.isChecked();
            switchPreference.setChecked(!zIsChecked);
            if (ActivityManager.isUserAMonkey()) {
                Log.d("AutoSyncDataController", "ignoring monkey's attempt to flip sync state");
                return true;
            }
            ConfirmAutoSyncChangeFragment.show(this.mParentFragment, zIsChecked, this.mUserHandle, switchPreference);
            return true;
        }
        return false;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !this.mUserManager.isManagedProfile() && (this.mUserManager.isRestrictedProfile() || this.mUserManager.getProfiles(UserHandle.myUserId()).size() == 1);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "auto_sync_account_data";
    }

    public static class ConfirmAutoSyncChangeFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
        boolean mEnabling;
        int mIdentifier;
        SwitchPreference mPreference;
        UserHandle mUserHandle;

        public static void show(Fragment fragment, boolean z, UserHandle userHandle, SwitchPreference switchPreference) {
            if (fragment.isAdded()) {
                ConfirmAutoSyncChangeFragment confirmAutoSyncChangeFragment = new ConfirmAutoSyncChangeFragment();
                confirmAutoSyncChangeFragment.mEnabling = z;
                confirmAutoSyncChangeFragment.mUserHandle = userHandle;
                confirmAutoSyncChangeFragment.mIdentifier = userHandle.getIdentifier();
                confirmAutoSyncChangeFragment.setTargetFragment(fragment, 0);
                confirmAutoSyncChangeFragment.mPreference = switchPreference;
                confirmAutoSyncChangeFragment.show(fragment.getFragmentManager(), "confirmAutoSyncChange");
            }
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            Activity activity = getActivity();
            if (bundle != null) {
                this.mEnabling = bundle.getBoolean("enabling");
                this.mUserHandle = (UserHandle) bundle.getParcelable("userHandle");
                this.mIdentifier = bundle.getInt("identifier");
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            if (!this.mEnabling) {
                builder.setTitle(R.string.data_usage_auto_sync_off_dialog_title);
                builder.setMessage(R.string.data_usage_auto_sync_off_dialog);
            } else {
                builder.setTitle(R.string.data_usage_auto_sync_on_dialog_title);
                builder.setMessage(R.string.data_usage_auto_sync_on_dialog);
            }
            builder.setPositiveButton(android.R.string.ok, this);
            builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }

        @Override // android.app.DialogFragment, android.app.Fragment
        public void onSaveInstanceState(Bundle bundle) {
            super.onSaveInstanceState(bundle);
            bundle.putBoolean("enabling", this.mEnabling);
            bundle.putParcelable("userHandle", this.mUserHandle);
            bundle.putInt("identifier", this.mIdentifier);
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 535;
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -1) {
                ContentResolver.setMasterSyncAutomaticallyAsUser(this.mEnabling, this.mIdentifier);
                if (this.mPreference != null) {
                    this.mPreference.setChecked(this.mEnabling);
                }
            }
        }
    }
}
