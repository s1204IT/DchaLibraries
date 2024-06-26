package com.android.settings.applications;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;
/* loaded from: classes.dex */
public abstract class AppInfoBase extends SettingsPreferenceFragment implements ApplicationsState.Callbacks {
    protected static final String TAG = AppInfoBase.class.getSimpleName();
    protected ApplicationsState.AppEntry mAppEntry;
    protected ApplicationFeatureProvider mApplicationFeatureProvider;
    protected RestrictedLockUtils.EnforcedAdmin mAppsControlDisallowedAdmin;
    protected boolean mAppsControlDisallowedBySystem;
    protected DevicePolicyManager mDpm;
    protected boolean mFinishing;
    protected boolean mListeningToPackageRemove;
    protected PackageInfo mPackageInfo;
    protected String mPackageName;
    protected final BroadcastReceiver mPackageRemovedReceiver = new BroadcastReceiver() { // from class: com.android.settings.applications.AppInfoBase.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String schemeSpecificPart = intent.getData().getSchemeSpecificPart();
            if (!AppInfoBase.this.mFinishing) {
                if (AppInfoBase.this.mAppEntry == null || AppInfoBase.this.mAppEntry.info == null || TextUtils.equals(AppInfoBase.this.mAppEntry.info.packageName, schemeSpecificPart)) {
                    AppInfoBase.this.onPackageRemoved();
                }
            }
        }
    };
    protected PackageManager mPm;
    protected ApplicationsState.Session mSession;
    protected ApplicationsState mState;
    protected IUsbManager mUsbManager;
    protected int mUserId;
    protected UserManager mUserManager;

    protected abstract AlertDialog createDialog(int i, int i2);

    protected abstract boolean refreshUi();

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mFinishing = false;
        Activity activity = getActivity();
        this.mApplicationFeatureProvider = FeatureFactory.getFactory(activity).getApplicationFeatureProvider(activity);
        this.mState = ApplicationsState.getInstance(activity.getApplication());
        this.mSession = this.mState.newSession(this, getLifecycle());
        this.mDpm = (DevicePolicyManager) activity.getSystemService("device_policy");
        this.mUserManager = (UserManager) activity.getSystemService("user");
        this.mPm = activity.getPackageManager();
        this.mUsbManager = IUsbManager.Stub.asInterface(ServiceManager.getService("usb"));
        retrieveAppEntry();
        startListeningToPackageRemove();
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mAppsControlDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(), "no_control_apps", this.mUserId);
        this.mAppsControlDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_control_apps", this.mUserId);
        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onDestroy() {
        stopListeningToPackageRemove();
        super.onDestroy();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public String retrieveAppEntry() {
        Bundle arguments = getArguments();
        this.mPackageName = arguments != null ? arguments.getString("package") : null;
        Intent intent = arguments == null ? getIntent() : (Intent) arguments.getParcelable("intent");
        if (this.mPackageName == null && intent != null && intent.getData() != null) {
            this.mPackageName = intent.getData().getSchemeSpecificPart();
        }
        if (intent != null && intent.hasExtra("android.intent.extra.user_handle")) {
            this.mUserId = ((UserHandle) intent.getParcelableExtra("android.intent.extra.user_handle")).getIdentifier();
        } else {
            this.mUserId = UserHandle.myUserId();
        }
        this.mAppEntry = this.mState.getEntry(this.mPackageName, this.mUserId);
        if (this.mAppEntry != null) {
            try {
                this.mPackageInfo = this.mPm.getPackageInfoAsUser(this.mAppEntry.info.packageName, 134222336, this.mUserId);
            } catch (PackageManager.NameNotFoundException e) {
                String str = TAG;
                Log.e(str, "Exception when retrieving package:" + this.mAppEntry.info.packageName, e);
            }
        } else {
            Log.w(TAG, "Missing AppEntry; maybe reinstalling?");
            this.mPackageInfo = null;
        }
        return this.mPackageName;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setIntentAndFinish(boolean z, boolean z2) {
        Intent intent = new Intent();
        intent.putExtra("chg", z2);
        ((SettingsActivity) getActivity()).finishPreferencePanel(-1, intent);
        this.mFinishing = true;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void showDialogInner(int i, int i2) {
        MyAlertDialogFragment newInstance = MyAlertDialogFragment.newInstance(i, i2);
        newInstance.setTargetFragment(this, 0);
        FragmentManager fragmentManager = getFragmentManager();
        newInstance.show(fragmentManager, "dialog " + i);
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onRunningStateChanged(boolean z) {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> arrayList) {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageIconChanged() {
    }

    @Override // com.android.settingslib.applications.ApplicationsState.Callbacks
    public void onPackageSizeChanged(String str) {
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
        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }

    public static void startAppInfoFragment(Class<?> cls, int i, String str, int i2, Fragment fragment, int i3, int i4) {
        Bundle bundle = new Bundle();
        bundle.putString("package", str);
        bundle.putInt("uid", i2);
        new SubSettingLauncher(fragment.getContext()).setDestination(cls.getName()).setSourceMetricsCategory(i4).setTitle(i).setArguments(bundle).setUserHandle(new UserHandle(UserHandle.getUserId(i2))).setResultListener(fragment, i3).launch();
    }

    /* loaded from: classes.dex */
    public static class MyAlertDialogFragment extends InstrumentedDialogFragment {
        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 558;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            int i = getArguments().getInt("id");
            AlertDialog createDialog = ((AppInfoBase) getTargetFragment()).createDialog(i, getArguments().getInt("moveError"));
            if (createDialog == null) {
                throw new IllegalArgumentException("unknown id " + i);
            }
            return createDialog;
        }

        public static MyAlertDialogFragment newInstance(int i, int i2) {
            MyAlertDialogFragment myAlertDialogFragment = new MyAlertDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("id", i);
            bundle.putInt("moveError", i2);
            myAlertDialogFragment.setArguments(bundle);
            return myAlertDialogFragment;
        }
    }

    protected void startListeningToPackageRemove() {
        if (this.mListeningToPackageRemove) {
            return;
        }
        this.mListeningToPackageRemove = true;
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        getContext().registerReceiver(this.mPackageRemovedReceiver, intentFilter);
    }

    protected void stopListeningToPackageRemove() {
        if (!this.mListeningToPackageRemove) {
            return;
        }
        this.mListeningToPackageRemove = false;
        getContext().unregisterReceiver(this.mPackageRemovedReceiver);
    }

    protected void onPackageRemoved() {
        getActivity().finishAndRemoveTask();
    }
}
