package com.android.settings.utils;

import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.drawer.UserAdapter;

public abstract class ProfileSettingsPreferenceFragment extends SettingsPreferenceFragment {
    protected abstract String getIntentActionString();

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UserManager um = (UserManager) getSystemService("user");
        final UserAdapter profileSpinnerAdapter = UserAdapter.createUserSpinnerAdapter(um, getActivity());
        if (profileSpinnerAdapter == null) {
            return;
        }
        final Spinner spinner = (Spinner) setPinnedHeaderView(R.layout.spinner_view);
        spinner.setAdapter((SpinnerAdapter) profileSpinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view2, int position, long id) {
                UserHandle selectedUser = profileSpinnerAdapter.getUserHandle(position);
                if (selectedUser.getIdentifier() == UserHandle.myUserId()) {
                    return;
                }
                Intent intent = new Intent(ProfileSettingsPreferenceFragment.this.getIntentActionString());
                intent.addFlags(268435456);
                intent.addFlags(32768);
                ProfileSettingsPreferenceFragment.this.getActivity().startActivityAsUser(intent, selectedUser);
                spinner.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
