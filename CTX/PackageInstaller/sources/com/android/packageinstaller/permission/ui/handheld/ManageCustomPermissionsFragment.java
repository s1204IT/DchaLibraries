package com.android.packageinstaller.permission.ui.handheld;

import android.os.Bundle;
import android.preference.Preference;
import android.view.MenuItem;
import com.android.packageinstaller.R;

public class ManageCustomPermissionsFragment extends ManagePermissionsFragment {
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

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.additional_permissions);
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
    protected void updatePermissionsUi() {
        updatePermissionsUi(false);
    }
}
