package com.android.packageinstaller.permission.ui.handheld;

import android.R;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.ArraySet;
import android.util.Log;
import com.android.packageinstaller.permission.model.PermissionApps;
import com.android.packageinstaller.permission.model.PermissionGroup;
import com.android.packageinstaller.permission.model.PermissionGroups;
import com.android.packageinstaller.permission.utils.Utils;
import java.util.List;

abstract class ManagePermissionsFragment extends PermissionsFrameFragment implements Preference.OnPreferenceClickListener, PermissionGroups.PermissionsGroupsChangeCallback {
    private ArraySet<String> mLauncherPkgs;
    private PermissionGroups mPermissions;

    protected abstract void updatePermissionsUi();

    ManagePermissionsFragment() {
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
        this.mLauncherPkgs = Utils.getLauncherPackages(getContext());
        this.mPermissions = new PermissionGroups(getContext(), getLoaderManager(), this);
    }

    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (this.mPermissions.getGroup(key) == null) {
            return false;
        }
        Intent intentPutExtra = new Intent("android.intent.action.MANAGE_PERMISSION_APPS").putExtra("android.intent.extra.PERMISSION_NAME", key);
        try {
            getActivity().startActivity(intentPutExtra);
            return true;
        } catch (ActivityNotFoundException e) {
            Log.w("ManagePermissionsFragment", "No app to handle " + intentPutExtra);
            return true;
        }
    }

    protected PermissionGroups getPermissions() {
        return this.mPermissions;
    }

    public void onPermissionGroupsChanged() {
        updatePermissionsUi();
    }

    protected PreferenceScreen updatePermissionsUi(boolean z) {
        Activity activity = getActivity();
        if (activity == null) {
            return null;
        }
        List<PermissionGroup> groups = this.mPermissions.getGroups();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) {
            preferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            setPreferenceScreen(preferenceScreen);
        } else {
            preferenceScreen.removeAll();
        }
        new PermissionApps.PmCache(getContext().getPackageManager());
        for (PermissionGroup permissionGroup : groups) {
            if (z == permissionGroup.getDeclaringPackage().equals("android")) {
                Preference preferenceFindPreference = findPreference(permissionGroup.getName());
                if (preferenceFindPreference == null) {
                    preferenceFindPreference = new Preference(activity);
                    preferenceFindPreference.setOnPreferenceClickListener(this);
                    preferenceFindPreference.setKey(permissionGroup.getName());
                    preferenceFindPreference.setIcon(Utils.applyTint(activity, permissionGroup.getIcon(), R.attr.colorControlNormal));
                    preferenceFindPreference.setTitle(permissionGroup.getLabel());
                    preferenceFindPreference.setSummary(" ");
                    preferenceFindPreference.setPersistent(false);
                    preferenceScreen.addPreference(preferenceFindPreference);
                }
                preferenceFindPreference.setSummary(getString(com.android.packageinstaller.R.string.app_permissions_group_summary, new Object[]{Integer.valueOf(permissionGroup.getGranted()), Integer.valueOf(permissionGroup.getTotal())}));
            }
        }
        if (preferenceScreen.getPreferenceCount() != 0) {
            setLoading(false, true);
        }
        return preferenceScreen;
    }
}
