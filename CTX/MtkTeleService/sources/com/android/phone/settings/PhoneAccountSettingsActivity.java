package com.android.phone.settings;

import android.app.ActionBar;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;

public class PhoneAccountSettingsActivity extends PreferenceActivity implements PhoneGlobals.SubInfoUpdateListener {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.phone_accounts);
        }
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PhoneAccountSettingsFragment()).commit();
        if (isPrimaryUser()) {
            PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }

    @Override
    protected void onDestroy() {
        if (isPrimaryUser()) {
            PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        }
        super.onDestroy();
    }

    private boolean isPrimaryUser() {
        return ((UserManager) getSystemService("user")).isPrimaryUser();
    }
}
