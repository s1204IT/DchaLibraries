package com.android.settings.vpn2;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.VpnConfig;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.vpn2.AppDialogFragment;
import com.android.settings.vpn2.ConfirmLockdownFragment;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.RestrictedSwitchPreference;

public class AppManagementFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, ConfirmLockdownFragment.ConfirmLockdownListener {
    private ConnectivityManager mConnectivityManager;
    private IConnectivityManager mConnectivityService;
    private PackageInfo mPackageInfo;
    private PackageManager mPackageManager;
    private String mPackageName;
    private RestrictedSwitchPreference mPreferenceAlwaysOn;
    private RestrictedPreference mPreferenceForget;
    private RestrictedSwitchPreference mPreferenceLockdown;
    private Preference mPreferenceVersion;
    private String mVpnLabel;
    private final int mUserId = UserHandle.myUserId();
    private final AppDialogFragment.Listener mForgetVpnDialogFragmentListener = new AppDialogFragment.Listener() {
        @Override
        public void onForget() {
            if (AppManagementFragment.this.isVpnAlwaysOn()) {
                AppManagementFragment.this.setAlwaysOnVpn(false, false);
            }
            AppManagementFragment.this.finish();
        }

        @Override
        public void onCancel() {
        }
    };

    public static void show(Context context, AppPreference appPreference, int i) {
        Bundle bundle = new Bundle();
        bundle.putString("package", appPreference.getPackageName());
        new SubSettingLauncher(context).setDestination(AppManagementFragment.class.getName()).setArguments(bundle).setTitle(appPreference.getLabel()).setSourceMetricsCategory(i).setUserHandle(new UserHandle(appPreference.getUserId())).launch();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.vpn_app_management);
        this.mPackageManager = getContext().getPackageManager();
        this.mConnectivityManager = (ConnectivityManager) getContext().getSystemService(ConnectivityManager.class);
        this.mConnectivityService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
        this.mPreferenceVersion = findPreference("version");
        this.mPreferenceAlwaysOn = (RestrictedSwitchPreference) findPreference("always_on_vpn");
        this.mPreferenceLockdown = (RestrictedSwitchPreference) findPreference("lockdown_vpn");
        this.mPreferenceForget = (RestrictedPreference) findPreference("forget_vpn");
        this.mPreferenceAlwaysOn.setOnPreferenceChangeListener(this);
        this.mPreferenceLockdown.setOnPreferenceChangeListener(this);
        this.mPreferenceForget.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (loadInfo()) {
            this.mPreferenceVersion.setTitle(getPrefContext().getString(R.string.vpn_version, this.mPackageInfo.versionName));
            updateUI();
        } else {
            finish();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (((key.hashCode() == -591389790 && key.equals("forget_vpn")) ? (byte) 0 : (byte) -1) == 0) {
            return onForgetVpnClick();
        }
        Log.w("AppManagementFragment", "unknown key is clicked: " + key);
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        byte b;
        String key = preference.getKey();
        int iHashCode = key.hashCode();
        if (iHashCode != -2008102204) {
            b = (iHashCode == -1808701950 && key.equals("lockdown_vpn")) ? (byte) 1 : (byte) -1;
        } else if (key.equals("always_on_vpn")) {
            b = 0;
        }
        switch (b) {
            case 0:
                return onAlwaysOnVpnClick(((Boolean) obj).booleanValue(), this.mPreferenceLockdown.isChecked());
            case 1:
                return onAlwaysOnVpnClick(this.mPreferenceAlwaysOn.isChecked(), ((Boolean) obj).booleanValue());
            default:
                Log.w("AppManagementFragment", "unknown key is clicked: " + preference.getKey());
                return false;
        }
    }

    @Override
    public int getMetricsCategory() {
        return 100;
    }

    private boolean onForgetVpnClick() {
        updateRestrictedViews();
        if (!this.mPreferenceForget.isEnabled()) {
            return false;
        }
        AppDialogFragment.show(this, this.mForgetVpnDialogFragmentListener, this.mPackageInfo, this.mVpnLabel, true, true);
        return true;
    }

    private boolean onAlwaysOnVpnClick(boolean z, boolean z2) {
        boolean zIsAnotherVpnActive = isAnotherVpnActive();
        boolean zIsAnyLockdownActive = VpnUtils.isAnyLockdownActive(getActivity());
        if (ConfirmLockdownFragment.shouldShow(zIsAnotherVpnActive, zIsAnyLockdownActive, z2)) {
            ConfirmLockdownFragment.show(this, zIsAnotherVpnActive, z, zIsAnyLockdownActive, z2, null);
            return false;
        }
        return setAlwaysOnVpnByUI(z, z2);
    }

    @Override
    public void onConfirmLockdown(Bundle bundle, boolean z, boolean z2) {
        setAlwaysOnVpnByUI(z, z2);
    }

    private boolean setAlwaysOnVpnByUI(boolean z, boolean z2) {
        updateRestrictedViews();
        if (!this.mPreferenceAlwaysOn.isEnabled()) {
            return false;
        }
        if (this.mUserId == 0) {
            VpnUtils.clearLockdownVpn(getContext());
        }
        boolean alwaysOnVpn = setAlwaysOnVpn(z, z2);
        if (z && (!alwaysOnVpn || !isVpnAlwaysOn())) {
            CannotConnectFragment.show(this, this.mVpnLabel);
        } else {
            updateUI();
        }
        return alwaysOnVpn;
    }

    private boolean setAlwaysOnVpn(boolean z, boolean z2) {
        return this.mConnectivityManager.setAlwaysOnVpnPackageForUser(this.mUserId, z ? this.mPackageName : null, z2);
    }

    private void updateUI() {
        if (isAdded()) {
            boolean zIsVpnAlwaysOn = isVpnAlwaysOn();
            boolean z = zIsVpnAlwaysOn && VpnUtils.isAnyLockdownActive(getActivity());
            this.mPreferenceAlwaysOn.setChecked(zIsVpnAlwaysOn);
            this.mPreferenceLockdown.setChecked(z);
            updateRestrictedViews();
        }
    }

    private void updateRestrictedViews() {
        if (isAdded()) {
            this.mPreferenceAlwaysOn.checkRestrictionAndSetDisabled("no_config_vpn", this.mUserId);
            this.mPreferenceLockdown.checkRestrictionAndSetDisabled("no_config_vpn", this.mUserId);
            this.mPreferenceForget.checkRestrictionAndSetDisabled("no_config_vpn", this.mUserId);
            if (this.mConnectivityManager.isAlwaysOnVpnPackageSupportedForUser(this.mUserId, this.mPackageName)) {
                this.mPreferenceAlwaysOn.setSummary(R.string.vpn_always_on_summary);
                return;
            }
            this.mPreferenceAlwaysOn.setEnabled(false);
            this.mPreferenceLockdown.setEnabled(false);
            this.mPreferenceAlwaysOn.setSummary(R.string.vpn_always_on_summary_not_supported);
        }
    }

    private String getAlwaysOnVpnPackage() {
        return this.mConnectivityManager.getAlwaysOnVpnPackageForUser(this.mUserId);
    }

    private boolean isVpnAlwaysOn() {
        return this.mPackageName.equals(getAlwaysOnVpnPackage());
    }

    private boolean loadInfo() {
        Bundle arguments = getArguments();
        if (arguments == null) {
            Log.e("AppManagementFragment", "empty bundle");
            return false;
        }
        this.mPackageName = arguments.getString("package");
        if (this.mPackageName != null) {
            try {
                this.mPackageInfo = this.mPackageManager.getPackageInfo(this.mPackageName, 0);
                this.mVpnLabel = VpnConfig.getVpnLabel(getPrefContext(), this.mPackageName).toString();
                if (this.mPackageInfo.applicationInfo == null) {
                    Log.e("AppManagementFragment", "package does not include an application");
                    return false;
                }
                if (!appHasVpnPermission(getContext(), this.mPackageInfo.applicationInfo)) {
                    Log.e("AppManagementFragment", "package didn't register VPN profile");
                    return false;
                }
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("AppManagementFragment", "package not found", e);
                return false;
            }
        }
        Log.e("AppManagementFragment", "empty package name");
        return false;
    }

    @VisibleForTesting
    static boolean appHasVpnPermission(Context context, ApplicationInfo applicationInfo) {
        return !ArrayUtils.isEmpty(((AppOpsManager) context.getSystemService("appops")).getOpsForPackage(applicationInfo.uid, applicationInfo.packageName, new int[]{47}));
    }

    private boolean isAnotherVpnActive() {
        try {
            VpnConfig vpnConfig = this.mConnectivityService.getVpnConfig(this.mUserId);
            if (vpnConfig != null) {
                return !TextUtils.equals(vpnConfig.user, this.mPackageName);
            }
            return false;
        } catch (RemoteException e) {
            Log.w("AppManagementFragment", "Failure to look up active VPN", e);
            return false;
        }
    }

    public static class CannotConnectFragment extends InstrumentedDialogFragment {
        @Override
        public int getMetricsCategory() {
            return 547;
        }

        public static void show(AppManagementFragment appManagementFragment, String str) {
            if (appManagementFragment.getFragmentManager().findFragmentByTag("CannotConnect") == null) {
                Bundle bundle = new Bundle();
                bundle.putString("label", str);
                CannotConnectFragment cannotConnectFragment = new CannotConnectFragment();
                cannotConnectFragment.setArguments(bundle);
                cannotConnectFragment.show(appManagementFragment.getFragmentManager(), "CannotConnect");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            return new AlertDialog.Builder(getActivity()).setTitle(getActivity().getString(R.string.vpn_cant_connect_title, new Object[]{getArguments().getString("label")})).setMessage(getActivity().getString(R.string.vpn_cant_connect_message)).setPositiveButton(R.string.okay, (DialogInterface.OnClickListener) null).create();
        }
    }
}
