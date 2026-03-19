package com.android.packageinstaller.permission.ui.television;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.util.Log;
import android.view.MenuItem;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.AppPermissions;
import com.android.packageinstaller.permission.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class AllAppPermissionsFragment extends SettingsWithHeader {
    private AppPermissions mAppPermissions;
    private PackageInfo mPackageInfo;

    public static AllAppPermissionsFragment newInstance(String str) {
        AllAppPermissionsFragment allAppPermissionsFragment = new AllAppPermissionsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("android.intent.extra.PACKAGE_NAME", str);
        allAppPermissionsFragment.setArguments(bundle);
        return allAppPermissionsFragment;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.all_permissions);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        try {
            this.mPackageInfo = getActivity().getPackageManager().getPackageInfo(getArguments().getString("android.intent.extra.PACKAGE_NAME"), 4096);
        } catch (PackageManager.NameNotFoundException e) {
            getActivity().finish();
        }
        this.mAppPermissions = new AppPermissions(getActivity(), this.mPackageInfo, null, false, new Runnable() {
            @Override
            public void run() {
                AllAppPermissionsFragment.this.getActivity().finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            getFragmentManager().popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private PreferenceGroup getOtherGroup() {
        PreferenceGroup preferenceGroup = (PreferenceGroup) findPreference("other_perms");
        if (preferenceGroup == null) {
            PreferenceCategory preferenceCategory = new PreferenceCategory(getPreferenceManager().getContext());
            preferenceCategory.setKey("other_perms");
            preferenceCategory.setTitle(getString(R.string.other_permissions));
            getPreferenceScreen().addPreference(preferenceCategory);
            return preferenceCategory;
        }
        return preferenceGroup;
    }

    private void updateUi() {
        Intent data;
        getPreferenceScreen().removeAll();
        ArrayList<Preference> arrayList = new ArrayList<>();
        PackageManager packageManager = getActivity().getPackageManager();
        ApplicationInfo applicationInfo = this.mPackageInfo.applicationInfo;
        Drawable drawableLoadIcon = applicationInfo.loadIcon(packageManager);
        CharSequence charSequenceLoadLabel = applicationInfo.loadLabel(packageManager);
        if (!getActivity().getIntent().getBooleanExtra("hideInfoButton", false)) {
            data = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", this.mPackageInfo.packageName, null));
        } else {
            data = null;
        }
        setHeader(drawableLoadIcon, charSequenceLoadLabel, data, null);
        if (this.mPackageInfo.requestedPermissions != null) {
            for (int i = 0; i < this.mPackageInfo.requestedPermissions.length; i++) {
                try {
                    PermissionInfo permissionInfo = packageManager.getPermissionInfo(this.mPackageInfo.requestedPermissions[i], 0);
                    if ((permissionInfo.flags & 1073741824) != 0 && (permissionInfo.flags & 2) == 0 && ((!applicationInfo.isInstantApp() || (permissionInfo.protectionLevel & 4096) != 0) && (applicationInfo.targetSdkVersion >= 23 || (permissionInfo.protectionLevel & 8192) == 0))) {
                        PermissionGroupInfo group = getGroup(permissionInfo.group, packageManager);
                        if ((permissionInfo.protectionLevel & 15) == 1) {
                            findOrCreate(group != null ? group : permissionInfo, packageManager, arrayList).addPreference(getPreference(permissionInfo, group));
                        } else if ((permissionInfo.protectionLevel & 15) == 0) {
                            PreferenceGroup otherGroup = getOtherGroup();
                            if (arrayList.indexOf(otherGroup) < 0) {
                                arrayList.add(otherGroup);
                            }
                            getOtherGroup().addPreference(getPreference(permissionInfo, group));
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("AllAppPermissionsFragment", "Can't get permission info for " + this.mPackageInfo.requestedPermissions[i], e);
                }
            }
        }
        Collections.sort(arrayList, new Comparator<Preference>() {
            @Override
            public int compare(Preference preference, Preference preference2) {
                String key = preference.getKey();
                String key2 = preference2.getKey();
                if (key.equals("other_perms")) {
                    return 1;
                }
                if (key2.equals("other_perms")) {
                    return -1;
                }
                if (Utils.isModernPermissionGroup(key) != Utils.isModernPermissionGroup(key2)) {
                    return Utils.isModernPermissionGroup(key) ? -1 : 1;
                }
                return preference.getTitle().toString().compareTo(preference2.getTitle().toString());
            }
        });
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            arrayList.get(i2).setOrder(i2);
        }
    }

    private PermissionGroupInfo getGroup(String str, PackageManager packageManager) {
        try {
            return packageManager.getPermissionGroupInfo(str, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private PreferenceGroup findOrCreate(PackageItemInfo packageItemInfo, PackageManager packageManager, ArrayList<Preference> arrayList) {
        PreferenceGroup preferenceGroup = (PreferenceGroup) findPreference(packageItemInfo.name);
        if (preferenceGroup == null) {
            PreferenceCategory preferenceCategory = new PreferenceCategory(getActivity());
            preferenceCategory.setKey(packageItemInfo.name);
            preferenceCategory.setLayoutResource(R.layout.preference_category_material);
            preferenceCategory.setTitle(packageItemInfo.loadLabel(packageManager));
            arrayList.add(preferenceCategory);
            getPreferenceScreen().addPreference(preferenceCategory);
            return preferenceCategory;
        }
        return preferenceGroup;
    }

    private Preference getPreference(PermissionInfo permissionInfo, PermissionGroupInfo permissionGroupInfo) {
        if (isMutableGranularPermission(permissionInfo.name)) {
            return getMutablePreference(permissionInfo, permissionGroupInfo);
        }
        return getImmutablePreference(permissionInfo, permissionGroupInfo);
    }

    private Preference getMutablePreference(PermissionInfo permissionInfo, PermissionGroupInfo permissionGroupInfo) {
        final AppPermissionGroup permissionGroup = this.mAppPermissions.getPermissionGroup(permissionGroupInfo.name);
        final String[] strArr = {permissionInfo.name};
        SwitchPreference switchPreference = new SwitchPreference(getPreferenceManager().getContext());
        switchPreference.setLayoutResource(R.layout.preference_permissions);
        switchPreference.setChecked(permissionGroup.areRuntimePermissionsGranted(strArr));
        switchPreference.setIcon(getTintedPermissionIcon(getActivity(), permissionInfo, permissionGroupInfo));
        switchPreference.setTitle(permissionInfo.loadLabel(getActivity().getPackageManager()));
        switchPreference.setPersistent(false);
        switchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object obj) {
                if (obj == Boolean.TRUE) {
                    permissionGroup.grantRuntimePermissions(false, strArr);
                    return true;
                }
                permissionGroup.revokeRuntimePermissions(false, strArr);
                return true;
            }
        });
        return switchPreference;
    }

    private Preference getImmutablePreference(final PermissionInfo permissionInfo, PermissionGroupInfo permissionGroupInfo) {
        final PackageManager packageManager = getActivity().getPackageManager();
        Preference preference = new Preference(getActivity());
        preference.setLayoutResource(R.layout.preference_permissions);
        preference.setIcon(getTintedPermissionIcon(getActivity(), permissionInfo, permissionGroupInfo));
        preference.setTitle(permissionInfo.loadLabel(packageManager));
        preference.setPersistent(false);
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference2) {
                new AlertDialog.Builder(AllAppPermissionsFragment.this.getActivity()).setMessage(permissionInfo.loadDescription(packageManager)).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
                return true;
            }
        });
        return preference;
    }

    private static Drawable getTintedPermissionIcon(Context context, PermissionInfo permissionInfo, PermissionGroupInfo permissionGroupInfo) {
        Drawable drawable;
        if (permissionInfo.icon != 0) {
            drawable = permissionInfo.loadIcon(context.getPackageManager());
        } else if (permissionGroupInfo != null && permissionGroupInfo.icon != 0) {
            drawable = permissionGroupInfo.loadIcon(context.getPackageManager());
        } else {
            drawable = context.getDrawable(R.drawable.ic_perm_device_info);
        }
        return Utils.applyTint(context, drawable, android.R.attr.colorControlNormal);
    }

    private boolean isMutableGranularPermission(String str) {
        if (!getContext().getPackageManager().isPermissionReviewModeEnabled()) {
            return false;
        }
        switch (str) {
        }
        return false;
    }
}
