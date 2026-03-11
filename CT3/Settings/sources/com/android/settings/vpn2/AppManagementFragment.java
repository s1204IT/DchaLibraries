package com.android.settings.vpn2;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.internal.net.VpnConfig;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.vpn2.AppDialogFragment;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.RestrictedSwitchPreference;
import java.util.List;

public class AppManagementFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private AppOpsManager mAppOpsManager;
    private ConnectivityManager mConnectivityManager;
    private PackageInfo mPackageInfo;
    private PackageManager mPackageManager;
    private String mPackageName;
    private int mPackageUid;
    private RestrictedSwitchPreference mPreferenceAlwaysOn;
    private RestrictedPreference mPreferenceForget;
    private Preference mPreferenceVersion;
    private String mVpnLabel;
    private final int mUserId = UserHandle.myUserId();
    private final AppDialogFragment.Listener mForgetVpnDialogFragmentListener = new AppDialogFragment.Listener() {
        @Override
        public void onForget() {
            if (AppManagementFragment.this.isVpnAlwaysOn()) {
                AppManagementFragment.this.setAlwaysOnVpnByUI(false);
            }
            AppManagementFragment.this.finish();
        }

        @Override
        public void onCancel() {
        }
    };

    public static void show(Context context, AppPreference pref) {
        Bundle args = new Bundle();
        args.putString("package", pref.getPackageName());
        Utils.startWithFragmentAsUser(context, AppManagementFragment.class.getName(), args, -1, pref.getLabel(), false, new UserHandle(pref.getUserId()));
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        addPreferencesFromResource(R.xml.vpn_app_management);
        this.mPackageManager = getContext().getPackageManager();
        this.mAppOpsManager = (AppOpsManager) getContext().getSystemService(AppOpsManager.class);
        this.mConnectivityManager = (ConnectivityManager) getContext().getSystemService(ConnectivityManager.class);
        this.mPreferenceVersion = findPreference("version");
        this.mPreferenceAlwaysOn = (RestrictedSwitchPreference) findPreference("always_on_vpn");
        this.mPreferenceForget = (RestrictedPreference) findPreference("forget_vpn");
        this.mPreferenceAlwaysOn.setOnPreferenceChangeListener(this);
        this.mPreferenceForget.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean isInfoLoaded = loadInfo();
        if (isInfoLoaded) {
            this.mPreferenceVersion.setTitle(getPrefContext().getString(R.string.vpn_version, this.mPackageInfo.versionName));
            updateUI();
        } else {
            finish();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (key.equals("forget_vpn")) {
            return onForgetVpnClick();
        }
        Log.w("AppManagementFragment", "unknown key is clicked: " + key);
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals("always_on_vpn")) {
            return onAlwaysOnVpnClick(((Boolean) newValue).booleanValue());
        }
        Log.w("AppManagementFragment", "unknown key is clicked: " + preference.getKey());
        return false;
    }

    @Override
    protected int getMetricsCategory() {
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

    private boolean onAlwaysOnVpnClick(boolean isChecked) {
        if (isChecked && isLegacyVpnLockDownOrAnotherPackageAlwaysOn()) {
            ReplaceExistingVpnFragment.show(this);
            return false;
        }
        return setAlwaysOnVpnByUI(isChecked);
    }

    public boolean setAlwaysOnVpnByUI(boolean isEnabled) {
        updateRestrictedViews();
        if (!this.mPreferenceAlwaysOn.isEnabled()) {
            return false;
        }
        if (this.mUserId == 0) {
            VpnUtils.clearLockdownVpn(getContext());
        }
        boolean success = this.mConnectivityManager.setAlwaysOnVpnPackageForUser(this.mUserId, isEnabled ? this.mPackageName : null, false);
        if (isEnabled && (!success || !isVpnAlwaysOn())) {
            CannotConnectFragment.show(this, this.mVpnLabel);
        }
        return success;
    }

    private boolean checkTargetVersion() {
        int targetSdk;
        if (this.mPackageInfo == null || this.mPackageInfo.applicationInfo == null || (targetSdk = this.mPackageInfo.applicationInfo.targetSdkVersion) >= 24) {
            return true;
        }
        if (Log.isLoggable("AppManagementFragment", 3)) {
            Log.d("AppManagementFragment", "Package " + this.mPackageName + " targets SDK version " + targetSdk + "; must target at least 24 to use always-on.");
            return false;
        }
        return false;
    }

    public void updateUI() {
        if (!isAdded()) {
            return;
        }
        this.mPreferenceAlwaysOn.setChecked(isVpnAlwaysOn());
        updateRestrictedViews();
    }

    private void updateRestrictedViews() {
        if (!isAdded()) {
            return;
        }
        this.mPreferenceAlwaysOn.checkRestrictionAndSetDisabled("no_config_vpn", this.mUserId);
        this.mPreferenceForget.checkRestrictionAndSetDisabled("no_config_vpn", this.mUserId);
        if (checkTargetVersion()) {
            return;
        }
        this.mPreferenceAlwaysOn.setEnabled(false);
    }

    private String getAlwaysOnVpnPackage() {
        return this.mConnectivityManager.getAlwaysOnVpnPackageForUser(this.mUserId);
    }

    public boolean isVpnAlwaysOn() {
        return this.mPackageName.equals(getAlwaysOnVpnPackage());
    }

    private boolean loadInfo() {
        Bundle args = getArguments();
        if (args == null) {
            Log.e("AppManagementFragment", "empty bundle");
            return false;
        }
        this.mPackageName = args.getString("package");
        if (this.mPackageName == null) {
            Log.e("AppManagementFragment", "empty package name");
            return false;
        }
        try {
            this.mPackageUid = this.mPackageManager.getPackageUid(this.mPackageName, 0);
            this.mPackageInfo = this.mPackageManager.getPackageInfo(this.mPackageName, 0);
            this.mVpnLabel = VpnConfig.getVpnLabel(getPrefContext(), this.mPackageName).toString();
            if (!isVpnActivated()) {
                Log.e("AppManagementFragment", "package didn't register VPN profile");
                return false;
            }
            return true;
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e("AppManagementFragment", "package not found", nnfe);
            return false;
        }
    }

    private boolean isVpnActivated() {
        List<AppOpsManager.PackageOps> apps = this.mAppOpsManager.getOpsForPackage(this.mPackageUid, this.mPackageName, new int[]{47});
        return (apps == null || apps.size() <= 0 || apps.get(0) == null) ? false : true;
    }

    private boolean isLegacyVpnLockDownOrAnotherPackageAlwaysOn() {
        if (this.mUserId == 0) {
            String lockdownKey = VpnUtils.getLockdownVpn();
            if (lockdownKey != null) {
                return true;
            }
        }
        return (getAlwaysOnVpnPackage() == null || isVpnAlwaysOn()) ? false : true;
    }

    public static class CannotConnectFragment extends DialogFragment {
        public static void show(AppManagementFragment parent, String vpnLabel) {
            if (parent.getFragmentManager().findFragmentByTag("CannotConnect") != null) {
                return;
            }
            Bundle args = new Bundle();
            args.putString("label", vpnLabel);
            DialogFragment frag = new CannotConnectFragment();
            frag.setArguments(args);
            frag.show(parent.getFragmentManager(), "CannotConnect");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String vpnLabel = getArguments().getString("label");
            return new AlertDialog.Builder(getActivity()).setTitle(getActivity().getString(R.string.vpn_cant_connect_title, new Object[]{vpnLabel})).setMessage(getActivity().getString(R.string.vpn_cant_connect_message)).setPositiveButton(R.string.okay, (DialogInterface.OnClickListener) null).create();
        }
    }

    public static class ReplaceExistingVpnFragment extends DialogFragment implements DialogInterface.OnClickListener {
        public static void show(AppManagementFragment parent) {
            if (parent.getFragmentManager().findFragmentByTag("ReplaceExistingVpn") != null) {
                return;
            }
            ReplaceExistingVpnFragment frag = new ReplaceExistingVpnFragment();
            frag.setTargetFragment(parent, 0);
            frag.show(parent.getFragmentManager(), "ReplaceExistingVpn");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.vpn_replace_always_on_vpn_title).setMessage(getActivity().getString(R.string.vpn_replace_always_on_vpn_message)).setNegativeButton(getActivity().getString(R.string.vpn_cancel), (DialogInterface.OnClickListener) null).setPositiveButton(getActivity().getString(R.string.vpn_replace), this).create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (!(getTargetFragment() instanceof AppManagementFragment)) {
                return;
            }
            AppManagementFragment target = (AppManagementFragment) getTargetFragment();
            if (!target.setAlwaysOnVpnByUI(true)) {
                return;
            }
            target.updateUI();
        }
    }
}
