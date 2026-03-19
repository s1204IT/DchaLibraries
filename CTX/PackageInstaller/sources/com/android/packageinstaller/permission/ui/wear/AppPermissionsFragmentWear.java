package com.android.packageinstaller.permission.ui.wear;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.wearable.view.WearableDialogHelper;
import android.util.Log;
import android.widget.Toast;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.AppPermissions;
import com.android.packageinstaller.permission.model.Permission;
import com.android.packageinstaller.permission.utils.ArrayUtils;
import com.android.packageinstaller.permission.utils.LocationUtils;
import com.android.packageinstaller.permission.utils.SafetyNetLogger;
import com.android.packageinstaller.permission.utils.Utils;
import com.android.settingslib.RestrictedLockUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AppPermissionsFragmentWear extends PreferenceFragment {
    private AppPermissions mAppPermissions;
    private boolean mHasConfirmedRevoke;
    private PackageManager mPackageManager;
    private List<AppPermissionGroup> mToggledGroups;

    public static AppPermissionsFragmentWear newInstance(String str) {
        return (AppPermissionsFragmentWear) setPackageName(new AppPermissionsFragmentWear(), str);
    }

    private static <T extends Fragment> T setPackageName(T t, String str) {
        Bundle bundle = new Bundle();
        bundle.putString("android.intent.extra.PACKAGE_NAME", str);
        t.setArguments(bundle);
        return t;
    }

    private static class PermissionSwitchPreference extends SwitchPreference {
        private final Activity mActivity;

        public PermissionSwitchPreference(Activity activity) {
            super(activity);
            this.mActivity = activity;
        }

        public void performClick(PreferenceScreen preferenceScreen) {
            super.performClick(preferenceScreen);
            if (!isEnabled()) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mActivity, RestrictedLockUtils.getProfileOrDeviceOwner(this.mActivity, UserHandle.myUserId()));
            }
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        PackageInfo packageInfo;
        super.onCreate(bundle);
        String string = getArguments().getString("android.intent.extra.PACKAGE_NAME");
        Activity activity = getActivity();
        this.mPackageManager = activity.getPackageManager();
        try {
            packageInfo = this.mPackageManager.getPackageInfo(string, 4096);
        } catch (PackageManager.NameNotFoundException e) {
            Log.i("AppPermFragWear", "No package:" + activity.getCallingPackage(), e);
            packageInfo = null;
        }
        PackageInfo packageInfo2 = packageInfo;
        if (packageInfo2 == null) {
            Toast.makeText(activity, R.string.app_not_found_dlg_title, 1).show();
            activity.finish();
        } else {
            this.mAppPermissions = new AppPermissions(activity, packageInfo2, null, true, new Runnable() {
                @Override
                public final void run() {
                    this.f$0.getActivity().finish();
                }
            });
            addPreferencesFromResource(R.xml.watch_permissions);
            initializePermissionGroupList();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mAppPermissions.refresh();
        for (AppPermissionGroup appPermissionGroup : this.mAppPermissions.getPermissionGroups()) {
            if (Utils.areGroupPermissionsIndividuallyControlled(getContext(), appPermissionGroup.getName())) {
                for (PermissionInfo permissionInfo : getPermissionInfosFromGroup(appPermissionGroup)) {
                    setPreferenceCheckedIfPresent(permissionInfo.name, appPermissionGroup.areRuntimePermissionsGranted(new String[]{permissionInfo.name}));
                }
            } else {
                setPreferenceCheckedIfPresent(appPermissionGroup.getName(), appPermissionGroup.areRuntimePermissionsGranted());
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        logAndClearToggledGroups();
    }

    private void initializePermissionGroupList() {
        String str = this.mAppPermissions.getPackageInfo().packageName;
        List<AppPermissionGroup> permissionGroups = this.mAppPermissions.getPermissionGroups();
        ArrayList arrayList = new ArrayList();
        if (!permissionGroups.isEmpty()) {
            getPreferenceScreen().removePreference(findPreference("no_permissions"));
        }
        for (AppPermissionGroup appPermissionGroup : permissionGroups) {
            if (Utils.shouldShowPermission(appPermissionGroup, str)) {
                boolean zEquals = appPermissionGroup.getDeclaringPackage().equals("android");
                if (Utils.areGroupPermissionsIndividuallyControlled(getContext(), appPermissionGroup.getName())) {
                    Iterator<PermissionInfo> it = getPermissionInfosFromGroup(appPermissionGroup).iterator();
                    while (it.hasNext()) {
                        showOrAddToNonSystemPreferences(createSwitchPreferenceForPermission(appPermissionGroup, it.next()), arrayList, zEquals);
                    }
                } else {
                    showOrAddToNonSystemPreferences(createSwitchPreferenceForGroup(appPermissionGroup), arrayList, zEquals);
                }
            }
        }
        Iterator<SwitchPreference> it2 = arrayList.iterator();
        while (it2.hasNext()) {
            getPreferenceScreen().addPreference(it2.next());
        }
    }

    private void showOrAddToNonSystemPreferences(SwitchPreference switchPreference, List<SwitchPreference> list, boolean z) {
        if (z) {
            getPreferenceScreen().addPreference(switchPreference);
        } else {
            list.add(switchPreference);
        }
    }

    private SwitchPreference createSwitchPreferenceForPermission(final AppPermissionGroup appPermissionGroup, final PermissionInfo permissionInfo) {
        final PermissionSwitchPreference permissionSwitchPreference = new PermissionSwitchPreference(getActivity());
        permissionSwitchPreference.setKey(permissionInfo.name);
        permissionSwitchPreference.setTitle(permissionInfo.loadLabel(this.mPackageManager));
        permissionSwitchPreference.setChecked(appPermissionGroup.areRuntimePermissionsGranted(new String[]{permissionInfo.name}));
        permissionSwitchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public final boolean onPreferenceChange(Preference preference, Object obj) {
                return AppPermissionsFragmentWear.lambda$createSwitchPreferenceForPermission$2(this.f$0, appPermissionGroup, permissionInfo, permissionSwitchPreference, preference, obj);
            }
        });
        return permissionSwitchPreference;
    }

    public static boolean lambda$createSwitchPreferenceForPermission$2(final AppPermissionsFragmentWear appPermissionsFragmentWear, final AppPermissionGroup appPermissionGroup, final PermissionInfo permissionInfo, final SwitchPreference switchPreference, Preference preference, Object obj) {
        int i;
        if (((Boolean) obj).booleanValue()) {
            appPermissionGroup.grantRuntimePermissions(false, new String[]{permissionInfo.name});
            if (Utils.areGroupPermissionsIndividuallyControlled(appPermissionsFragmentWear.getContext(), appPermissionGroup.getName()) && appPermissionGroup.doesSupportRuntimePermissions()) {
                String[] strArrAppendString = null;
                int size = appPermissionGroup.getPermissions().size();
                for (int i2 = 0; i2 < size; i2++) {
                    Permission permission = appPermissionGroup.getPermissions().get(i2);
                    if (!permission.isGranted() && !permission.isUserFixed()) {
                        strArrAppendString = ArrayUtils.appendString(strArrAppendString, permission.getName());
                    }
                }
                if (strArrAppendString != null) {
                    appPermissionGroup.revokeRuntimePermissions(true, strArrAppendString);
                }
            }
        } else {
            final Permission permissionFromGroup = getPermissionFromGroup(appPermissionGroup, permissionInfo.name);
            if (permissionFromGroup == null) {
                return false;
            }
            boolean zIsGrantedByDefault = permissionFromGroup.isGrantedByDefault();
            if (zIsGrantedByDefault || (!appPermissionGroup.doesSupportRuntimePermissions() && !appPermissionsFragmentWear.mHasConfirmedRevoke)) {
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public final void onClick(DialogInterface dialogInterface, int i3) {
                        AppPermissionsFragmentWear.lambda$createSwitchPreferenceForPermission$1(this.f$0, appPermissionGroup, permissionInfo, switchPreference, permissionFromGroup, dialogInterface, i3);
                    }
                };
                if (zIsGrantedByDefault) {
                    i = R.string.system_warning;
                } else {
                    i = R.string.old_sdk_deny_warning;
                }
                appPermissionsFragmentWear.showRevocationWarningDialog(onClickListener, i);
                return false;
            }
            appPermissionsFragmentWear.revokePermissionInGroup(appPermissionGroup, permissionInfo.name);
        }
        return true;
    }

    public static void lambda$createSwitchPreferenceForPermission$1(AppPermissionsFragmentWear appPermissionsFragmentWear, AppPermissionGroup appPermissionGroup, PermissionInfo permissionInfo, SwitchPreference switchPreference, Permission permission, DialogInterface dialogInterface, int i) {
        appPermissionsFragmentWear.revokePermissionInGroup(appPermissionGroup, permissionInfo.name);
        switchPreference.setChecked(false);
        if (!permission.isGrantedByDefault()) {
            appPermissionsFragmentWear.mHasConfirmedRevoke = true;
        }
    }

    private void showRevocationWarningDialog(DialogInterface.OnClickListener onClickListener, int i) {
        new WearableDialogHelper.DialogBuilder(getContext()).setNegativeIcon(R.drawable.confirm_button).setPositiveIcon(R.drawable.cancel_button).setNegativeButton(R.string.grant_dialog_button_deny_anyway, onClickListener).setPositiveButton(R.string.cancel, (DialogInterface.OnClickListener) null).setMessage(i).show();
    }

    private static Permission getPermissionFromGroup(AppPermissionGroup appPermissionGroup, String str) {
        int size = appPermissionGroup.getPermissions().size();
        for (int i = 0; i < size; i++) {
            Permission permission = appPermissionGroup.getPermissions().get(i);
            if (permission.getName().equals(str)) {
                return permission;
            }
        }
        if ("user".equals(Build.TYPE)) {
            Log.e("AppPermFragWear", String.format("The impossible happens, permission %s is not in group %s.", str, appPermissionGroup.getName()));
            return null;
        }
        throw new IllegalArgumentException(String.format("Permission %s is not in group %s", str, appPermissionGroup.getName()));
    }

    private void revokePermissionInGroup(AppPermissionGroup appPermissionGroup, String str) {
        appPermissionGroup.revokeRuntimePermissions(true, new String[]{str});
        if (Utils.areGroupPermissionsIndividuallyControlled(getContext(), appPermissionGroup.getName()) && appPermissionGroup.doesSupportRuntimePermissions() && !appPermissionGroup.areRuntimePermissionsGranted()) {
            appPermissionGroup.revokeRuntimePermissions(false);
        }
    }

    private SwitchPreference createSwitchPreferenceForGroup(final AppPermissionGroup appPermissionGroup) {
        final PermissionSwitchPreference permissionSwitchPreference = new PermissionSwitchPreference(getActivity());
        permissionSwitchPreference.setKey(appPermissionGroup.getName());
        permissionSwitchPreference.setTitle(appPermissionGroup.getLabel());
        permissionSwitchPreference.setChecked(appPermissionGroup.areRuntimePermissionsGranted());
        if (appPermissionGroup.isPolicyFixed()) {
            permissionSwitchPreference.setEnabled(false);
        } else {
            permissionSwitchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public final boolean onPreferenceChange(Preference preference, Object obj) {
                    return AppPermissionsFragmentWear.lambda$createSwitchPreferenceForGroup$4(this.f$0, appPermissionGroup, permissionSwitchPreference, preference, obj);
                }
            });
        }
        return permissionSwitchPreference;
    }

    public static boolean lambda$createSwitchPreferenceForGroup$4(final AppPermissionsFragmentWear appPermissionsFragmentWear, final AppPermissionGroup appPermissionGroup, final SwitchPreference switchPreference, Preference preference, Object obj) {
        int i;
        if (LocationUtils.isLocationGroupAndProvider(appPermissionGroup.getName(), appPermissionGroup.getApp().packageName)) {
            LocationUtils.showLocationDialog(appPermissionsFragmentWear.getContext(), appPermissionsFragmentWear.mAppPermissions.getAppLabel());
            return false;
        }
        if (((Boolean) obj).booleanValue()) {
            appPermissionsFragmentWear.setPermission(appPermissionGroup, switchPreference, true);
        } else {
            boolean zHasGrantedByDefaultPermission = appPermissionGroup.hasGrantedByDefaultPermission();
            if (zHasGrantedByDefaultPermission || (!appPermissionGroup.doesSupportRuntimePermissions() && !appPermissionsFragmentWear.mHasConfirmedRevoke)) {
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public final void onClick(DialogInterface dialogInterface, int i2) {
                        AppPermissionsFragmentWear.lambda$createSwitchPreferenceForGroup$3(this.f$0, appPermissionGroup, switchPreference, dialogInterface, i2);
                    }
                };
                if (zHasGrantedByDefaultPermission) {
                    i = R.string.system_warning;
                } else {
                    i = R.string.old_sdk_deny_warning;
                }
                appPermissionsFragmentWear.showRevocationWarningDialog(onClickListener, i);
                return false;
            }
            appPermissionsFragmentWear.setPermission(appPermissionGroup, switchPreference, false);
        }
        return true;
    }

    public static void lambda$createSwitchPreferenceForGroup$3(AppPermissionsFragmentWear appPermissionsFragmentWear, AppPermissionGroup appPermissionGroup, SwitchPreference switchPreference, DialogInterface dialogInterface, int i) {
        appPermissionsFragmentWear.setPermission(appPermissionGroup, switchPreference, false);
        if (!appPermissionGroup.hasGrantedByDefaultPermission()) {
            appPermissionsFragmentWear.mHasConfirmedRevoke = true;
        }
    }

    private void setPermission(AppPermissionGroup appPermissionGroup, SwitchPreference switchPreference, boolean z) {
        if (z) {
            appPermissionGroup.grantRuntimePermissions(false);
        } else {
            appPermissionGroup.revokeRuntimePermissions(false);
        }
        addToggledGroup(appPermissionGroup);
        switchPreference.setChecked(z);
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

    private void logAndClearToggledGroups() {
        if (this.mToggledGroups != null) {
            SafetyNetLogger.logPermissionsToggled(this.mAppPermissions.getPackageInfo().packageName, this.mToggledGroups);
            this.mToggledGroups = null;
        }
    }

    private List<PermissionInfo> getPermissionInfosFromGroup(AppPermissionGroup appPermissionGroup) {
        ArrayList arrayList = new ArrayList(appPermissionGroup.getPermissions().size());
        for (Permission permission : appPermissionGroup.getPermissions()) {
            try {
                arrayList.add(this.mPackageManager.getPermissionInfo(permission.getName(), 0));
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("AppPermFragWear", "No permission:" + permission.getName());
            }
        }
        return arrayList;
    }

    private void setPreferenceCheckedIfPresent(String str, boolean z) {
        Preference preferenceFindPreference = findPreference(str);
        if (preferenceFindPreference instanceof SwitchPreference) {
            ((SwitchPreference) preferenceFindPreference).setChecked(z);
        }
    }
}
