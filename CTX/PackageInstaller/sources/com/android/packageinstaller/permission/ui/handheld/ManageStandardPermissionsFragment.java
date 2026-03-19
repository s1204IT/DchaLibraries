package com.android.packageinstaller.permission.ui.handheld;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.PermissionGroup;
import com.android.packageinstaller.permission.utils.Utils;
import java.util.Iterator;

public final class ManageStandardPermissionsFragment extends ManagePermissionsFragment {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public void onPermissionGroupsChanged() {
        super.onPermissionGroupsChanged();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return super.onPreferenceClick(preference);
    }

    public static ManageStandardPermissionsFragment newInstance() {
        return new ManageStandardPermissionsFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.app_permissions);
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
    protected void updatePermissionsUi() {
        PreferenceScreen preferenceScreenUpdatePermissionsUi = updatePermissionsUi(true);
        if (preferenceScreenUpdatePermissionsUi == null) {
            return;
        }
        Iterator<PermissionGroup> it = getPermissions().getGroups().iterator();
        int i = 0;
        while (it.hasNext()) {
            if (!it.next().getDeclaringPackage().equals("android")) {
                i++;
            }
        }
        Preference preferenceFindPreference = preferenceScreenUpdatePermissionsUi.findPreference("extra_prefs_key");
        if (i == 0) {
            if (preferenceFindPreference != null) {
                preferenceScreenUpdatePermissionsUi.removePreference(preferenceFindPreference);
                return;
            }
            return;
        }
        if (preferenceFindPreference == null) {
            preferenceFindPreference = new Preference(getActivity());
            preferenceFindPreference.setKey("extra_prefs_key");
            preferenceFindPreference.setIcon(Utils.applyTint(getActivity(), R.drawable.ic_more_items, android.R.attr.colorControlNormal));
            preferenceFindPreference.setTitle(R.string.additional_permissions);
            preferenceFindPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public final boolean onPreferenceClick(Preference preference) {
                    return ManageStandardPermissionsFragment.lambda$updatePermissionsUi$0(this.f$0, preference);
                }
            });
            preferenceScreenUpdatePermissionsUi.addPreference(preferenceFindPreference);
        }
        preferenceFindPreference.setSummary(getResources().getQuantityString(R.plurals.additional_permissions_more, i, Integer.valueOf(i)));
    }

    public static boolean lambda$updatePermissionsUi$0(ManageStandardPermissionsFragment manageStandardPermissionsFragment, Preference preference) {
        ManageCustomPermissionsFragment manageCustomPermissionsFragment = new ManageCustomPermissionsFragment();
        manageCustomPermissionsFragment.setTargetFragment(manageStandardPermissionsFragment, 0);
        FragmentTransaction fragmentTransactionBeginTransaction = manageStandardPermissionsFragment.getFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.replace(android.R.id.content, manageCustomPermissionsFragment);
        fragmentTransactionBeginTransaction.addToBackStack(null);
        fragmentTransactionBeginTransaction.commit();
        return true;
    }
}
