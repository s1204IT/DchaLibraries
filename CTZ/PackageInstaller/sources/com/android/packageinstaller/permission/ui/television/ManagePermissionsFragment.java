package com.android.packageinstaller.permission.ui.television;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.ArraySet;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.PermissionApps;
import com.android.packageinstaller.permission.model.PermissionGroup;
import com.android.packageinstaller.permission.model.PermissionGroups;
import com.android.packageinstaller.permission.utils.Utils;
import java.util.List;

public final class ManagePermissionsFragment extends SettingsWithHeader implements Preference.OnPreferenceClickListener, PermissionGroups.PermissionsGroupsChangeCallback {
    private PreferenceScreen mExtraScreen;
    private ArraySet<String> mLauncherPkgs;
    private PermissionGroups mPermissions;

    public static ManagePermissionsFragment newInstance() {
        return new ManagePermissionsFragment();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            getActivity().finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
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

    @Override
    public void onPermissionGroupsChanged() {
        updatePermissionsUi();
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        bindPermissionUi(this, getView());
    }

    private static void bindPermissionUi(SettingsWithHeader settingsWithHeader, View view) {
        if (settingsWithHeader == null || view == null) {
            return;
        }
        settingsWithHeader.setHeader(null, null, null, settingsWithHeader.getString(R.string.manage_permissions_decor_title));
    }

    private void updatePermissionsUi() {
        Context context = getPreferenceManager().getContext();
        if (context == null) {
            return;
        }
        List<PermissionGroup> groups = this.mPermissions.getGroups();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        new PermissionApps.PmCache(getContext().getPackageManager());
        for (PermissionGroup permissionGroup : groups) {
            boolean zEquals = permissionGroup.getDeclaringPackage().equals("android");
            Preference preferenceFindPreference = findPreference(permissionGroup.getName());
            if (preferenceFindPreference == null && this.mExtraScreen != null) {
                preferenceFindPreference = this.mExtraScreen.findPreference(permissionGroup.getName());
            }
            if (preferenceFindPreference == null) {
                preferenceFindPreference = new Preference(context);
                preferenceFindPreference.setOnPreferenceClickListener(this);
                preferenceFindPreference.setKey(permissionGroup.getName());
                preferenceFindPreference.setIcon(Utils.applyTint(context, permissionGroup.getIcon(), android.R.attr.colorControlNormal));
                preferenceFindPreference.setTitle(permissionGroup.getLabel());
                preferenceFindPreference.setSummary(" ");
                preferenceFindPreference.setPersistent(false);
                if (zEquals) {
                    preferenceScreen.addPreference(preferenceFindPreference);
                } else {
                    if (this.mExtraScreen == null) {
                        this.mExtraScreen = getPreferenceManager().createPreferenceScreen(context);
                    }
                    this.mExtraScreen.addPreference(preferenceFindPreference);
                }
            }
            preferenceFindPreference.setSummary(getString(R.string.app_permissions_group_summary, new Object[]{Integer.valueOf(permissionGroup.getGranted()), Integer.valueOf(permissionGroup.getTotal())}));
        }
        if (this.mExtraScreen != null && this.mExtraScreen.getPreferenceCount() > 0 && preferenceScreen.findPreference("extra_prefs_key") == null) {
            Preference preference = new Preference(context);
            preference.setKey("extra_prefs_key");
            preference.setIcon(Utils.applyTint(context, R.drawable.ic_more_items, android.R.attr.colorControlNormal));
            preference.setTitle(R.string.additional_permissions);
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference2) {
                    AdditionalPermissionsFragment additionalPermissionsFragment = new AdditionalPermissionsFragment();
                    additionalPermissionsFragment.setTargetFragment(ManagePermissionsFragment.this, 0);
                    FragmentTransaction fragmentTransactionBeginTransaction = ManagePermissionsFragment.this.getFragmentManager().beginTransaction();
                    fragmentTransactionBeginTransaction.replace(android.R.id.content, additionalPermissionsFragment);
                    fragmentTransactionBeginTransaction.addToBackStack(null);
                    fragmentTransactionBeginTransaction.commit();
                    return true;
                }
            });
            int preferenceCount = this.mExtraScreen.getPreferenceCount();
            preference.setSummary(getResources().getQuantityString(R.plurals.additional_permissions_more, preferenceCount, Integer.valueOf(preferenceCount)));
            preferenceScreen.addPreference(preference);
        }
        if (preferenceScreen.getPreferenceCount() != 0) {
            setLoading(false, true);
        }
    }

    public static class AdditionalPermissionsFragment extends SettingsWithHeader {
        @Override
        public void onCreate(Bundle bundle) {
            setLoading(true, false);
            super.onCreate(bundle);
            getActivity().setTitle(R.string.additional_permissions);
            setHasOptionsMenu(true);
        }

        @Override
        public void onDestroy() {
            getActivity().setTitle(R.string.app_permissions);
            super.onDestroy();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem menuItem) {
            if (menuItem.getItemId() == 16908332) {
                getFragmentManager().popBackStack();
                return true;
            }
            return super.onOptionsItemSelected(menuItem);
        }

        @Override
        public void onViewCreated(View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            bindPermissionUi(this, getView());
        }

        private static void bindPermissionUi(SettingsWithHeader settingsWithHeader, View view) {
            if (settingsWithHeader == null || view == null) {
                return;
            }
            settingsWithHeader.setHeader(null, null, null, settingsWithHeader.getString(R.string.additional_permissions_decor_title));
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String str) {
            setPreferenceScreen(((ManagePermissionsFragment) getTargetFragment()).mExtraScreen);
            setLoading(false, true);
        }
    }
}
