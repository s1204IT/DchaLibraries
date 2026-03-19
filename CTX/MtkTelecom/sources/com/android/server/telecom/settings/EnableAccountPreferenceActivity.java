package com.android.server.telecom.settings;

import android.R;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

public class EnableAccountPreferenceActivity extends Activity {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getFragmentManager().beginTransaction().replace(R.id.content, new EnableAccountPreferenceFragment()).commit();
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
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
}
