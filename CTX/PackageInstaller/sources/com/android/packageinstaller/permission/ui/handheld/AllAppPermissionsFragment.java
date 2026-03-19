package com.android.packageinstaller.permission.ui.handheld;

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
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.Permission;
import com.android.packageinstaller.permission.ui.handheld.AllAppPermissionsFragment;
import com.android.packageinstaller.permission.utils.ArrayUtils;
import com.android.packageinstaller.permission.utils.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class AllAppPermissionsFragment extends SettingsWithHeader {
    private List<AppPermissionGroup> mGroups;

    public static AllAppPermissionsFragment newInstance(String str) {
        return newInstance(str, null);
    }

    public static AllAppPermissionsFragment newInstance(String str, String str2) {
        AllAppPermissionsFragment allAppPermissionsFragment = new AllAppPermissionsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("android.intent.extra.PACKAGE_NAME", str);
        bundle.putString("com.android.packageinstaller.extra.FILTER_GROUP", str2);
        allAppPermissionsFragment.setArguments(bundle);
        return allAppPermissionsFragment;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            if (getArguments().getString("com.android.packageinstaller.extra.FILTER_GROUP") == null) {
                actionBar.setTitle(R.string.all_permissions);
            } else {
                actionBar.setTitle(R.string.app_permissions);
            }
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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

    private void updateUi() {
        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
        }
        addPreferencesFromResource(R.xml.all_permissions);
        PreferenceGroup preferenceGroup = (PreferenceGroup) findPreference("other_perms");
        ArrayList<Preference> arrayList = new ArrayList<>();
        arrayList.add(preferenceGroup);
        String string = getArguments().getString("android.intent.extra.PACKAGE_NAME");
        String string2 = getArguments().getString("com.android.packageinstaller.extra.FILTER_GROUP");
        preferenceGroup.removeAll();
        PackageManager packageManager = getContext().getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(string, 4096);
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            setHeader(IconDrawableFactory.newInstance(getContext()).getBadgedIcon(applicationInfo), applicationInfo.loadLabel(packageManager), getActivity().getIntent().getBooleanExtra("hideInfoButton", false) ? null : new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", string, null)));
            if (packageInfo.requestedPermissions != null) {
                for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
                    try {
                        PermissionInfo permissionInfo = packageManager.getPermissionInfo(packageInfo.requestedPermissions[i], 0);
                        if ((permissionInfo.flags & 1073741824) != 0 && (permissionInfo.flags & 2) == 0 && ((!applicationInfo.isInstantApp() || (permissionInfo.protectionLevel & 4096) != 0) && (applicationInfo.targetSdkVersion >= 23 || (permissionInfo.protectionLevel & 8192) == 0))) {
                            if ((permissionInfo.protectionLevel & 15) == 1) {
                                PermissionGroupInfo group = getGroup(permissionInfo.group, packageManager);
                                if (group == null) {
                                    group = permissionInfo;
                                }
                                if (string2 == null || ((PackageItemInfo) group).name.equals(string2)) {
                                    findOrCreate(group, packageManager, arrayList).addPreference(getPreference(packageInfo, permissionInfo, group, packageManager));
                                }
                            } else if (string2 == null && (permissionInfo.protectionLevel & 15) == 0) {
                                preferenceGroup.addPreference(getPreference(packageInfo, permissionInfo, getGroup(permissionInfo.group, packageManager), packageManager));
                            }
                            if (string2 != null) {
                                getPreferenceScreen().removePreference(preferenceGroup);
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e("AllAppPermissionsFragment", "Can't get permission info for " + packageInfo.requestedPermissions[i], e);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e2) {
            Log.e("AllAppPermissionsFragment", "Problem getting package info for " + string, e2);
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
            PreferenceCategory preferenceCategory = new PreferenceCategory(getContext());
            preferenceCategory.setKey(packageItemInfo.name);
            preferenceCategory.setTitle(packageItemInfo.loadLabel(packageManager));
            arrayList.add(preferenceCategory);
            getPreferenceScreen().addPreference(preferenceCategory);
            return preferenceCategory;
        }
        return preferenceGroup;
    }

    private Preference getPreference(PackageInfo packageInfo, PermissionInfo permissionInfo, PackageItemInfo packageItemInfo, PackageManager packageManager) {
        Preference preference;
        Drawable drawable;
        final boolean zIsPermissionIndividuallyControlled = Utils.isPermissionIndividuallyControlled(getContext(), permissionInfo.name);
        if (zIsPermissionIndividuallyControlled) {
            preference = new MyMultiTargetSwitchPreference(getContext(), permissionInfo.name, getPermissionGroup(packageInfo, permissionInfo.name));
        } else {
            preference = new Preference(getContext());
        }
        if (permissionInfo.icon != 0) {
            drawable = permissionInfo.loadIcon(packageManager);
        } else if (packageItemInfo != null && packageItemInfo.icon != 0) {
            drawable = packageItemInfo.loadIcon(packageManager);
        } else {
            drawable = getContext().getDrawable(R.drawable.ic_perm_device_info);
        }
        preference.setIcon(Utils.applyTint(getContext(), drawable, android.R.attr.colorControlNormal));
        preference.setTitle(permissionInfo.loadSafeLabel(packageManager, 20000.0f, 1));
        preference.setSingleLineTitle(false);
        final CharSequence charSequenceLoadDescription = permissionInfo.loadDescription(packageManager);
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public final boolean onPreferenceClick(Preference preference2) {
                return AllAppPermissionsFragment.lambda$getPreference$0(this.f$0, charSequenceLoadDescription, zIsPermissionIndividuallyControlled, preference2);
            }
        });
        return preference;
    }

    public static boolean lambda$getPreference$0(AllAppPermissionsFragment allAppPermissionsFragment, CharSequence charSequence, boolean z, Preference preference) {
        new AlertDialog.Builder(allAppPermissionsFragment.getContext()).setMessage(charSequence).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
        return z;
    }

    private AppPermissionGroup getPermissionGroup(PackageInfo packageInfo, String str) throws PackageManager.NameNotFoundException {
        AppPermissionGroup appPermissionGroupCreate;
        if (this.mGroups != null) {
            int size = this.mGroups.size();
            for (int i = 0; i < size; i++) {
                appPermissionGroupCreate = this.mGroups.get(i);
                if (appPermissionGroupCreate.hasPermission(str)) {
                    break;
                }
            }
            appPermissionGroupCreate = null;
        } else {
            appPermissionGroupCreate = null;
        }
        if (appPermissionGroupCreate == null) {
            appPermissionGroupCreate = AppPermissionGroup.create(getContext(), packageInfo, str);
            if (this.mGroups == null) {
                this.mGroups = new ArrayList();
            }
            this.mGroups.add(appPermissionGroupCreate);
        }
        return appPermissionGroupCreate;
    }

    private static final class MyMultiTargetSwitchPreference extends MultiTargetSwitchPreference {
        MyMultiTargetSwitchPreference(Context context, final String str, final AppPermissionGroup appPermissionGroup) {
            super(context);
            setChecked(appPermissionGroup.areRuntimePermissionsGranted(new String[]{str}));
            setSwitchOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    AllAppPermissionsFragment.MyMultiTargetSwitchPreference.lambda$new$0(appPermissionGroup, str, view);
                }
            });
        }

        static void lambda$new$0(AppPermissionGroup appPermissionGroup, String str, View view) {
            if (((Switch) view).isChecked()) {
                appPermissionGroup.grantRuntimePermissions(false, new String[]{str});
                if (appPermissionGroup.doesSupportRuntimePermissions()) {
                    int size = appPermissionGroup.getPermissions().size();
                    String[] strArrAppendString = null;
                    int i = 0;
                    for (int i2 = 0; i2 < size; i2++) {
                        Permission permission = appPermissionGroup.getPermissions().get(i2);
                        if (!permission.isGranted()) {
                            if (!permission.isUserFixed()) {
                                strArrAppendString = ArrayUtils.appendString(strArrAppendString, permission.getName());
                            }
                        } else {
                            i++;
                        }
                    }
                    if (strArrAppendString != null) {
                        appPermissionGroup.revokeRuntimePermissions(true, strArrAppendString);
                        return;
                    } else {
                        if (appPermissionGroup.getPermissions().size() == i) {
                            appPermissionGroup.grantRuntimePermissions(false);
                            return;
                        }
                        return;
                    }
                }
                return;
            }
            appPermissionGroup.revokeRuntimePermissions(true, new String[]{str});
            if (appPermissionGroup.doesSupportRuntimePermissions() && !appPermissionGroup.areRuntimePermissionsGranted()) {
                appPermissionGroup.revokeRuntimePermissions(false);
            }
        }
    }
}
