package com.android.packageinstaller.permission.ui.television;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.AppPermissions;
import com.android.packageinstaller.permission.ui.ReviewPermissionsActivity;
import com.android.packageinstaller.permission.utils.LocationUtils;
import com.android.packageinstaller.permission.utils.SafetyNetLogger;
import com.android.packageinstaller.permission.utils.Utils;
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
            return;
        }
        this.mAppPermissions = new AppPermissions(activity, packageInfo, null, true, new Runnable() {
            @Override
            public final void run() {
                this.f$0.getActivity().finish();
            }
        });
        if (this.mAppPermissions.isReviewRequired()) {
            Intent intent = new Intent(getActivity(), (Class<?>) ReviewPermissionsActivity.class);
            intent.putExtra("android.intent.extra.PACKAGE_NAME", string);
            startActivity(intent);
            getActivity().finish();
            return;
        }
        loadPreferences();
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
            getFragmentManager().beginTransaction().replace(android.R.id.content, AllAppPermissionsFragment.newInstance(getArguments().getString("android.intent.extra.PACKAGE_NAME"))).addToBackStack("AllPerms").commit();
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
    }

    private static void bindUi(SettingsWithHeader settingsWithHeader, PackageInfo packageInfo) {
        Activity activity = settingsWithHeader.getActivity();
        PackageManager packageManager = activity.getPackageManager();
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        settingsWithHeader.setHeader(applicationInfo.loadIcon(packageManager), applicationInfo.loadLabel(packageManager), activity.getIntent().getBooleanExtra("hideInfoButton", false) ? null : new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", packageInfo.packageName, null)), settingsWithHeader.getString(R.string.app_permissions_decor_title));
    }

    private void loadPreferences() {
        Context context = getPreferenceManager().getContext();
        if (context == null) {
            return;
        }
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        preferenceScreen.addPreference(createHeaderLineTwoPreference(context));
        if (this.mExtraScreen != null) {
            this.mExtraScreen.removeAll();
            this.mExtraScreen = null;
        }
        Preference preference = new Preference(context);
        preference.setIcon(R.drawable.ic_toc);
        preference.setTitle(R.string.additional_permissions);
        for (AppPermissionGroup appPermissionGroup : this.mAppPermissions.getPermissionGroups()) {
            if (Utils.shouldShowPermission(appPermissionGroup, this.mAppPermissions.getPackageInfo().packageName)) {
                boolean zEquals = appPermissionGroup.getDeclaringPackage().equals("android");
                SwitchPreference switchPreference = new SwitchPreference(context);
                switchPreference.setOnPreferenceChangeListener(this);
                switchPreference.setKey(appPermissionGroup.getName());
                switchPreference.setIcon(Utils.applyTint(getContext(), Utils.loadDrawable(context.getPackageManager(), appPermissionGroup.getIconPkg(), appPermissionGroup.getIconResId()), android.R.attr.colorControlNormal));
                switchPreference.setTitle(appPermissionGroup.getLabel());
                if (appPermissionGroup.isPolicyFixed()) {
                    switchPreference.setSummary(getString(R.string.permission_summary_enforced_by_policy));
                }
                switchPreference.setPersistent(false);
                switchPreference.setEnabled(!appPermissionGroup.isPolicyFixed());
                switchPreference.setChecked(appPermissionGroup.areRuntimePermissionsGranted());
                if (zEquals) {
                    preferenceScreen.addPreference(switchPreference);
                } else {
                    if (this.mExtraScreen == null) {
                        this.mExtraScreen = getPreferenceManager().createPreferenceScreen(context);
                        this.mExtraScreen.addPreference(createHeaderLineTwoPreference(context));
                    }
                    this.mExtraScreen.addPreference(switchPreference);
                }
            }
        }
        if (this.mExtraScreen != null) {
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public final boolean onPreferenceClick(Preference preference2) {
                    return AppPermissionsFragment.lambda$loadPreferences$1(this.f$0, preference2);
                }
            });
            int preferenceCount = this.mExtraScreen.getPreferenceCount() - 1;
            preference.setSummary(getResources().getQuantityString(R.plurals.additional_permissions_more, preferenceCount, Integer.valueOf(preferenceCount)));
            preferenceScreen.addPreference(preference);
        }
        setLoading(false, true);
    }

    public static boolean lambda$loadPreferences$1(AppPermissionsFragment appPermissionsFragment, Preference preference) {
        AdditionalPermissionsFragment additionalPermissionsFragment = new AdditionalPermissionsFragment();
        setPackageName(additionalPermissionsFragment, appPermissionsFragment.getArguments().getString("android.intent.extra.PACKAGE_NAME"));
        additionalPermissionsFragment.setTargetFragment(appPermissionsFragment, 0);
        appPermissionsFragment.getFragmentManager().beginTransaction().replace(android.R.id.content, additionalPermissionsFragment).addToBackStack(null).commit();
        return true;
    }

    private Preference createHeaderLineTwoPreference(Context context) {
        Preference preference = new Preference(context) {
            @Override
            public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
                super.onBindViewHolder(preferenceViewHolder);
                preferenceViewHolder.itemView.setBackgroundColor(AppPermissionsFragment.this.getResources().getColor(R.color.lb_header_banner_color));
            }
        };
        preference.setKey("HeaderPreferenceKey");
        preference.setSelectable(false);
        preference.setTitle(this.mLabel);
        preference.setIcon(this.mIcon);
        return preference;
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
            new AlertDialog.Builder(getContext()).setMessage(zHasGrantedByDefaultPermission ? R.string.system_warning : R.string.old_sdk_deny_warning).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(R.string.grant_dialog_button_deny_anyway, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    AppPermissionsFragment.lambda$onPreferenceChange$2(this.f$0, preference, permissionGroup, zHasGrantedByDefaultPermission, dialogInterface, i);
                }
            }).show();
            return false;
        }
        permissionGroup.revokeRuntimePermissions(false);
        return true;
    }

    public static void lambda$onPreferenceChange$2(AppPermissionsFragment appPermissionsFragment, Preference preference, AppPermissionGroup appPermissionGroup, boolean z, DialogInterface dialogInterface, int i) {
        ((SwitchPreference) preference).setChecked(false);
        appPermissionGroup.revokeRuntimePermissions(false);
        if (!z) {
            appPermissionsFragment.mHasConfirmedRevoke = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        logToggledGroups();
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
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String str) {
            setPreferenceScreen(this.mOuterFragment.mExtraScreen);
        }

        @Override
        public void onViewCreated(View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            bindUi(this, AppPermissionsFragment.getPackageInfo(getActivity(), getArguments().getString("android.intent.extra.PACKAGE_NAME")));
        }

        private static void bindUi(SettingsWithHeader settingsWithHeader, PackageInfo packageInfo) {
            Activity activity = settingsWithHeader.getActivity();
            PackageManager packageManager = activity.getPackageManager();
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            settingsWithHeader.setHeader(applicationInfo.loadIcon(packageManager), applicationInfo.loadLabel(packageManager), activity.getIntent().getBooleanExtra("hideInfoButton", false) ? null : new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", packageInfo.packageName, null)), settingsWithHeader.getString(R.string.additional_permissions_decor_title));
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
