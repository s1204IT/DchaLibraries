package com.android.settings.applications;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;
/* loaded from: classes.dex */
public abstract class AppInfoBase extends SettingsPreferenceFragment implements ApplicationsState.Callbacks {
    protected static final String TAG = AppInfoBase.class.getSimpleName();
    protected ApplicationsState.AppEntry mAppEntry;
    protected RestrictedLockUtils.EnforcedAdmin mAppsControlDisallowedAdmin;
    protected boolean mAppsControlDisallowedBySystem;
    protected DevicePolicyManager mDpm;
    protected boolean mFinishing;
    protected PackageInfo mPackageInfo;
    protected String mPackageName;
    protected PackageManager mPm;
    protected ApplicationsState.Session mSession;
    protected ApplicationsState mState;
    protected IUsbManager mUsbManager;
    protected int mUserId;
    protected UserManager mUserManager;

    protected abstract AlertDialog createDialog(int i, int i2);

    protected abstract boolean refreshUi();

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mFinishing = false;
        this.mState = ApplicationsState.getInstance(getActivity().getApplication());
        this.mSession = this.mState.newSession(this);
        Context context = getActivity();
        this.mDpm = (DevicePolicyManager) context.getSystemService("device_policy");
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mPm = context.getPackageManager();
        IBinder b = ServiceManager.getService("usb");
        this.mUsbManager = IUsbManager.Stub.asInterface(b);
        retrieveAppEntry();
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.InstrumentedPreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mSession.resume();
        this.mAppsControlDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), "no_control_apps", this.mUserId);
        this.mAppsControlDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_control_apps", this.mUserId);
        if (refreshUi()) {
            return;
        }
        setIntentAndFinish(true, true);
    }

    @Override // com.android.settings.InstrumentedPreferenceFragment, android.app.Fragment
    public void onPause() {
        this.mSession.pause();
        super.onPause();
    }

    @Override // android.app.Fragment
    public void onDestroy() {
        this.mSession.release();
        super.onDestroy();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String retrieveAppEntry() {
        Bundle args = getArguments();
        this.mPackageName = args != null ? args.getString("package") : null;
        if (this.mPackageName == null) {
            Intent intent = args == null ? getActivity().getIntent() : (Intent) args.getParcelable("intent");
            if (intent != null) {
                this.mPackageName = intent.getData().getSchemeSpecificPart();
            }
        }
        this.mUserId = UserHandle.myUserId();
        this.mAppEntry = this.mState.getEntry(this.mPackageName, this.mUserId);
        if (this.mAppEntry != null) {
            try {
                this.mPackageInfo = this.mPm.getPackageInfo(this.mAppEntry.info.packageName, 12864);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Exception when retrieving package:" + this.mAppEntry.info.packageName, e);
            }
        } else {
            Log.w(TAG, "Missing AppEntry; maybe reinstalling?");
            this.mPackageInfo = null;
        }
        return this.mPackageName;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setIntentAndFinish(boolean finish, boolean appChanged) {
        Intent intent = new Intent();
        intent.putExtra("chg", appChanged);
        SettingsActivity sa = (SettingsActivity) getActivity();
        sa.finishPreferencePanel(this, -1, intent);
        this.mFinishing = true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void showDialogInner(int id, int moveErrorCode) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id, moveErrorCode);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onRunningStateChanged(boolean running) {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageIconChanged() {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageSizeChanged(String packageName) {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onAllSizesComputed() {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onLauncherInfoChanged() {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onLoadEntriesCompleted() {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageListChanged() {
        if (refreshUi()) {
            return;
        }
        setIntentAndFinish(true, true);
    }

    public static void startAppInfoFragment(Class<?> fragment, int titleRes, String pkg, int uid, Fragment source, int request) {
        startAppInfoFragment(fragment, titleRes, pkg, uid, source.getActivity(), request);
    }

    public static void startAppInfoFragment(Class<?> fragment, int titleRes, String pkg, int uid, Activity source, int request) {
        Bundle args = new Bundle();
        args.putString("package", pkg);
        args.putInt("uid", uid);
        Intent intent = Utils.onBuildStartFragmentIntent(source, fragment.getName(), args, null, titleRes, null, false);
        source.startActivityForResultAsUser(intent, request, new UserHandle(UserHandle.getUserId(uid)));
    }

    /* loaded from: classes.dex */
    public static class MyAlertDialogFragment extends DialogFragment {
        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            int errorCode = getArguments().getInt("moveError");
            Dialog dialog = ((AppInfoBase) getTargetFragment()).createDialog(id, errorCode);
            if (dialog == null) {
                throw new IllegalArgumentException("unknown id " + id);
            }
            return dialog;
        }

        public static MyAlertDialogFragment newInstance(int id, int errorCode) {
            MyAlertDialogFragment dialogFragment = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putInt("moveError", errorCode);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }
    }
}
