package com.android.packageinstaller.permission.ui.handheld;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.AppPermissions;
import com.android.packageinstaller.permission.model.Permission;
import com.android.packageinstaller.permission.utils.LocationUtils;
import com.android.packageinstaller.permission.utils.SafetyNetLogger;
import com.android.packageinstaller.permission.utils.Utils;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import java.util.ArrayList;
import java.util.List;

public final class AppPermissionsFragment extends SettingsWithHeader implements Preference.OnPreferenceChangeListener {
    private AppPermissions mAppPermissions;
    private PreferenceScreen mExtraScreen;
    private boolean mHasConfirmedRevoke;
    private List<AppPermissionGroup> mToggledGroups;

    public static AppPermissionsFragment newInstance(String str) {
        return (AppPermissionsFragment) setPackageName(new AppPermissionsFragment(), str);
    }

    private static <T extends Fragment> T setPackageName(T t, String str) {
        Bundle bundle = new Bundle();
        bundle.putString("android.intent.extra.PACKAGE_NAME", str);
        t.setArguments(bundle);
        return t;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setLoading(true, false);
        setHasOptionsMenu(true);
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        String string = getArguments().getString("android.intent.extra.PACKAGE_NAME");
        Activity activity = getActivity();
        PackageInfo packageInfo = getPackageInfo(activity, string);
        if (packageInfo == null) {
            Toast.makeText(activity, R.string.app_not_found_dlg_title, 1).show();
            activity.finish();
        } else {
            this.mAppPermissions = new AppPermissions(activity, packageInfo, null, true, new Runnable() {
                @Override
                public void run() {
                    AppPermissionsFragment.this.getActivity().finish();
                }
            });
            loadPreferences();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mAppPermissions.refresh();
        loadPreferences();
        setPreferencesCheckedState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 0) {
            showAllPermissions(null);
            return true;
        }
        if (itemId == 16908332) {
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        if (this.mAppPermissions != null) {
            bindUi(this, this.mAppPermissions.getPackageInfo());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menu.add(0, 0, 0, R.string.all_permissions);
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, R.string.help_app_permissions, getClass().getName());
    }

    private void showAllPermissions(String str) {
        getFragmentManager().beginTransaction().replace(android.R.id.content, AllAppPermissionsFragment.newInstance(getArguments().getString("android.intent.extra.PACKAGE_NAME"), str)).addToBackStack("AllPerms").commit();
    }

    private static void bindUi(SettingsWithHeader settingsWithHeader, PackageInfo packageInfo) {
        Activity activity = settingsWithHeader.getActivity();
        PackageManager packageManager = activity.getPackageManager();
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        settingsWithHeader.setHeader(IconDrawableFactory.newInstance(activity).getBadgedIcon(applicationInfo), applicationInfo.loadLabel(packageManager), activity.getIntent().getBooleanExtra("hideInfoButton", false) ? null : new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", packageInfo.packageName, null)));
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_permissions);
        }
    }

    private void loadPreferences() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) {
            preferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            setPreferenceScreen(preferenceScreen);
        }
        preferenceScreen.removeAll();
        if (this.mExtraScreen != null) {
            this.mExtraScreen.removeAll();
        }
        Preference preference = new Preference(activity);
        preference.setIcon(R.drawable.ic_toc);
        preference.setTitle(R.string.additional_permissions);
        for (final AppPermissionGroup appPermissionGroup : this.mAppPermissions.getPermissionGroups()) {
            if (Utils.shouldShowPermission(appPermissionGroup, this.mAppPermissions.getPackageInfo().packageName)) {
                boolean zEquals = appPermissionGroup.getDeclaringPackage().equals("android");
                final RestrictedSwitchPreference restrictedSwitchPreference = new RestrictedSwitchPreference(activity);
                restrictedSwitchPreference.setChecked(appPermissionGroup.areRuntimePermissionsGranted());
                if (Utils.areGroupPermissionsIndividuallyControlled(getContext(), appPermissionGroup.getName())) {
                    restrictedSwitchPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public final boolean onPreferenceClick(Preference preference2) {
                            return AppPermissionsFragment.lambda$loadPreferences$0(this.f$0, appPermissionGroup, preference2);
                        }
                    });
                    restrictedSwitchPreference.setSwitchOnClickListener(new View.OnClickListener() {
                        @Override
                        public final void onClick(View view) {
                            AppPermissionsFragment.lambda$loadPreferences$1(this.f$0, restrictedSwitchPreference, appPermissionGroup, view);
                        }
                    });
                    updateSummaryForIndividuallyControlledPermissionGroup(appPermissionGroup, restrictedSwitchPreference);
                } else {
                    restrictedSwitchPreference.setOnPreferenceChangeListener(this);
                }
                restrictedSwitchPreference.setKey(appPermissionGroup.getName());
                restrictedSwitchPreference.setIcon(Utils.applyTint(getContext(), Utils.loadDrawable(activity.getPackageManager(), appPermissionGroup.getIconPkg(), appPermissionGroup.getIconResId()), android.R.attr.colorControlNormal));
                restrictedSwitchPreference.setTitle(appPermissionGroup.getLabel());
                if (appPermissionGroup.isPolicyFixed()) {
                    RestrictedLockUtils.EnforcedAdmin profileOrDeviceOwner = RestrictedLockUtils.getProfileOrDeviceOwner(getContext(), appPermissionGroup.getUserId());
                    if (profileOrDeviceOwner != null) {
                        restrictedSwitchPreference.setDisabledByAdmin(profileOrDeviceOwner);
                        restrictedSwitchPreference.setSummary(R.string.disabled_by_admin_summary_text);
                    } else {
                        restrictedSwitchPreference.setSummary(R.string.permission_summary_enforced_by_policy);
                        restrictedSwitchPreference.setEnabled(false);
                    }
                }
                restrictedSwitchPreference.setPersistent(false);
                if (zEquals) {
                    preferenceScreen.addPreference(restrictedSwitchPreference);
                } else {
                    if (this.mExtraScreen == null) {
                        this.mExtraScreen = getPreferenceManager().createPreferenceScreen(activity);
                    }
                    this.mExtraScreen.addPreference(restrictedSwitchPreference);
                }
            }
        }
        if (this.mExtraScreen != null) {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference2) {
                    AdditionalPermissionsFragment additionalPermissionsFragment = new AdditionalPermissionsFragment();
                    AppPermissionsFragment.setPackageName(additionalPermissionsFragment, AppPermissionsFragment.this.getArguments().getString("android.intent.extra.PACKAGE_NAME"));
                    additionalPermissionsFragment.setTargetFragment(AppPermissionsFragment.this, 0);
                    AppPermissionsFragment.this.getFragmentManager().beginTransaction().replace(android.R.id.content, additionalPermissionsFragment).addToBackStack(null).commit();
                    return true;
                }
            });
            int preferenceCount = this.mExtraScreen.getPreferenceCount();
            preference.setSummary(getResources().getQuantityString(R.plurals.additional_permissions_more, preferenceCount, Integer.valueOf(preferenceCount)));
            preferenceScreen.addPreference(preference);
        }
        setLoading(false, true);
    }

    public static boolean lambda$loadPreferences$0(AppPermissionsFragment appPermissionsFragment, AppPermissionGroup appPermissionGroup, Preference preference) {
        appPermissionsFragment.showAllPermissions(appPermissionGroup.getName());
        return false;
    }

    public static void lambda$loadPreferences$1(AppPermissionsFragment appPermissionsFragment, RestrictedSwitchPreference restrictedSwitchPreference, AppPermissionGroup appPermissionGroup, View view) {
        Switch r4 = (Switch) view;
        appPermissionsFragment.onPreferenceChange(restrictedSwitchPreference, Boolean.valueOf(r4.isChecked()));
        appPermissionsFragment.updateSummaryForIndividuallyControlledPermissionGroup(appPermissionGroup, restrictedSwitchPreference);
        restrictedSwitchPreference.setCheckedOverride(r4.isChecked());
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, Object obj) {
        final AppPermissionGroup permissionGroup = this.mAppPermissions.getPermissionGroup(preference.getKey());
        if (permissionGroup == null) {
            return false;
        }
        addToggledGroup(permissionGroup);
        if (LocationUtils.isLocationGroupAndProvider(permissionGroup.getName(), permissionGroup.getApp().packageName)) {
            LocationUtils.showLocationDialog(getContext(), this.mAppPermissions.getAppLabel());
            return false;
        }
        if (obj == Boolean.TRUE) {
            permissionGroup.grantRuntimePermissions(false);
            return true;
        }
        final boolean zHasGrantedByDefaultPermission = permissionGroup.hasGrantedByDefaultPermission();
        if (zHasGrantedByDefaultPermission || (!permissionGroup.doesSupportRuntimePermissions() && !this.mHasConfirmedRevoke)) {
            new AlertDialog.Builder(getContext()).setMessage(zHasGrantedByDefaultPermission ? R.string.system_warning : R.string.old_sdk_deny_warning).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    AppPermissionsFragment.lambda$onPreferenceChange$2(preference, dialogInterface, i);
                }
            }).setPositiveButton(R.string.grant_dialog_button_deny_anyway, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    AppPermissionsFragment.lambda$onPreferenceChange$3(this.f$0, preference, permissionGroup, zHasGrantedByDefaultPermission, dialogInterface, i);
                }
            }).show();
            return false;
        }
        permissionGroup.revokeRuntimePermissions(false);
        return true;
    }

    static void lambda$onPreferenceChange$2(Preference preference, DialogInterface dialogInterface, int i) {
        if (preference instanceof MultiTargetSwitchPreference) {
            ((MultiTargetSwitchPreference) preference).setCheckedOverride(true);
        }
    }

    public static void lambda$onPreferenceChange$3(AppPermissionsFragment appPermissionsFragment, Preference preference, AppPermissionGroup appPermissionGroup, boolean z, DialogInterface dialogInterface, int i) {
        ((SwitchPreference) preference).setChecked(false);
        appPermissionGroup.revokeRuntimePermissions(false);
        if (Utils.areGroupPermissionsIndividuallyControlled(appPermissionsFragment.getContext(), appPermissionGroup.getName())) {
            appPermissionsFragment.updateSummaryForIndividuallyControlledPermissionGroup(appPermissionGroup, preference);
        }
        if (!z) {
            appPermissionsFragment.mHasConfirmedRevoke = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        logToggledGroups();
    }

    private void updateSummaryForIndividuallyControlledPermissionGroup(AppPermissionGroup appPermissionGroup, Preference preference) {
        int i;
        List<Permission> permissions = appPermissionGroup.getPermissions();
        int size = permissions.size();
        int i2 = 0;
        for (int i3 = 0; i3 < size; i3++) {
            Permission permission = permissions.get(i3);
            if (appPermissionGroup.doesSupportRuntimePermissions()) {
                if (!permission.isGranted()) {
                    i2++;
                }
            } else if (!permission.isAppOpAllowed() || permission.isReviewRequired()) {
            }
        }
        if (i2 == 0) {
            i = R.string.permission_revoked_none;
        } else if (i2 == size) {
            i = R.string.permission_revoked_all;
        } else {
            i = R.string.permission_revoked_count;
        }
        preference.setSummary(getString(i, new Object[]{Integer.valueOf(i2)}));
    }

    private void addToggledGroup(AppPermissionGroup appPermissionGroup) {
        if (this.mToggledGroups == null) {
            this.mToggledGroups = new ArrayList();
        }
        if (this.mToggledGroups.contains(appPermissionGroup)) {
            this.mToggledGroups.remove(appPermissionGroup);
        } else {
            this.mToggledGroups.add(appPermissionGroup);
        }
    }

    private void logToggledGroups() {
        if (this.mToggledGroups != null) {
            SafetyNetLogger.logPermissionsToggled(this.mAppPermissions.getPackageInfo().packageName, this.mToggledGroups);
            this.mToggledGroups = null;
        }
    }

    private void setPreferencesCheckedState() {
        setPreferencesCheckedState(getPreferenceScreen());
        if (this.mExtraScreen != null) {
            setPreferencesCheckedState(this.mExtraScreen);
        }
    }

    private void setPreferencesCheckedState(PreferenceScreen preferenceScreen) {
        int preferenceCount = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = preferenceScreen.getPreference(i);
            if (preference instanceof SwitchPreference) {
                SwitchPreference switchPreference = (SwitchPreference) preference;
                AppPermissionGroup permissionGroup = this.mAppPermissions.getPermissionGroup(switchPreference.getKey());
                if (permissionGroup != null) {
                    switchPreference.setChecked(permissionGroup.areRuntimePermissionsGranted());
                }
            }
        }
    }

    private static PackageInfo getPackageInfo(Activity activity, String str) {
        try {
            return activity.getPackageManager().getPackageInfo(str, 4096);
        } catch (PackageManager.NameNotFoundException e) {
            Log.i("ManagePermsFragment", "No package:" + activity.getCallingPackage(), e);
            return null;
        }
    }

    public static class AdditionalPermissionsFragment extends SettingsWithHeader {
        AppPermissionsFragment mOuterFragment;

        @Override
        public void onCreate(Bundle bundle) {
            this.mOuterFragment = (AppPermissionsFragment) getTargetFragment();
            super.onCreate(bundle);
            setHeader(this.mOuterFragment.mIcon, this.mOuterFragment.mLabel, this.mOuterFragment.mInfoIntent);
            setHasOptionsMenu(true);
            setPreferenceScreen(this.mOuterFragment.mExtraScreen);
        }

        @Override
        public void onViewCreated(View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            AppPermissionsFragment.bindUi(this, AppPermissionsFragment.getPackageInfo(getActivity(), getArguments().getString("android.intent.extra.PACKAGE_NAME")));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem menuItem) {
            if (menuItem.getItemId() == 16908332) {
                getFragmentManager().popBackStack();
                return true;
            }
            return super.onOptionsItemSelected(menuItem);
        }
    }
}
